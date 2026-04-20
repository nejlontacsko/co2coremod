package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.network.NetworkHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SmogWorldTickHandler {
    private static final Queue<ChunkPos> PROCESSING_QUEUE = new ConcurrentLinkedQueue<>();

    private static int tickCounter = 0;
    private static final int PROCESS_INTERVAL_TICKS = 20;
    private static final int CHUNKS_PER_INTERVAL = 60;

    private static final Map<UUID, ChunkPos> lastPlayerChunks = new HashMap<>();

    private SmogWorldTickHandler() {}

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
                    NetworkHandler.syncAroundPlayer(player, 6);
                    lastPlayerChunks.put(player.getUUID(), current);
                }
            }

            tickCounter++;
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

        //ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            //ServerPlayer player = handler.player;
            //LevelChunk chunk = player.level().getChunk(player.chunkPosition().x, player.chunkPosition().z);
            //int smog = SmogHandler.getChunkAmount(chunk);
            //NetworkHandler.syncChunkToPlayer(player, chunk, smog);
        //});

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.syncAroundPlayer(handler.player, 6);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            lastPlayerChunks.remove(handler.player.getUUID());
        });
    }
}