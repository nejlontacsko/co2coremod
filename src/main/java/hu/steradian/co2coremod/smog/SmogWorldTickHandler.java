package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.network.NetworkHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public final class SmogWorldTickHandler {
    private static final Queue<ChunkPos> PROCESSING_QUEUE = new ConcurrentLinkedQueue<>();

    private static int tickCounter = 0;
    private static final int PROCESS_INTERVAL_TICKS = 20;
    private static final int CHUNKS_PER_INTERVAL = 60;

    private static final int TORCH_EMISSION = 1;
    private static final int ENTITY_EMISSION = 10;
    private static final int RANDOM_EMISSION_INTERVAL_TICKS = 20;
    private static final int TORCH_SAMPLES_PER_CHUNK = 8;
    private static final int TORCH_SCAN_RADIUS_Y = 8;
    private static final double TORCH_EMISSION_CHANCE_PER_SAMPLE = 0.25;
    private static final double ENTITY_EMISSION_CHANCE_PER_CHECK = 0.10;

    private static final Map<UUID, ChunkPos> lastPlayerChunks = new HashMap<>();

    private SmogWorldTickHandler() {}

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

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.dimension() != Level.OVERWORLD)
                return;

            ChunkPos pos = chunk.getPos();
            if (!PROCESSING_QUEUE.contains(pos))
                PROCESSING_QUEUE.offer(pos);
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (world.dimension() != Level.OVERWORLD)
                return;

            PROCESSING_QUEUE.remove(chunk.getPos());
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.dimension() != Level.OVERWORLD)
                return;

            for (ServerPlayer player : world.players()) {
                ChunkPos current = player.chunkPosition();
                ChunkPos previous = lastPlayerChunks.get(player.getUUID());

                if (previous == null || !previous.equals(current)) {
                    if (previous == null || current.getChessboardDistance(previous) > 1)
                        NetworkHandler.syncAroundPlayer(player, 2);
                    lastPlayerChunks.put(player.getUUID(), current);
                }
            }

            tickCounter++;

            if (tickCounter % RANDOM_EMISSION_INTERVAL_TICKS == 0) {
                for (Entity entity : world.getAllEntities()) {
                    if (!(entity instanceof LivingEntity))
                        continue;

                    if (ThreadLocalRandom.current().nextDouble() < ENTITY_EMISSION_CHANCE_PER_CHECK)
                        emitAt(world, entity.blockPosition(), ENTITY_EMISSION);
                }

                for (ChunkPos pos : PROCESSING_QUEUE) {
                    LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x, pos.z);
                    if (chunk != null)
                        emitFromRandomTorchSamples(world, chunk);
                }
            }

            if (tickCounter < PROCESS_INTERVAL_TICKS)
                return;
            tickCounter = 0;

            for (int i = 0; i < CHUNKS_PER_INTERVAL; i++) {
                ChunkPos pos = PROCESSING_QUEUE.poll();
                if (pos == null)
                    break;

                LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk == null)
                    continue;

                SmogHandler.calcChunkAmountChange(chunk);
                PROCESSING_QUEUE.offer(pos);
            }

            SmogHandler.tick();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.syncAroundPlayer(handler.player, 6);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            lastPlayerChunks.remove(handler.player.getUUID());
        });
    }
}