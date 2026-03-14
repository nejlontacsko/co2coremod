package hu.steradian.co2coremod.mixin;

import hu.steradian.co2coremod.smog.SmogManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Unique
    private static final int BURN_TICK_PPM = 1;
    @Unique
    private static final int ADVANCED_MULTIPLIER = 2;

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void onServerTick(
            ServerLevel serverLevel,
            BlockPos blockPos,
            BlockState blockState,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfo ci) {

        int amount = BURN_TICK_PPM;
        if (furnace instanceof BlastFurnaceBlockEntity || furnace instanceof SmokerBlockEntity)
            amount *= ADVANCED_MULTIPLIER;

        if (blockState.getValue(AbstractFurnaceBlock.LIT))
            chunkBasedApproach(amount, serverLevel, blockPos);
    }

    @Unique
    private static void chunkBasedApproach(int amount, ServerLevel serverLevel, BlockPos blockPos) {
        ChunkPos pos = new ChunkPos(blockPos);
        LevelChunk chunk = serverLevel.getChunk(pos.x, pos.z);

        SmogManager.add(chunk, amount);
        //Co2CoreMod.LOGGER.info("Emit " + amount + " ppm on Chunk " + chunk.getPos());
    }
}