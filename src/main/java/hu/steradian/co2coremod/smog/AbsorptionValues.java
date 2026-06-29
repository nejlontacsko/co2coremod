package hu.steradian.co2coremod.smog;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class AbsorptionValues {
    private static final int LOW_ABSORPTION = 1;
    private static final int STANDARD_ABSORPTION = 2;
    private static final int SAPLING_ABSORPTION = 6;
    private static final int STRONG_STATIC_ABSORPTION = 10;

    // Map to store CO2 absorption values per block.
    private static final Map<Block, Integer> MAP = new HashMap<>();

    static {
        putAll(LOW_ABSORPTION,
            Blocks.BAMBOO,
            Blocks.CHERRY_SAPLING,
            Blocks.MANGROVE_PROPAGULE,
            Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA,
            Blocks.SMALL_DRIPLEAF,
            Blocks.BIG_DRIPLEAF,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT
        );

        putAll(STANDARD_ABSORPTION,
            Blocks.SHORT_GRASS,
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.LARGE_FERN,
            Blocks.VINE,
            Blocks.SUGAR_CANE,
            Blocks.WHEAT,
            Blocks.CARROTS,
            Blocks.POTATOES,
            Blocks.BEETROOTS,
            Blocks.DANDELION,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER,
            Blocks.LILY_OF_THE_VALLEY,
            Blocks.SUNFLOWER,
            Blocks.LILAC,
            Blocks.ROSE_BUSH,
            Blocks.PEONY
        );

        putAll(SAPLING_ABSORPTION,
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.DARK_OAK_SAPLING
        );

        // Strong static absorbers. These do not mature into other blocks on their own.
        putAll(STRONG_STATIC_ABSORPTION,
            Blocks.MOSS_BLOCK,
            Blocks.MOSS_CARPET
        );
    }

    private static void putAll(int absorption, Block... blocks) {
        for (Block block : blocks) {
            MAP.put(block, absorption);
        }
    }

    public static int get(Block block) {
        return MAP.getOrDefault(block, 0);
    }

    public static void add(Block block, int amount) {
        MAP.put(block, amount);
    }
}