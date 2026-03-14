package hu.steradian.co2coremod.network;

import hu.steradian.co2coremod.Co2CoreMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ChunkSmogSyncS2CPayload(int chunkX, int chunkZ, int smog) implements CustomPacketPayload {
    public static final Identifier PAYLOAD_ID = Co2CoreMod.getId("chunk_smog_sync");
    public static final Type<@NotNull ChunkSmogSyncS2CPayload> ID = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull ChunkSmogSyncS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ChunkSmogSyncS2CPayload::chunkX,
                    ByteBufCodecs.INT, ChunkSmogSyncS2CPayload::chunkZ,
                    ByteBufCodecs.INT, ChunkSmogSyncS2CPayload::smog,
                    ChunkSmogSyncS2CPayload::new
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}