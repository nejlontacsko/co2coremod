package hu.steradian.co2coremod.components;

import hu.steradian.co2coremod.Co2CoreMod;

import net.minecraft.world.level.chunk.ChunkAccess;

import org.ladysnake.cca.api.v3.chunk.ChunkComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.chunk.ChunkComponentInitializer;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;

public class ModComponents implements ChunkComponentInitializer {
    public static final ComponentKey<IChunkSmogData> CHUNK_DATA =
        ComponentRegistryV3.INSTANCE.getOrCreate(Co2CoreMod.getId("chunk_data"), IChunkSmogData.class);

    @Override
    public void registerChunkComponentFactories(ChunkComponentFactoryRegistry registry) {
        Co2CoreMod.LOGGER.info("[MOD COMPONENTS] Registering chunk components");
        registry.register(CHUNK_DATA, (ChunkAccess chunk) -> new ChunkSmogData());
    }
}
