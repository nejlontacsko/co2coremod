package hu.steradian.co2coremod.smog;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class AbsorptionValues {
    // Map to store CO2 absorption values per block.
    private static final Map<Block, Integer> MAP = new HashMap<>();

    static {
        // Standard absorption value
        int standardAbsorption = 2;
        // Higher absorption for saplings
        int saplingAbsorption = 6;

        // Grasses and Ferns (TallGrassBlock covers Grass and Tall Grass)
        MAP.put(Blocks.SHORT_GRASS, standardAbsorption);
        MAP.put(Blocks.TALL_GRASS, standardAbsorption);
        MAP.put(Blocks.FERN, standardAbsorption);
        MAP.put(Blocks.LARGE_FERN, standardAbsorption);

        // Other Plants
        MAP.put(Blocks.VINE, standardAbsorption);
        MAP.put(Blocks.SUGAR_CANE, standardAbsorption);

        // Crops (Block instances)
        MAP.put(Blocks.WHEAT, standardAbsorption);
        MAP.put(Blocks.CARROTS, standardAbsorption);
        MAP.put(Blocks.POTATOES, standardAbsorption);
        MAP.put(Blocks.BEETROOTS, standardAbsorption);

        // Flowers (Specific instances) - Add more as needed
        MAP.put(Blocks.DANDELION, standardAbsorption);
        MAP.put(Blocks.POPPY, standardAbsorption);
        MAP.put(Blocks.BLUE_ORCHID, standardAbsorption);
        MAP.put(Blocks.ALLIUM, standardAbsorption);
        MAP.put(Blocks.AZURE_BLUET, standardAbsorption);
        MAP.put(Blocks.RED_TULIP, standardAbsorption);
        MAP.put(Blocks.ORANGE_TULIP, standardAbsorption);
        MAP.put(Blocks.WHITE_TULIP, standardAbsorption);
        MAP.put(Blocks.PINK_TULIP, standardAbsorption);
        MAP.put(Blocks.OXEYE_DAISY, standardAbsorption);
        MAP.put(Blocks.CORNFLOWER, standardAbsorption);
        MAP.put(Blocks.LILY_OF_THE_VALLEY, standardAbsorption);
        MAP.put(Blocks.SUNFLOWER, standardAbsorption); // Tall flower
        MAP.put(Blocks.LILAC, standardAbsorption);     // Tall flower
        MAP.put(Blocks.ROSE_BUSH, standardAbsorption); // Tall flower
        MAP.put(Blocks.PEONY, standardAbsorption);     // Tall flower

        // Saplings (Higher value)
        MAP.put(Blocks.OAK_SAPLING, saplingAbsorption);
        MAP.put(Blocks.SPRUCE_SAPLING, saplingAbsorption);
        MAP.put(Blocks.BIRCH_SAPLING, saplingAbsorption);
        MAP.put(Blocks.JUNGLE_SAPLING, saplingAbsorption);
        MAP.put(Blocks.ACACIA_SAPLING, saplingAbsorption);
        MAP.put(Blocks.DARK_OAK_SAPLING, saplingAbsorption);
    }

    public static int get(Block block) {
        return MAP.getOrDefault(block, 0);
    }

    public static void add(Block block, int amount) {
        MAP.put(block, amount);
    }
}