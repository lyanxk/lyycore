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

    // Container data packets carry signed 16-bit values in 1.21.1, so each
    // 32-bit energy value is split into two words for lossless GUI syncing.
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int value = index < 2 ? storage.getImaginaryEnergyStored() : getMaxIEnergyStored();
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
        tag.putInt("IE", storage.getImaginaryEnergyStored());
        tag.putByte("energy", feAdapter.getRemainder());
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.setImaginaryEnergy(tag.getInt("IE"));
        feAdapter.setRemainder(tag.getByte("energy"));
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
        int interval = Config.ENERGY_CELL_TRANSFER_INTERVAL.get();
        if (level.getGameTime() % interval == 0) {
            int ieBurst = (int) Math.min((long) Config.ENERGY_CELL_IE_RATE.get() * interval, Integer.MAX_VALUE);
            be.pushIEToNeighbors(ieBurst);
        }

        be.chargeBattery();
        // Update the visible gauge only when crossing a segment threshold.
        int stored = be.getIEnergyStored();
        int charge = (int) (((long) stored * 5 + be.getMaxIEnergyStored() - 1) / be.getMaxIEnergyStored());
        if (state.getValue(EnergyCellBlock.CHARGE) != charge) {
            level.setBlock(pos, state.setValue(EnergyCellBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
        }
    }

    private void pushIEToNeighbors(int ieBurst) {
        int remainingIE = Math.min(ieBurst, storage.getImaginaryEnergyStored());

        for (Direction dir : Direction.values()) {
            if (remainingIE <= 0) break;

            ImaginaryEnergy nativeTarget = level.getCapability(LyyCapabilities.IMAGINARY_ENERGY,
                    worldPosition.relative(dir), dir.getOpposite());
            if (nativeTarget != null) {
                int accepted = nativeTarget.receiveImaginaryEnergy(remainingIE, false);
                storage.extractImaginaryEnergy(accepted);
                remainingIE -= accepted;
                continue;
            }

            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (target != null) remainingIE -= transferFe(target, remainingIE);
        }
    }

    private void chargeBattery() {
        var battery = items.getStackInSlot(0).getCapability(Capabilities.EnergyStorage.ITEM);
        if (battery != null) transferFe(battery, Config.ENERGY_CELL_IE_RATE.get());
    }

    /** Offer whole IE, then charge for accepted FE rounded up to IE; no output remainder is retained. */
    private int transferFe(IEnergyStorage target, int ieBudget) {
        int offered = IEnergyConversion.toFE(Math.min(ieBudget, storage.getImaginaryEnergyStored()));
        int plannedIE = IEnergyConversion.fromFE(target.receiveEnergy(offered, true));
        if (plannedIE == 0) return 0;
        int accepted = target.receiveEnergy(IEnergyConversion.toFE(plannedIE), false);
        int spentIE = (int) (((long) accepted + IEnergyConversion.FE_PER_IMAGINARY - 1) / IEnergyConversion.FE_PER_IMAGINARY);
        storage.extractImaginaryEnergy(spentIE);
        return spentIE;
    }
}
