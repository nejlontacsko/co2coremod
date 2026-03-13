package hu.steradian.co2coremod;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import net.minecraft.core.RegistrySetBuilder;

import org.jetbrains.annotations.NotNull;

public class Co2CoreModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		//FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
	}

	@Override
	public void buildRegistry(@NotNull RegistrySetBuilder registryBuilder) {
		DataGeneratorEntrypoint.super.buildRegistry(registryBuilder);
	}
}
