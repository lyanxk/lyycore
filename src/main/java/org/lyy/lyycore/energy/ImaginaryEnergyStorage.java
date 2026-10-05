package org.lyy.lyycore.energy;

/** The sole whole-IE balance. FE conversion and fractional FE live in the adapter. */
public final class ImaginaryEnergyStorage implements ImaginaryEnergy {
    private int imaginaryEnergy;
    private final int capacity, maxReceive, maxExtract;
    private final Runnable changed;

    public ImaginaryEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable changed) {
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0)
            throw new IllegalArgumentException("Energy capacity and transfer limits must be nonnegative");
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.changed = java.util.Objects.requireNonNull(changed);
    }

    public ImaginaryEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, () -> {});
    }

    @Override public int getImaginaryEnergyStored() { return imaginaryEnergy; }
    @Override public int getMaxImaginaryEnergyStored() { return capacity; }
    @Override public boolean canReceiveImaginaryEnergy() { return maxReceive > 0; }
    @Override public boolean canExtractImaginaryEnergy() { return maxExtract > 0; }

    /** Internal consumption and save restoration are independent of external I/O permissions. */
    public void setImaginaryEnergy(int value) {
        int next = Math.clamp(value, 0, capacity);
        if (next != imaginaryEnergy) {
            imaginaryEnergy = next;
            changed.run();
        }
    }

    @Override public int receiveImaginaryEnergy(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), Math.min(maxReceive, capacity - imaginaryEnergy));
        if (!simulate && accepted > 0) setImaginaryEnergy(imaginaryEnergy + accepted);
        return accepted;
    }

    @Override public int extractImaginaryEnergy(int amount, boolean simulate) {
        int extracted = Math.min(Math.max(0, amount), Math.min(maxExtract, imaginaryEnergy));
        if (!simulate && extracted > 0) setImaginaryEnergy(imaginaryEnergy - extracted);
        return extracted;
    }
}
