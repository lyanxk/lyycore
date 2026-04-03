package org.lyy.lyycore.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class ImaginaryEnergyStorage implements ImaginaryEnergy, IEnergyStorage {
    protected int imaginaryEnergy;
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public ImaginaryEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public void setEnergy(int v) { energy = Math.max(0, Math.min(v, capacity)); }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate && received > 0) energy += received;
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate && extracted > 0) energy -= extracted;
        return extracted;
    }

    public int getImaginaryEnergyStored() { return imaginaryEnergy; }
    public void setImaginaryEnergy(int v) { imaginaryEnergy = Math.max(0, v); }

    public int receiveImaginaryEnergy(int amount) {
        imaginaryEnergy += amount;
        return amount;
    }

    public int extractImaginaryEnergy(int amount) {
        int extracted = Math.min(imaginaryEnergy, amount);
        imaginaryEnergy -= extracted;
        return extracted;
    }

    @Override public int getEnergyStored() { return energy; }
    @Override public int getMaxEnergyStored() { return capacity; }
    @Override public boolean canExtract() { return maxExtract > 0; }
    @Override public boolean canReceive() { return maxReceive > 0; }
}
