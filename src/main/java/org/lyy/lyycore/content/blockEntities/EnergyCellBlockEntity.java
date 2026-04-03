package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.menu.EnergyCellMenu;
import org.lyy.lyycore.energy.IEnergyConversion;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.registry.LyyBlockEntities;

import javax.annotation.Nullable;

public class EnergyCellBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TRANSFER_INTERVAL_T = 2;

    private final ImaginaryEnergyStorage storage = new ImaginaryEnergyStorage(1_000_000_000, 1_000_000_000, 1_000_000_000);
    private int feRatePerTick = 1000;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IMAGINARY_ENERGY_CELL.get(), pos, state);
    }

    @Override
    public Component getDisplayName() { return Component.empty(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EnergyCellMenu(id, inv, this);
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IE", storage.getImaginaryEnergyStored());
        tag.putInt("energy", storage.getEnergyStored());
        tag.putInt("feRatePerTick", feRatePerTick);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.setEnergy(tag.getInt("energy"));
        storage.setImaginaryEnergy(tag.getInt("IE"));
        feRatePerTick = tag.getInt("feRatePerTick");
        items.deserializeNBT(registries, tag.getCompound("items"));
    }

    // --- Accessors ---
    public ImaginaryEnergyStorage getEnergyStorage() { return storage; }
    public ItemStackHandler getItemHandler() { return items; }
    public int getIEnergyStored() { return storage.getImaginaryEnergyStored(); }
    public int getMaxIEnergyStored() { return Integer.MAX_VALUE; }

    // --- Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCellBlockEntity be) {
        if (level == null || level.isClientSide) return;

        be.normalizeFeToIe();

        if (level.getGameTime() % TRANSFER_INTERVAL_T == 0) {
            int ieBurst = be.feRatePerTick * TRANSFER_INTERVAL_T;
            be.pushIEToNeighbors(ieBurst);
        }

        be.chargeBattery();
        be.setChanged();
    }

    private void normalizeFeToIe() {
        int feBuffered = storage.getEnergyStored();
        if (feBuffered <= 0) return;
        int feExtracted = storage.extractEnergy(feBuffered, false);
        if (feExtracted <= 0) return;
        int ieToAdd = IEnergyConversion.fromFE(feExtracted);
        storage.receiveImaginaryEnergy(ieToAdd);
    }

    private void pushIEToNeighbors(int ieBurst) {
        if (level == null || ieBurst <= 0) return;
        int remainingIE = Math.min(ieBurst, storage.getImaginaryEnergyStored());
        if (remainingIE <= 0) return;

        for (Direction dir : Direction.values()) {
            if (remainingIE <= 0) break;

            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (target == null) continue;

            int feSendCap = IEnergyConversion.toFE(remainingIE);
            if (feSendCap <= 0) continue;

            int feAcceptedSim = target.receiveEnergy(feSendCap, true);
            if (feAcceptedSim <= 0) continue;

            int ieNeeded = IEnergyConversion.fromFE(feAcceptedSim);
            if (ieNeeded <= 0) continue;

            int ieExtracted = storage.extractImaginaryEnergy(ieNeeded);
            if (ieExtracted <= 0) continue;

            int feExact = IEnergyConversion.toFE(ieExtracted);
            int feAcceptedReal = target.receiveEnergy(feExact, false);

            if (feAcceptedReal < feExact) {
                int ieRefund = IEnergyConversion.fromFE(feExact - feAcceptedReal);
                if (ieRefund > 0) storage.receiveImaginaryEnergy(ieRefund);
            } else {
                remainingIE -= ieExtracted;
            }
        }
    }

    private void chargeBattery() {
        if (items.getSlots() <= 0) return;
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty()) return;

        IEnergyStorage batt = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (batt == null) return;

        int ieBudget = Math.min(feRatePerTick, storage.getImaginaryEnergyStored());
        if (ieBudget <= 0) return;

        int fePlan = IEnergyConversion.toFE(ieBudget);
        if (fePlan <= 0) return;

        int feAcceptedSim = batt.receiveEnergy(fePlan, true);
        if (feAcceptedSim <= 0) return;

        int ieNeed = IEnergyConversion.fromFE(feAcceptedSim);
        int ieExtracted = storage.extractImaginaryEnergy(ieNeed);
        if (ieExtracted <= 0) return;

        int feExact = IEnergyConversion.toFE(ieExtracted);
        int feAcceptedReal = batt.receiveEnergy(feExact, false);
        if (feAcceptedReal < feExact) {
            int ieRefund = IEnergyConversion.fromFE(feExact - feAcceptedReal);
            if (ieRefund > 0) storage.receiveImaginaryEnergy(ieRefund);
        }
    }
}
