package org.lyy.lyycore.energy;

public class IEnergyConversion {
    public static final double IMAGINARY_TO_FE = 100;
    public static final double FE_TO_IMAGINARY = 1 / IMAGINARY_TO_FE;

    public static int toFE(int imaginaryEnergy) {
        return (int) (imaginaryEnergy * IMAGINARY_TO_FE);
    }

    public static int fromFE(int feEnergy) {
        return (int) (feEnergy * FE_TO_IMAGINARY);
    }
}
