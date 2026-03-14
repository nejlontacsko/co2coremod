package hu.steradian.co2coremod.components;

import hu.steradian.co2coremod.smog.SmogLevel;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChunkSmogData implements IChunkSmogData {
    private int smogAmount = 0;
    private SmogLevel smogLevel = SmogLevel.CLEAN;
    private boolean initialized = false;

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
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public void readData(ValueInput readView) {
        this.setSmogAmount(readView.getIntOr("CO2Level", 0));
        this.setInitialized(readView.getBooleanOr("Initialized", false));
    }

    @Override
    public void writeData(ValueOutput writeView) {
        writeView.putInt("CO2Level", this.getSmogAmount());
        writeView.putBoolean("Initialized", this.isInitialized());
    }
}