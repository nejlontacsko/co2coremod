package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.Co2CoreMod;

import hu.steradian.co2coremod.network.NetworkHandler;
import hu.steradian.co2coremod.components.ModComponents;
import hu.steradian.co2coremod.util.ModTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public final class SmogWorldTickHandler {
    private static final Queue<ChunkPos> PROCESSING_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<ChunkPos> QUEUED_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final long SLOW_WORLD_TICK_LOG_THRESHOLD_NS = 10_000_000L;
    private static final long SLOW_SECTION_LOG_THRESHOLD_NS = 5_000_000L;

    private static int tickCounter = 0;
    private static int rainWashTickCounter = 0;
    private static final int PROCESS_INTERVAL_TICKS = 20;
    private static final int RAIN_WASH_INTERVAL_TICKS = 20 * 10;
    private static final int RAIN_WASH_AMOUNT = RAIN_WASH_INTERVAL_TICKS;
    private static final int CHUNKS_PER_INTERVAL = 2;

    private static final int TORCH_EMISSION = 1;
    private static final int ENTITY_EMISSION = 10;
    private static final int CO2_DAMAGE_THRESHOLD = 200_000;
    private static final int CO2_DAMAGE_INTERVAL_TICKS = 40;
    private static final int RANDOM_EMISSION_INTERVAL_TICKS = 20;
    private static final int CO2_POISON_DURATION_TICKS = 100;
    private static final ResourceKey<DamageType> CO2_POISONING_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Co2CoreMod.getId("co2_poisoning")
    );
    private static final int TORCH_SAMPLES_PER_CHUNK = 8;
    private static final int TORCH_CHUNKS_PER_INTERVAL = 32;
    private static final int TORCH_SCAN_RADIUS_Y = 8;
    private static final double TORCH_EMISSION_CHANCE_PER_SAMPLE = 0.25;
    private static final double ENTITY_EMISSION_CHANCE_PER_CHECK = 0.10;

    private static final Map<UUID, ChunkPos> lastPlayerChunks = new HashMap<>();

    private SmogWorldTickHandler() {}

    private static boolean queueChunk(ChunkPos pos) {
        if (!QUEUED_CHUNKS.add(pos))
            return false;

        PROCESSING_QUEUE.offer(pos);
        return true;
    }

    private static void emitAt(ServerLevel world, BlockPos pos, int amount) {
        if (world.dimension() != Level.OVERWORLD)
            return;

        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null)
            SmogHandler.add(chunk, amount);
    }

    private static boolean isSmogEmittingTorch(BlockState state) {
        return state.getBlock() instanceof BaseTorchBlock
                || state.getBlock() instanceof WallTorchBlock
                || state.getBlock() instanceof RedstoneTorchBlock;
    }

    private static void emitFromRandomTorchSamples(ServerLevel world, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < TORCH_SAMPLES_PER_CHUNK; i++) {
            int x = chunkPos.getMinBlockX() + random.nextInt(16);
            int z = chunkPos.getMinBlockZ() + random.nextInt(16);
            BlockPos surfacePos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));
            int minY = Math.max(world.getMinY(), surfacePos.getY() - TORCH_SCAN_RADIUS_Y);
            int maxY = Math.min(world.getMaxY() - 1, surfacePos.getY() + 2);

            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(pos);

                if (isSmogEmittingTorch(state)
                        && random.nextDouble() < TORCH_EMISSION_CHANCE_PER_SAMPLE) {
                    emitAt(world, pos, TORCH_EMISSION);
                }
            }
        }
    }

    private static boolean applyRainWash(ServerLevel world, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos centerPos = new BlockPos(chunkPos.getMiddleBlockX(), 0, chunkPos.getMiddleBlockZ());
        BlockPos rainCheckPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, centerPos);

        if (!world.isRainingAt(rainCheckPos))
            return false;

        SmogHandler.add(chunk, -RAIN_WASH_AMOUNT);
        return true;
    }

    private static boolean hasCo2Protection(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModTags.Items.CO2_PROTECTION_HELMETS);
    }

    private static void applyPlayerCo2Effects(ServerLevel world, ServerPlayer player) {
        if (world.dimension() != Level.OVERWORLD)
            return;

        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL)
            return;

        if (player.tickCount % CO2_DAMAGE_INTERVAL_TICKS != 0)
            return;

        LevelChunk chunk = world.getChunkSource().getChunkNow(player.chunkPosition().x, player.chunkPosition().z);
        if (chunk == null)
            return;

        int smogAmount = ModComponents.CHUNK_DATA.get(chunk).getSmogAmount();
        if (smogAmount < CO2_DAMAGE_THRESHOLD)
            return;

        if (hasCo2Protection(player))
            return;

        player.addEffect(new MobEffectInstance(
                MobEffects.POISON,
                CO2_POISON_DURATION_TICKS,
                0,
                true,
                true
        ));

        if (player.getHealth() <= 1.0F)
            player.hurt(world.damageSources().source(CO2_POISONING_DAMAGE), 1.0F);
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.dimension() != Level.OVERWORLD)
                return;

            queueChunk(chunk.getPos());
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (world.dimension() != Level.OVERWORLD)
                return;

            ChunkPos pos = chunk.getPos();
            QUEUED_CHUNKS.remove(pos);
            PROCESSING_QUEUE.remove(pos);
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.dimension() != Level.OVERWORLD)
                return;

            long tickStartNs = System.nanoTime();
            long sectionStartNs = System.nanoTime();
            long playerSyncNs = 0L;
            long entityEmissionNs = 0L;
            long torchEmissionNs = 0L;
            long queueProcessingNs = 0L;
            int syncedPlayers = 0;
            int scannedEntities = 0;
            int emittedEntities = 0;
            int torchChunks = 0;
            int rainWashedChunks = 0;
            int processedChunks = 0;
            int queuedChunksBefore = QUEUED_CHUNKS.size();

            for (ServerPlayer player : world.players()) {
                ChunkPos current = player.chunkPosition();
                applyPlayerCo2Effects(world, player);
                ChunkPos previous = lastPlayerChunks.get(player.getUUID());

                if (previous == null || !previous.equals(current)) {
                    if (previous == null || current.getChessboardDistance(previous) > 1) {
                        NetworkHandler.syncAroundPlayer(player, 2);
                        syncedPlayers++;
                    }
                    lastPlayerChunks.put(player.getUUID(), current);
                }
            }

            playerSyncNs = System.nanoTime() - sectionStartNs;

            tickCounter++;
            rainWashTickCounter++;

            if (tickCounter % RANDOM_EMISSION_INTERVAL_TICKS == 0) {
                sectionStartNs = System.nanoTime();
                for (Entity entity : world.getAllEntities()) {
                    scannedEntities++;
                    if (!(entity instanceof LivingEntity))
                        continue;

                    if (ThreadLocalRandom.current().nextDouble() < ENTITY_EMISSION_CHANCE_PER_CHECK) {
                        emitAt(world, entity.blockPosition(), ENTITY_EMISSION);
                        emittedEntities++;
                    }
                }
                entityEmissionNs = System.nanoTime() - sectionStartNs;

                sectionStartNs = System.nanoTime();
                for (int i = 0; i < TORCH_CHUNKS_PER_INTERVAL; i++) {
                    ChunkPos pos = PROCESSING_QUEUE.poll();
                    if (pos == null)
                        break;

                    LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x, pos.z);
                    if (chunk != null) {
                        emitFromRandomTorchSamples(world, chunk);
                        torchChunks++;
                    }

                    PROCESSING_QUEUE.offer(pos);
                }

                torchEmissionNs = System.nanoTime() - sectionStartNs;
            }

            if (rainWashTickCounter >= RAIN_WASH_INTERVAL_TICKS) {
                rainWashTickCounter = 0;
                sectionStartNs = System.nanoTime();

                for (ChunkPos pos : QUEUED_CHUNKS) {
                    LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x, pos.z);
                    if (chunk != null && applyRainWash(world, chunk))
                        rainWashedChunks++;
                }

                torchEmissionNs += System.nanoTime() - sectionStartNs;
            }

            if (tickCounter < PROCESS_INTERVAL_TICKS)
                return;
            tickCounter = 0;

            sectionStartNs = System.nanoTime();

            for (int i = 0; i < CHUNKS_PER_INTERVAL; i++) {
                ChunkPos pos = PROCESSING_QUEUE.poll();
                if (pos == null)
                    break;

                LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk == null) {
                    QUEUED_CHUNKS.remove(pos);
                    continue;
                }

                SmogHandler.calcChunkAmountChange(chunk);
                processedChunks++;
                PROCESSING_QUEUE.offer(pos);
            }

            SmogHandler.tick();
            queueProcessingNs = System.nanoTime() - sectionStartNs;

            long tickElapsedNs = System.nanoTime() - tickStartNs;
            if (tickElapsedNs >= SLOW_WORLD_TICK_LOG_THRESHOLD_NS) {
                Co2CoreMod.LOGGER.info(
                        "co2.telemetry event=world_tick_slow duration_ms={} player_sync_ms={} entity_emission_ms={} torch_emission_ms={} queue_processing_ms={} queued_chunks_before={} processed_chunks={} synced_players={} scanned_entities={} emitted_entities={} torch_chunks={} rain_washed_chunks={}",
                        tickElapsedNs / 1_000_000.0,
                        playerSyncNs / 1_000_000.0,
                        entityEmissionNs / 1_000_000.0,
                        torchEmissionNs / 1_000_000.0,
                        queueProcessingNs / 1_000_000.0,
                        queuedChunksBefore,
                        processedChunks,
                        syncedPlayers,
                        scannedEntities,
                        emittedEntities,
                        torchChunks,
                        rainWashedChunks
                );
            } else {
                if (playerSyncNs >= SLOW_SECTION_LOG_THRESHOLD_NS)
                    Co2CoreMod.LOGGER.info("co2.telemetry event=section_slow section=player_sync duration_ms={} synced_players={}", playerSyncNs / 1_000_000.0, syncedPlayers);
                if (entityEmissionNs >= SLOW_SECTION_LOG_THRESHOLD_NS)
                    Co2CoreMod.LOGGER.info("co2.telemetry event=section_slow section=entity_emission duration_ms={} scanned_entities={} emitted_entities={}", entityEmissionNs / 1_000_000.0, scannedEntities, emittedEntities);
                if (torchEmissionNs >= SLOW_SECTION_LOG_THRESHOLD_NS)
                    Co2CoreMod.LOGGER.info("co2.telemetry event=section_slow section=emission_and_rain duration_ms={} torch_chunks={} rain_washed_chunks={} queued_chunks={}", torchEmissionNs / 1_000_000.0, torchChunks, rainWashedChunks, queuedChunksBefore);
                if (queueProcessingNs >= SLOW_SECTION_LOG_THRESHOLD_NS)
                    Co2CoreMod.LOGGER.info("co2.telemetry event=section_slow section=queue_processing duration_ms={} processed_chunks={} queued_chunks={}", queueProcessingNs / 1_000_000.0, processedChunks, queuedChunksBefore);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.syncAroundPlayer(handler.player, 6);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            lastPlayerChunks.remove(handler.player.getUUID());
        });
    }
}