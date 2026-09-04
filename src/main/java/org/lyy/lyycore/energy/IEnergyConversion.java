package org.lyy.lyycore.energy;

public final class IEnergyConversion {
    public static final int FE_PER_IMAGINARY = 100;

    public static int toFE(int imaginaryEnergy) {
        if (imaginaryEnergy <= 0) return 0;
        return (int) Math.min((long) imaginaryEnergy * FE_PER_IMAGINARY, Integer.MAX_VALUE);
    }

    public static int fromFE(int feEnergy) {
        return Math.max(feEnergy, 0) / FE_PER_IMAGINARY;
    }

    private IEnergyConversion() { }
}
