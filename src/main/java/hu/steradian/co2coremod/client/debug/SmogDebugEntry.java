package hu.steradian.co2coremod.client.debug;

import hu.steradian.co2coremod.Co2CoreMod;
import hu.steradian.co2coremod.client.ClientSmogData;
import hu.steradian.co2coremod.components.ModComponents;
import hu.steradian.co2coremod.smog.SmogLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class SmogDebugEntry implements DebugScreenEntry {

    @Override
    public void display(@NotNull DebugScreenDisplayer displayer,
                        @Nullable Level level,
                        @Nullable LevelChunk clientChunk,
                        @Nullable LevelChunk serverChunk) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        ChunkPos playerChunkPos = new ChunkPos(mc.player.blockPosition());
        ClientSmogData.tick();
        int playerSmog = ClientSmogData.getSmogAmount(playerChunkPos);
        //int playerSmog = ModComponents.CHUNK_DATA.get(new LevelChunk(level, playerChunkPos)).getSmogAmount();

        displayer.addToGroup(Co2CoreMod.getId("smog"), "[Smog]");
        displayer.addToGroup(Co2CoreMod.getId("smog"),
      "Player chunk: " + playerChunkPos
            + " | Smog: " + playerSmog + " ppm"
            + " | Level: " + SmogLevel.of(playerSmog)
        );

        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos lookedPos = blockHitResult.getBlockPos();
            ChunkPos lookedChunkPos = new ChunkPos(lookedPos);
            int lookedSmog = ClientSmogData.getSmogAmount(lookedChunkPos);

            displayer.addToGroup(
                    hu.steradian.co2coremod.Co2CoreMod.getId("smog"),
                    "Looked chunk: " + lookedChunkPos
                            + " | Smog: " + lookedSmog + " ppm"
                            + " | Level: " + SmogLevel.of(lookedSmog)
            );
            displayer.addToGroup(
                    hu.steradian.co2coremod.Co2CoreMod.getId("smog"),
                    "Looked block: " + lookedPos.getX()
                            + ", " + lookedPos.getY()
                            + ", " + lookedPos.getZ()
            );
        }
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }
}