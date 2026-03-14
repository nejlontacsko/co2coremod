package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.components.IChunkSmogData;
import hu.steradian.co2coremod.components.ModComponents;
import hu.steradian.co2coremod.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public class SmogManager {
    public static int getChunkAmount(LevelChunk chunk) {
        return ModComponents.CHUNK_DATA.get(chunk).getSmogAmount();
    }

    public static void setChunkAmount(LevelChunk chunk, int amount) {
        IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
        data.setSmogAmount(amount);
        sync(chunk, data);
    }

    public static void add(LevelChunk chunk, int amount) {
        if (amount == 0) return;

        IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
        data.setSmogAmount(data.getSmogAmount() + amount);
        sync(chunk, data);
    }

    private static void sync(LevelChunk chunk, IChunkSmogData data) {
        if (chunk.getLevel() instanceof ServerLevel)
            NetworkHandler.syncChunkToTracking(chunk, data.getSmogAmount());
    }

    public static int calcChunkAmountChange(LevelChunk chunk) {
        int delta = SmogCalculator.calculateSmogChange(chunk);
        add(chunk, delta);
        return delta;
    }
}
