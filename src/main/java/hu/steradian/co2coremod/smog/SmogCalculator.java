package hu.steradian.co2coremod.smog;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SmogCalculator {
    private static final int LEAVES_ABSORPTION = 2;
    private static final int LAVA_EMISSION = 1;
    private static final int CAMPFIRE_EMISSION = 3;
    private static final int FIRE_EMISSION = 4;

    private SmogCalculator() {}

    public static int calculateSmogChange(LevelChunk chunk) {
        Level level = chunk.getLevel();
        if (level.isClientSide())
            return 0;

        int decreaseAmount = 0;
        int increaseAmount = 0;

        int chunkXStart = chunk.getPos().getMinBlockX();
        int chunkZStart = chunk.getPos().getMinBlockZ();
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int toY = Math.min(surfaceY + 16, maxY);

                for (int y = minY; y < toY; y++) {
                    pos.set(chunkXStart + x, y, chunkZStart + z);

                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir())
                        continue;

                    Block block = state.getBlock();
                    decreaseAmount += (block instanceof LeavesBlock) ? LEAVES_ABSORPTION : AbsorptionValues.get(block);

                    if (block instanceof CampfireBlock
                            && state.hasProperty(CampfireBlock.LIT)
                            && state.getValue(CampfireBlock.LIT))
                        increaseAmount += CAMPFIRE_EMISSION;

                    if (state.getFluidState().is(FluidTags.LAVA))
                        increaseAmount += LAVA_EMISSION;

                    if (isFire(state)) {
                        increaseAmount += FIRE_EMISSION;
                        continue;
                    }

                    if (isAdjacentToFire(chunk, pos))
                        increaseAmount += FIRE_EMISSION;
                }
            }
        }

        return increaseAmount - decreaseAmount;
    }

    private static boolean isAdjacentToFire(LevelChunk chunk, BlockPos pos) {
        return isFire(chunk.getBlockState(pos.above()))
                || isFire(chunk.getBlockState(pos.below()))
                || isFire(chunk.getBlockState(pos.north()))
                || isFire(chunk.getBlockState(pos.south()))
                || isFire(chunk.getBlockState(pos.east()))
                || isFire(chunk.getBlockState(pos.west()));
    }

    private static boolean isFire(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock;
    }
}
