package hu.steradian.co2coremod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
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
	}
}