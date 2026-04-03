package hu.steradian.co2coremod.api;

import hu.steradian.co2coremod.smog.SmogHandler;

import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ISmogNode {
    class Result
    {
        public boolean succeed;
        public int delta;

        public Result(int previous, int current, int intended)
        {
            this.delta = Mth.abs(previous - current);
            this.succeed = delta == intended;
        }
    }

    default Result emitSmog(LevelChunk chunk, int amount) {
        int prev = SmogHandler.getChunkAmount(chunk);
        SmogHandler.add(chunk, amount);
        return new Result(prev, SmogHandler.getChunkAmount(chunk), amount);
    }

    default Result absorbSmog(LevelChunk chunk, int amount) {
        int prev = SmogHandler.getChunkAmount(chunk);
        SmogHandler.add(chunk, -amount);
        return new Result(prev, SmogHandler.getChunkAmount(chunk), amount);
    }
}
