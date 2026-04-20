package hu.steradian.co2coremod.network;

import hu.steradian.co2coremod.smog.SmogHandler;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

public final class NetworkHandler {
    public static void syncChunkToTracking(LevelChunk chunk, int smog) {
        if (!(chunk.getLevel() instanceof ServerLevel serverLevel))
            return;

        ChunkSmogSyncS2CPayload payload =
                new ChunkSmogSyncS2CPayload(chunk.getPos().x, chunk.getPos().z, smog);

        BlockPos pos = new BlockPos(
                chunk.getPos().getMiddleBlockX(),
                serverLevel.getMinY(),
                chunk.getPos().getMiddleBlockZ()
        );
        for (ServerPlayer player : PlayerLookup.tracking((ServerLevel) chunk.getLevel(), pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void syncChunkToPlayer(ServerPlayer player, LevelChunk chunk, int smog) {
        ServerPlayNetworking.send(player,
                new ChunkSmogSyncS2CPayload(chunk.getPos().x, chunk.getPos().z, smog)
        );
    }

    public static void syncAroundPlayer(ServerPlayer player, int radius) {
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = player.level().getChunk(centerX + dx, centerZ + dz);
                int smog = SmogHandler.getChunkAmount(chunk);
                syncChunkToPlayer(player, chunk, smog);
            }
        }
    }
}
