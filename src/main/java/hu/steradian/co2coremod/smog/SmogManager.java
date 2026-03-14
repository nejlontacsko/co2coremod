package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.components.IChunkSmogData;
import hu.steradian.co2coremod.components.ModComponents;
import net.minecraft.world.level.chunk.LevelChunk;

public class SmogManager {
    public static int getChunkAmount(LevelChunk chunk) {
        return ModComponents.CHUNK_DATA.get(chunk).getSmogAmount();
    }

    public static void setChunkAmount(LevelChunk chunk, int amount) {
        ModComponents.CHUNK_DATA.get(chunk).setSmogAmount(amount);
    }

    public static void add(LevelChunk chunk, int amount) {
        IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
        data.setSmogAmount(data.getSmogAmount() + amount);
    }
}
