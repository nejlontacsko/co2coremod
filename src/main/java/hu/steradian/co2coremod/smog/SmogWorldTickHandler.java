package hu.steradian.co2coremod.smog;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SmogWorldTickHandler {
    private static final Queue<ChunkPos> PROCESSING_QUEUE = new ConcurrentLinkedQueue<>();

    private static int tickCounter = 0;
    private static final int PROCESS_INTERVAL_TICKS = 20;
    private static final int CHUNKS_PER_INTERVAL = 60;

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
    }
}