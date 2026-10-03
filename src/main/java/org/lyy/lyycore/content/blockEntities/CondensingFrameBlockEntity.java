package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.lyy.lyycore.content.blocks.CondensingFrameBlock;
import org.lyy.lyycore.content.FrameProduction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.lyy.lyycore.energy.ImaginaryEnergy;
import org.lyy.lyycore.energy.ImaginaryEnergyFeAdapter;

public abstract class CondensingFrameBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = Integer.MAX_VALUE;
    public static final int OUTPUT_CAPACITY = 1024;
    private int energyIE;
    private int passiveTicks;
    private ItemStack storedItem = ItemStack.EMPTY;
    private int outputCount;

    private final ImaginaryEnergy imaginaryEnergy = new ImaginaryEnergy() {
        @Override public int receiveImaginaryEnergy(int offered, boolean simulate) {
            int accepted = Math.min(Math.max(0, offered), CAPACITY - energyIE);
            if (!simulate && accepted > 0) {
                energyIE += accepted;
                setChanged();
            }
            return accepted;
        }
        @Override public int extractImaginaryEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getImaginaryEnergyStored() { return energyIE; }
        @Override public int getMaxImaginaryEnergyStored() { return CAPACITY; }
        @Override public boolean canExtractImaginaryEnergy() { return false; }
        @Override public boolean canReceiveImaginaryEnergy() { return true; }
    };
    private final ImaginaryEnergyFeAdapter energy = new ImaginaryEnergyFeAdapter(imaginaryEnergy, this::setChanged);

    // Expose one ordinary stack at a time. The bulk count never enters ItemStack's
    // count codec or vanilla cursor/hotbar logic, which cannot safely hold 1024.
    private final IItemHandlerModifiable output = new IItemHandlerModifiable() {
        private void check(int slot) { if (slot != 0) throw new IndexOutOfBoundsException(slot); }
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) {
            check(slot);
            return outputCount == 0 ? ItemStack.EMPTY : storedItem.copyWithCount(Math.min(outputCount, storedItem.getMaxStackSize()));
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { check(slot); return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            check(slot);
            int taken = Math.min(Math.max(0, amount), Math.min(outputCount, storedItem.getMaxStackSize()));
            if (taken == 0) return ItemStack.EMPTY;
            ItemStack result = storedItem.copyWithCount(taken);
            if (!simulate) {
                outputCount -= taken;
                if (outputCount == 0) storedItem = ItemStack.EMPTY;
                setChanged();
            }
            return result;
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            ItemStack visible = getStackInSlot(slot);
            if (stack.isEmpty()) extractItem(slot, visible.getCount(), false);
            else if (ItemStack.isSameItemSameComponents(stack, visible) && stack.getCount() <= visible.getCount())
                extractItem(slot, visible.getCount() - stack.getCount(), false);
        }
        @Override public int getSlotLimit(int slot) { check(slot); return OUTPUT_CAPACITY; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { check(slot); return false; }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> energyIE & 0xFFFF;
                case 1 -> energyIE >>> 16;
                case 2 -> outputCount;
                case 3 -> getBlockState().getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false) ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) { }
        @Override public int getCount() { return 4; }
    };

    protected CondensingFrameBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    public IEnergyStorage getEnergyStorage() { return energy; }
    public ImaginaryEnergy getImaginaryEnergyStorage() { return imaginaryEnergy; }
    public IItemHandlerModifiable getOutput() { return output; }
    public ContainerData getData() { return data; }
    public FrameProduction production() { return ((CondensingFrameBlock) getBlockState().getBlock()).production(); }
    public int getEnergyIE() { return energyIE; }
    public int getOutputCount() { return outputCount; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IE", energyIE);
        tag.putByte("FERemainder", energy.getRemainder());
        tag.putInt("PassiveTicks", passiveTicks);
        // Keep the original keys so existing crystal and resource frames load unchanged.
        tag.putInt("CrystalCount", outputCount);
        if (!storedItem.isEmpty()) tag.put("Crystal", storedItem.copyWithCount(1).save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyIE = Math.max(0, tag.getInt("IE"));
        energy.setRemainder(tag.getInt("FERemainder"));
        passiveTicks = Math.clamp(tag.getInt("PassiveTicks"), 0, 19);
        storedItem = ItemStack.parseOptional(registries, tag.getCompound("Crystal"));
        outputCount = storedItem.isEmpty() ? 0 : Math.clamp(tag.getInt("CrystalCount"), 0, OUTPUT_CAPACITY);
        if (outputCount == 0) storedItem = ItemStack.EMPTY;
        else storedItem.setCount(1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CondensingFrameBlockEntity be) {
        if (level.isClientSide) return;
        if (++be.passiveTicks == 20) {
            be.passiveTicks = 0;
            if (be.energyIE < CAPACITY) {
                be.energyIE += be.production().passiveEnergyPerSecond();
            }
            // Retry blocked output even if the last production used all energy.
            be.pushOutput();
        }
        be.setChanged();
        be.produce(state);
    }

    protected abstract void produce(BlockState state);

    /** Shared energy, bulk storage and export path for all condensing frames. */
    protected void produceItem(ItemStack result, int cost) {
        cost = production().energyCost(cost);
        int batchSize = production().batchSize();
        if (cost <= 0 || result.isEmpty() || energyIE < cost) return;
        if (outputCount > OUTPUT_CAPACITY - batchSize || (!storedItem.isEmpty() && !ItemStack.isSameItemSameComponents(storedItem, result))) return;
        energyIE -= cost;
        storedItem = result.copyWithCount(1);
        outputCount += batchSize;
        setChanged();
        pushOutput();
    }

    private void pushOutput() {
        for (Direction side : new Direction[]{Direction.UP, Direction.DOWN}) {
            BlockPos targetPos = worldPosition.relative(side);
            if (outputCount == 0 || !level.hasChunkAt(targetPos)) continue;
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, side.getOpposite());
            if (target == null || target == output) continue;
            while (outputCount > 0) {
                ItemStack offered = output.getStackInSlot(0);
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, offered.copy(), false);
                int moved = offered.getCount() - remaining.getCount();
                if (moved <= 0) break;
                output.extractItem(0, moved, false);
            }
        }
    }
}
