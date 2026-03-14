package hu.steradian.co2coremod.smog;

import net.minecraft.util.Mth;

public enum SmogLevel {
    CLEAN,
    LOW,
    MEDIUM,
    HIGH;

    public static final int MIN = 0;
    public static final int MAX = 500000;

    public static SmogLevel of(int i) {
        if (i < 100)
            return CLEAN;
        else if (i < 1000)
            return LOW;
        else if (i < 2500)
            return MEDIUM;
        return HIGH;
    }

    public static int clamp(int amount) {
        return Mth.clamp(amount, MIN, MAX);
    }
}
