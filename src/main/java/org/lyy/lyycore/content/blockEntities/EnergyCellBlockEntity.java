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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.Config;
import org.lyy.lyycore.content.menu.EnergyCellMenu;
import org.lyy.lyycore.energy.IEnergyConversion;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.energy.ImaginaryEnergyFeAdapter;
import org.lyy.lyycore.energy.ImaginaryEnergy;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyCapabilities;
import org.lyy.lyycore.content.blocks.EnergyCellBlock;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public class EnergyCellBlockEntity extends BlockEntity implements MenuProvider {
    private final ImaginaryEnergyStorage storage = new ImaginaryEnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, this::setChanged);
    private final ImaginaryEnergyFeAdapter feAdapter = new ImaginaryEnergyFeAdapter(
            storage, this::setChanged, 1_000_000_000, 1_000_000_000);
    // Only old saves can contain an extra whole-FE buffer. Preserve any amount
    // that cannot yet fit into IE; all new transfers use the single IE store.
    private int legacyEnergyFE;

    // Container data packets carry signed 16-bit values in 1.21.1, so each
    // 32-bit energy value is split into two words for lossless GUI syncing.
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int value = switch (index / 2) {
                case 0 -> storage.getImaginaryEnergyStored();
                case 1 -> getMaxIEnergyStored();
                default -> 0;
            };
            return index % 2 == 0 ? value & 0xFFFF : value >>> 16 & 0xFFFF;
        }

        @Override
        public void set(int index, int value) {
            // The server owns energy state. Client-side values live in the
            // SimpleContainerData supplied by EnergyCellMenu.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
        }

        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IMAGINARY_ENERGY_CELL.get(), pos, state);
    }

    @Override
    public Component getDisplayName() { return Component.translatable("block.lyycore.imaginary_energy_cell"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EnergyCellMenu(id, inv, this, data);
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        normalizeLegacyFeToIe();
        tag.putInt("IE", storage.getImaginaryEnergyStored());
        int fe = legacyEnergyFE + feAdapter.getRemainder();
        if (fe < IEnergyConversion.FE_PER_IMAGINARY) tag.putByte("energy", (byte) fe);
        else tag.putInt("energy", fe); // Keep unconverted FE from legacy saves intact.
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.setImaginaryEnergy(tag.getInt("IE"));
        int savedFE = Math.clamp(tag.getInt("energy"), 0, 1_000_000_000);
        feAdapter.setRemainder(savedFE % IEnergyConversion.FE_PER_IMAGINARY);
        legacyEnergyFE = savedFE - feAdapter.getRemainder();
        normalizeLegacyFeToIe();
        items.deserializeNBT(registries, tag.getCompound("items"));
    }

    // --- Accessors ---
    public IEnergyStorage getEnergyStorage() { return feAdapter; }
    public ImaginaryEnergyStorage getImaginaryEnergyStorage() { return storage; }
    public ItemStackHandler getItemHandler() { return items; }
    public ContainerData getData() { return data; }
    public int getIEnergyStored() { return storage.getImaginaryEnergyStored(); }
    public int getMaxIEnergyStored() { return Integer.MAX_VALUE; }

    // --- Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCellBlockEntity be) {
        if (level == null || level.isClientSide) return;

        boolean changed = be.normalizeLegacyFeToIe();

        int interval = Config.ENERGY_CELL_TRANSFER_INTERVAL.get();
        if (level.getGameTime() % interval == 0) {
            int ieBurst = (int) Math.min((long) Config.ENERGY_CELL_IE_RATE.get() * interval, Integer.MAX_VALUE);
            changed |= be.pushIEToNeighbors(ieBurst);
        }

        changed |= be.chargeBattery();
        if (changed) be.setChanged();
        // Update the visible gauge only when crossing a segment threshold.
        int stored = be.getIEnergyStored();
        int charge = stored <= 0 ? 0 : (int) Math.min(5,
                ((long) stored * 5 + be.getMaxIEnergyStored() - 1) / be.getMaxIEnergyStored());
        if (state.getValue(EnergyCellBlock.CHARGE) != charge) {
            level.setBlock(pos, state.setValue(EnergyCellBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
        }
    }

    private boolean normalizeLegacyFeToIe() {
        int feBuffered = legacyEnergyFE;
        int ieToAdd = IEnergyConversion.fromFE(feBuffered);
        if (ieToAdd <= 0) return false;

        int accepted = storage.receiveImaginaryEnergy(ieToAdd);
        if (accepted <= 0) return false;
        legacyEnergyFE -= IEnergyConversion.toFE(accepted);
        return true;
    }

    private boolean pushIEToNeighbors(int ieBurst) {
        if (level == null || ieBurst <= 0) return false;
        int remainingIE = Math.min(ieBurst, storage.getImaginaryEnergyStored());
        if (remainingIE <= 0) return false;
        boolean changed = false;

        for (Direction dir : Direction.values()) {
            if (remainingIE <= 0) break;

            ImaginaryEnergy nativeTarget = level.getCapability(LyyCapabilities.IMAGINARY_ENERGY,
                    worldPosition.relative(dir), dir.getOpposite());
            if (nativeTarget != null) {
                if (nativeTarget.canReceiveImaginaryEnergy()) {
                    int accepted = nativeTarget.receiveImaginaryEnergy(remainingIE, false);
                    storage.extractImaginaryEnergy(accepted);
                    remainingIE -= accepted;
                    changed |= accepted > 0;
                }
                continue;
            }

            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (target == null) continue;

            int feSendCap = IEnergyConversion.toFE(remainingIE);

            int feAcceptedSim = target.receiveEnergy(feSendCap, true);
            if (feAcceptedSim <= 0) continue;

            // Deliberately plan whole IE only: offers below 100 FE are skipped;
            // the fractional FE part is not carried into a later output transfer.
            int ieNeeded = IEnergyConversion.fromFE(feAcceptedSim);
            if (ieNeeded <= 0) continue;

            int ieExtracted = storage.extractImaginaryEnergy(ieNeeded);
            if (ieExtracted <= 0) continue;

            int feExact = IEnergyConversion.toFE(ieExtracted);
            int feAcceptedReal = target.receiveEnergy(feExact, false);

            if (feAcceptedReal < feExact) {
                // Policy: discard the sub-100 FE refund remainder instead of buffering it.
                int ieRefund = IEnergyConversion.fromFE(feExact - feAcceptedReal);
                if (ieRefund > 0) storage.receiveImaginaryEnergy(ieRefund);
            }
            int refundedIE = IEnergyConversion.fromFE(feExact - feAcceptedReal);
            remainingIE -= Math.max(ieExtracted - refundedIE, 0);
            changed |= feAcceptedReal > 0;
        }
        return changed;
    }

    private boolean chargeBattery() {
        if (items.getSlots() <= 0) return false;
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty()) return false;

        IEnergyStorage batt = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (batt == null) return false;

        int ieBudget = Math.min(Config.ENERGY_CELL_IE_RATE.get(), storage.getImaginaryEnergyStored());
        if (ieBudget <= 0) return false;

        int fePlan = IEnergyConversion.toFE(ieBudget);

        int feAcceptedSim = batt.receiveEnergy(fePlan, true);
        if (feAcceptedSim <= 0) return false;

        // Charging follows the same whole-IE output policy; below 100 FE is skipped.
        int ieNeed = IEnergyConversion.fromFE(feAcceptedSim);
        int ieExtracted = storage.extractImaginaryEnergy(ieNeed);
        if (ieExtracted <= 0) return false;

        int feExact = IEnergyConversion.toFE(ieExtracted);
        int feAcceptedReal = batt.receiveEnergy(feExact, false);
        if (feAcceptedReal < feExact) {
            // Policy: discard the sub-100 FE refund remainder instead of buffering it.
            int ieRefund = IEnergyConversion.fromFE(feExact - feAcceptedReal);
            if (ieRefund > 0) storage.receiveImaginaryEnergy(ieRefund);
        }
        return feAcceptedReal > 0;
    }
}
