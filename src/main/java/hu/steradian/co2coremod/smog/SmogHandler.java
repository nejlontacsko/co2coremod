package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.client.ClientSmogData;
import hu.steradian.co2coremod.components.IChunkSmogData;
import hu.steradian.co2coremod.components.ModComponents;
import hu.steradian.co2coremod.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

public class SmogHandler {
    private static final int SYNC_TICK_INTERVAL = 5;
    private static int tickCounter = 0;
    private static final Set<LevelChunk> dirtyChunks = new HashSet<>();
    private static final Set<ChunkPos> restrictedChunks = new HashSet<>();

    private static boolean isWorldSpawnProtected(LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ServerLevel serverLevel))
            return false;

        BlockPos spawnPos = serverLevel.getLevelData().getRespawnData().pos();
        int spawnChunkX = spawnPos.getX() >> 4;
        int spawnChunkZ = spawnPos.getZ() >> 4;
        ChunkPos chunkPos = chunk.getPos();

        return Math.abs(chunkPos.x - spawnChunkX) <= 1
            && Math.abs(chunkPos.z - spawnChunkZ) <= 1;
    }

    public static boolean isRestricted(LevelChunk chunk) {
        return isWorldSpawnProtected(chunk) || restrictedChunks.contains(chunk.getPos());
    }

    public static boolean addRestrictedChunk(ChunkPos chunkPos) {
        return restrictedChunks.add(chunkPos);
    }

    public static boolean removeRestrictedChunk(ChunkPos chunkPos) {
        return restrictedChunks.remove(chunkPos);
    }

    public static int getRestrictedChunkCount() {
        return restrictedChunks.size();
    }

    private static void writeChunkAmount(LevelChunk chunk, int amount) {
        IChunkSmogData data = ModComponents.CHUNK_DATA.get(chunk);
        int oldAmount = data.getSmogAmount();
        int newAmount = isRestricted(chunk) ? 0 : Math.max(0, amount);

        if (newAmount == oldAmount)
            return;

        data.setSmogAmount(newAmount);
        markDirty(chunk);
    }

    private static void markDirty(LevelChunk chunk) {
        dirtyChunks.add(chunk);
        chunk.markUnsaved();
    }

    private static void syncDirtyChunk(LevelChunk chunk, IChunkSmogData data) {
        if (chunk.getLevel() instanceof net.minecraft.server.level.ServerLevel)
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
        writeChunkAmount(chunk, amount);
    }

    public static void add(LevelChunk chunk, int amount) {
        writeChunkAmount(chunk, getChunkAmount(chunk) + amount);
    }

    public static int calcChunkAmountChange(LevelChunk chunk) {
        int delta = SmogCalculator.calculateSmogChange(chunk);
        add(chunk, delta);
        return delta;
    }
}
