package org.lyy.lyycore.energy;

public interface ImaginaryEnergy {
    int getEnergyStored();
    int getMaxEnergyStored();
    boolean canExtract();
    boolean canReceive();
    int extractEnergy(int amount, boolean simulate);
    int receiveEnergy(int amount, boolean simulate);
}
