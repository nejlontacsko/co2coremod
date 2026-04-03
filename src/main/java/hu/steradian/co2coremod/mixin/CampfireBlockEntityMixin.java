package hu.steradian.co2coremod.mixin;

import hu.steradian.co2coremod.smog.SmogHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @Unique
    private static final int CAMPFIRE_TICK_PPM = 1;

    @Inject(method = "cookTick", at = @At("TAIL"))
    private static void onCookTick(
            ServerLevel serverLevel,
            BlockPos blockPos,
            BlockState blockState,
            CampfireBlockEntity campfireBlockEntity,
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> cachedCheck,
            CallbackInfo ci) {

        if (!blockState.getValue(CampfireBlock.LIT)) return;

        chunkBasedApproach(CAMPFIRE_TICK_PPM, serverLevel, blockPos);
    }

    @Unique
    private static void chunkBasedApproach(int amount, ServerLevel serverLevel, BlockPos blockPos) {
        ChunkPos pos = new ChunkPos(blockPos);
        LevelChunk chunk = serverLevel.getChunk(pos.x, pos.z);

        SmogHandler.add(chunk, amount);
    }
}