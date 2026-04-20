package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.client.ClientSmogData;
import hu.steradian.co2coremod.components.IChunkSmogData;
import hu.steradian.co2coremod.components.ModComponents;
import hu.steradian.co2coremod.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

public class SmogHandler {
    private static final int SYNC_TICK_INTERVAL = 5;
    private static int tickCounter = 0;
    private static final Set<LevelChunk> dirtyChunks = new HashSet<>();

    private static void markDirty(LevelChunk chunk) {
        dirtyChunks.add(chunk);
        chunk.markUnsaved();
    }

    private static void syncDirtyChunk(LevelChunk chunk, IChunkSmogData data) {
        if (chunk.getLevel() instanceof ServerLevel)
            NetworkHandler.syncChunkToTracking(chunk, data.getSmogAmount());
    }

    public static void tick() {
        tickCounter++;

        if (tickCounter >= SYNC_TICK_INTERVAL) {
            tickCounter = 0;

            //Set<LevelChunk> chunksToSync = new HashSet<>(dirtyChunks);

            for (LevelChunk chunk : dirtyChunks) {
                IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
                syncDirtyChunk(chunk, data);
            }
            dirtyChunks.clear();
        }
    }

    public static int getChunkAmount(LevelChunk chunk) {
        if (chunk.getLevel().isClientSide())
            return ClientSmogData.getSmogAmount(chunk.getPos());

        return ModComponents.CHUNK_DATA.get(chunk).getSmogAmount();
    }

    public static void setChunkAmount(LevelChunk chunk, int amount) {
        IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
        int oldAmount = data.getSmogAmount();

        data.setSmogAmount(amount);

        if (data.getSmogAmount() != oldAmount)
            markDirty(chunk);
    }

    public static void add(LevelChunk chunk, int amount) {
        if (amount == 0) return;

        IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
        int oldAmount = data.getSmogAmount();

        data.setSmogAmount(oldAmount + amount);

        if (data.getSmogAmount() != oldAmount)
            markDirty(chunk);
    }

    public static int calcChunkAmountChange(LevelChunk chunk) {
        int delta = SmogCalculator.calculateSmogChange(chunk);
        add(chunk, delta);
        return delta;
    }
}
