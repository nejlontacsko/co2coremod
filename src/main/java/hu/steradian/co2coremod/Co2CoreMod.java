package hu.steradian.co2coremod;

import hu.steradian.co2coremod.commands.SmogCommands;
import hu.steradian.co2coremod.network.ChunkSmogSyncS2CPayload;
import hu.steradian.co2coremod.network.NetworkHandler;
import hu.steradian.co2coremod.smog.SmogManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Co2CoreMod implements ModInitializer {
	public static final String MOD_ID = "co2coremod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier getId(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

        PayloadTypeRegistry.playS2C().register(
                ChunkSmogSyncS2CPayload.ID,
                ChunkSmogSyncS2CPayload.CODEC
        );

        SmogCommands.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            LevelChunk chunk = player.level().getChunk(player.chunkPosition().x, player.chunkPosition().z);
            int smog = SmogManager.getChunkAmount(chunk);
            NetworkHandler.syncChunkToPlayer(player, chunk, smog);
        });
	}
}