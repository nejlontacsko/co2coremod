package hu.steradian.co2coremod.co2;

public enum SmogLevel {
    CLEAN,
    LOW,
    MEDIUM,
    HIGH;

    public static SmogLevel of(int i) {
        if (i < 100)
            return CLEAN;
        else if (i < 1000)
            return LOW;
        else if (i < 2500)
            return MEDIUM;
        return HIGH;
    }
}
