package hu.steradian.co2coremod.smog;

import hu.steradian.co2coremod.Co2CoreMod;

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
    private static final int SURFACE_SCAN_BELOW = 16;
    private static final int SURFACE_SCAN_ABOVE = 16;
    private static final int MAGMA_SCAN_MAX_Y = 48;
    private static final long SLOW_SCAN_LOG_THRESHOLD_NS = 5_000_000L;

    private SmogCalculator() {}

    public static int calculateSmogChange(LevelChunk chunk) {
        long startNs = System.nanoTime();

        Level level = chunk.getLevel();
        if (level.isClientSide())
            return 0;

        int decreaseAmount = 0;
        int increaseAmount = 0;
        int scannedBlocks = 0;
        int nonAirBlocks = 0;
        int adjacencyChecks = 0;
        int fireBlocks = 0;
        int campfireBlocks = 0;
        int lavaBlocks = 0;
        int leafBlocks = 0;

        int chunkXStart = chunk.getPos().getMinBlockX();
        int chunkZStart = chunk.getPos().getMinBlockZ();
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        boolean surfaceScan = shouldRunSurfaceScan(level, chunk);
        String scanMode = surfaceScan ? "surface" : "magma";

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int fromY;
                int toY;

                if (surfaceScan) {
                    fromY = Math.max(surfaceY - SURFACE_SCAN_BELOW, minY);
                    toY = Math.min(surfaceY + SURFACE_SCAN_ABOVE, maxY);
                } else {
                    fromY = minY;
                    toY = Math.min(MAGMA_SCAN_MAX_Y, maxY);
                }

                for (int y = fromY; y < toY; y++) {
                    scannedBlocks++;
                    pos.set(chunkXStart + x, y, chunkZStart + z);

                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir())
                        continue;

                    nonAirBlocks++;
                    Block block = state.getBlock();
                    if (block instanceof LeavesBlock)
                        leafBlocks++;
                    decreaseAmount += (block instanceof LeavesBlock) ? LEAVES_ABSORPTION : AbsorptionValues.get(block);

                    if (block instanceof CampfireBlock
                            && state.hasProperty(CampfireBlock.LIT)
                            && state.getValue(CampfireBlock.LIT)) {
                        increaseAmount += CAMPFIRE_EMISSION;
                        campfireBlocks++;
                    }

                    if (state.getFluidState().is(FluidTags.LAVA)) {
                        increaseAmount += LAVA_EMISSION;
                        lavaBlocks++;
                    }

                    if (isFire(state)) {
                        increaseAmount += FIRE_EMISSION;
                        fireBlocks++;
                        int adjacentNonAirBlocks = countAdjacentNonAirBlocks(chunk, pos);
                        adjacencyChecks += 6;
                        increaseAmount += adjacentNonAirBlocks * FIRE_EMISSION;
                        continue;
                    }
                }
            }
        }

        int delta = increaseAmount - decreaseAmount;
        long elapsedNs = System.nanoTime() - startNs;

        if (elapsedNs >= SLOW_SCAN_LOG_THRESHOLD_NS) {
            Co2CoreMod.LOGGER.info(
                    "co2.telemetry event=smog_scan_slow scan_mode={} chunk_x={} chunk_z={} duration_ms={} scanned_blocks={} non_air_blocks={} adjacency_checks={} fire_blocks={} campfire_blocks={} lava_blocks={} leaf_blocks={} increase={} decrease={} delta={}",
                    scanMode,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    elapsedNs / 1_000_000.0,
                    scannedBlocks,
                    nonAirBlocks,
                    adjacencyChecks,
                    fireBlocks,
                    campfireBlocks,
                    lavaBlocks,
                    leafBlocks,
                    increaseAmount,
                    decreaseAmount,
                    delta
            );
        }

        return delta;
    }

    private static boolean shouldRunSurfaceScan(Level level, LevelChunk chunk) {
        long scanPhase = level.getGameTime() / 20L;
        int chunkParity = Math.floorMod(chunk.getPos().x * 31 + chunk.getPos().z, 2);
        return ((scanPhase + chunkParity) & 1L) == 0L;
    }

    private static int countAdjacentNonAirBlocks(LevelChunk chunk, BlockPos pos) {
        int count = 0;

        if (!chunk.getBlockState(pos.above()).isAir()) count++;
        if (!chunk.getBlockState(pos.below()).isAir()) count++;
        if (!chunk.getBlockState(pos.north()).isAir()) count++;
        if (!chunk.getBlockState(pos.south()).isAir()) count++;
        if (!chunk.getBlockState(pos.east()).isAir()) count++;
        if (!chunk.getBlockState(pos.west()).isAir()) count++;

        return count;
    }

    private static boolean isFire(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock;
    }
}
