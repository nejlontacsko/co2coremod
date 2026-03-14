package hu.steradian.co2coremod.components;

import hu.steradian.co2coremod.smog.SmogLevel;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChunkSmogData implements IChunkSmogData {
    private int smogAmount = 0;
    private SmogLevel smogLevel = SmogLevel.CLEAN;

    @Override
    public int getSmogAmount() {
        return smogAmount;
    }

    @Override
    public void setSmogAmount(int amount) {
        int val = SmogLevel.clamp(amount);
        this.smogAmount = val;
        this.smogLevel = SmogLevel.of(val);
    }

    @Override
    public SmogLevel getSmogLevel() {
        return smogLevel;
    }

    @Override
    public void readData(ValueInput readView) {
        this.setSmogAmount(readView.getIntOr("CO2Level", 0));
    }

    @Override
    public void writeData(ValueOutput writeView) {
        writeView.putInt("CO2Level", this.getSmogAmount());
    }
}