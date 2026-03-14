package hu.steradian.co2coremod;

import hu.steradian.co2coremod.client.ClientSmogData;
import hu.steradian.co2coremod.client.debug.SmogDebugEntry;
import hu.steradian.co2coremod.network.ChunkSmogSyncS2CPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

public class Co2CoreModClient implements ClientModInitializer {
    public static Identifier SMOG_DEBUG_ENTRY;

    @Override
    public void onInitializeClient() {
        SMOG_DEBUG_ENTRY = DebugScreenEntries.register(
            Co2CoreMod.getId("smog"),
            new SmogDebugEntry()
        );

        Minecraft.getInstance().execute(() -> {
            if (!Minecraft.getInstance().debugEntries.isCurrentlyEnabled(SMOG_DEBUG_ENTRY))
                Minecraft.getInstance().debugEntries.toggleStatus(SMOG_DEBUG_ENTRY);
        });

        ClientPlayNetworking.registerGlobalReceiver(
            ChunkSmogSyncS2CPayload.ID,
            (payload, context) -> {
                ChunkPos pos = new ChunkPos(payload.chunkX(), payload.chunkZ());
                ClientSmogData.setSmogAmount(pos, payload.smog());
            }
        );
    }
}
