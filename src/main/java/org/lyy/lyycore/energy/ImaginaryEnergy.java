package org.lyy.lyycore.energy;

/** Native IE, independent of the FE compatibility capability. */
public interface ImaginaryEnergy {
    int getImaginaryEnergyStored();
    int getMaxImaginaryEnergyStored();
    boolean canExtractImaginaryEnergy();
    boolean canReceiveImaginaryEnergy();
    int extractImaginaryEnergy(int amount, boolean simulate);
    int receiveImaginaryEnergy(int amount, boolean simulate);

    default int receiveImaginaryEnergy(int amount) { return receiveImaginaryEnergy(amount, false); }
    default int extractImaginaryEnergy(int amount) { return extractImaginaryEnergy(amount, false); }
}
