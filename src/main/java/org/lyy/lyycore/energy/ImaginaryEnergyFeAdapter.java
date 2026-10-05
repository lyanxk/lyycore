package org.lyy.lyycore.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** Converts only at the FE boundary; an incomplete IE is kept as 0..99 FE. */
public final class ImaginaryEnergyFeAdapter implements IEnergyStorage {
    private static final int FE_PER_IE = IEnergyConversion.FE_PER_IMAGINARY;
    private final ImaginaryEnergy storage;
    private final Runnable changed;
    private final int maxReceive, maxExtract;
    private byte remainder;

    public ImaginaryEnergyFeAdapter(ImaginaryEnergy storage, Runnable changed) {
        this(storage, changed, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public ImaginaryEnergyFeAdapter(ImaginaryEnergy storage, Runnable changed, int maxReceive, int maxExtract) {
        this.storage = storage;
        this.changed = changed;
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
    }

    public byte getRemainder() { return remainder; }
    public void setRemainder(int value) { remainder = (byte) Math.clamp(value, 0, FE_PER_IE - 1); }

    @Override
    public int receiveEnergy(int offered, boolean simulate) {
        offered = Math.min(offered, maxReceive);
        long room = (long) (storage.getMaxImaginaryEnergyStored() - storage.getImaginaryEnergyStored()) * FE_PER_IE - remainder;
        if (offered <= 0 || room <= 0 || !canReceive()) return 0;
        int accepted = (int) Math.min(offered, room);
        int ie = (int) (((long) accepted + remainder) / FE_PER_IE);
        int allowed = storage.receiveImaginaryEnergy(ie, true);
        if (allowed < ie) {
            accepted = (int) Math.max(0, (long) allowed * FE_PER_IE - remainder);
            ie = allowed;
        }
        if (!simulate && accepted > 0) {
            int received = storage.receiveImaginaryEnergy(ie, false);
            if (received < ie) accepted = (int) Math.max(0, (long) received * FE_PER_IE - remainder);
            remainder = (byte) (remainder + (long) accepted - (long) received * FE_PER_IE);
            changed.run();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !canExtract()) return 0;
        int extracted = Math.min(Math.min(amount, maxExtract), getEnergyStored());
        int needed = Math.max(0, extracted - remainder);
        int ie = needed / FE_PER_IE + (needed % FE_PER_IE == 0 ? 0 : 1);
        int allowed = storage.extractImaginaryEnergy(ie, true);
        if (allowed < ie) {
            extracted = (int) Math.min(extracted, (long) allowed * FE_PER_IE + remainder);
            ie = allowed;
        }
        if (!simulate && extracted > 0) {
            int removed = storage.extractImaginaryEnergy(ie, false);
            extracted = (int) Math.min(extracted, (long) removed * FE_PER_IE + remainder);
            remainder = (byte) (remainder + (long) removed * FE_PER_IE - extracted);
            changed.run();
        }
        return extracted;
    }

    @Override public int getEnergyStored() {
        int whole = IEnergyConversion.toFE(storage.getImaginaryEnergyStored());
        return whole > Integer.MAX_VALUE - remainder ? Integer.MAX_VALUE : whole + remainder;
    }
    @Override public int getMaxEnergyStored() { return IEnergyConversion.toFE(storage.getMaxImaginaryEnergyStored()); }
    @Override public boolean canExtract() { return maxExtract > 0 && storage.canExtractImaginaryEnergy(); }
    @Override public boolean canReceive() { return maxReceive > 0 && storage.canReceiveImaginaryEnergy(); }
}
