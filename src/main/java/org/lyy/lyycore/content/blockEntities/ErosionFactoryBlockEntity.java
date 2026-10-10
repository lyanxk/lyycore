package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.*;
import org.lyy.lyycore.content.blocks.ErosionFactoryBlock;
import org.lyy.lyycore.content.menu.ErosionFactoryMenu;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** One template, one complete input stack per transaction, and a reserved output slot. */
public final class ErosionFactoryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 1_000_000_000, COST = 1_000_000;
    public final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(CAPACITY, CAPACITY, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);
    public final ItemStackHandler items = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 ? stack.is(Tags.Items.ORES) : slot == 1 && stone(stack); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() { return 3; }
        public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot == 1 ? items.insertItem(slot, stack, simulate) : stack; }
        public ItemStack extractItem(int slot, int count, boolean simulate) { return slot == 2 ? items.extractItem(slot, count, simulate) : ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) { return slot == 1 && stone(stack); }
    };
    private final ContainerData data = new ContainerData() {
        public int get(int index) { return energy.getImaginaryEnergyStored() >>> (index * 16) & 65535; }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };
    public ErosionFactoryBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.EROSION_FACTORY.get(), pos, state); }
    private static boolean stone(ItemStack stack) { return stack.is(Items.STONE) || stack.is(Items.COBBLESTONE) || stack.is(Items.DEEPSLATE) || stack.is(Items.COBBLED_DEEPSLATE); }
    public static boolean matches(ItemStack template, ItemStack material) {
        if (!template.is(Tags.Items.ORES) || template.isEmpty() || material.isEmpty()) return false;
        // Ore host tags also support other mods; Nether ores cannot be made from Overworld stone.
        if (template.is(ItemTags.create(ResourceLocation.parse("c:ores_in_ground/deepslate"))))
            return material.is(Items.DEEPSLATE) || material.is(Items.COBBLED_DEEPSLATE);
        if (template.is(ItemTags.create(ResourceLocation.parse("c:ores_in_ground/stone"))))
            return material.is(Items.STONE) || material.is(Items.COBBLESTONE);
        return false;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, ErosionFactoryBlockEntity factory) {
        var template = factory.items.getStackInSlot(0);
        var input = factory.items.getStackInSlot(1);
        if (factory.energy.getImaginaryEnergyStored() < COST || !matches(template, input)) return;
        var result = template.copyWithCount(input.getCount());
        var output = factory.items.getStackInSlot(2);
        if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > Math.min(64, result.getMaxStackSize())) return;
        factory.items.setStackInSlot(2, result.copyWithCount(output.getCount() + result.getCount()));
        factory.items.setStackInSlot(1, ItemStack.EMPTY);
        factory.energy.setImaginaryEnergy(factory.energy.getImaginaryEnergyStored() - COST);
        factory.pushOutput();
    }
    /** Exactly one ordered pass after a successful conversion, across the external surface. */
    private void pushOutput() {
        var front = getBlockState().getValue(ErosionFactoryBlock.FACING);
        for (var side : new Direction[]{front.getOpposite(), Direction.UP, Direction.DOWN, front.getCounterClockWise(), front.getClockWise(), front}) {
            for (int part = 0; part < 27; part++) {
                var cell = ErosionFactoryBlock.partPos(worldPosition, part);
                var targetPos = cell.relative(side);
                var offset = targetPos.subtract(worldPosition);
                if (Math.abs(offset.getX()) <= 1 && Math.abs(offset.getZ()) <= 1 && offset.getY() >= 0 && offset.getY() <= 2) continue;
                if (!level.hasChunkAt(targetPos)) continue;
                var target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, side.getOpposite());
                if (target == null || target == automation) continue;
                var remaining = ItemHandlerHelper.insertItemStacked(target, items.getStackInSlot(2).copy(), false);
                items.setStackInSlot(2, remaining);
                if (remaining.isEmpty()) return;
            }
        }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.erosion_factory"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ErosionFactoryMenu(id, inv, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Energy", energy.getImaginaryEnergyStored()); tag.putInt("FeRemainder", fe.getRemainder());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items.deserializeNBT(registries, tag.getCompound("Items"));
        energy.setImaginaryEnergy(tag.getInt("Energy")); fe.setRemainder(tag.getInt("FeRemainder"));
    }
}
