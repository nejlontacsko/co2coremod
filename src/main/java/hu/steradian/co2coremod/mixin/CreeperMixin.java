package hu.steradian.co2coremod.mixin;

import hu.steradian.co2coremod.smog.SmogHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"))
    private void onExplode(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel))
            return;
        if (serverLevel.dimension() != Level.OVERWORLD)
            return;

        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(
                self.blockPosition().getX() >> 4,
                self.blockPosition().getZ() >> 4
        );
        if (chunk != null)
            SmogHandler.add(chunk, 2000);
    }
}