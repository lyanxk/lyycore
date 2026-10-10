package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.*;
import org.lyy.lyycore.content.blocks.FissionFurnaceBlock;
import org.lyy.lyycore.content.menu.FissionFurnaceMenu;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.*;

public final class FissionFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 1_000_000_000, COST = 1000;
    private float experience;
    private int activeUntil;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes = RecipeManager.createCheck(RecipeType.SMELTING);
    public final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(CAPACITY, CAPACITY, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);
    public final ItemStackHandler items = new ItemStackHandler(3) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() { return 3; }
        public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot == 0 ? items.insertItem(slot, stack, simulate) : stack; }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot == 0 ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate); }
        public int getSlotLimit(int slot) { return 64; }
        public boolean isItemValid(int slot, ItemStack stack) { return slot == 0; }
    };
    private final ContainerData data = new ContainerData() {
        public int get(int index) { return index == 0 ? energy.getImaginaryEnergyStored() & 65535 : energy.getImaginaryEnergyStored() >>> 16; }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };
    public FissionFurnaceBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.FISSION_FURNACE.get(), pos, state); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FissionFurnaceBlockEntity furnace) {
        boolean worked = false;
        // Each transaction reserves both product and container remainder before consuming anything.
        while (!furnace.items.getStackInSlot(0).isEmpty() && furnace.energy.getImaginaryEnergyStored() >= COST) {
            var input = new SingleRecipeInput(furnace.items.getStackInSlot(0));
            var recipe = furnace.recipes.getRecipeFor(input, level).orElse(null);
            if (recipe == null) break;
            var result = recipe.value().assemble(input, level.registryAccess());
            if (result.isEmpty()) break;
            var outputs = new ItemStack[]{furnace.items.getStackInSlot(1).copy(), furnace.items.getStackInSlot(2).copy()};
            if (!insert(outputs, result.copyWithCount(result.getCount() * 2))) break;
            var remainder = recipe.value().getRemainingItems(input).getFirst();
            boolean last = input.item().getCount() == 1;
            if (!last && !remainder.isEmpty() && !insert(outputs, remainder)) break;
            furnace.items.extractItem(0, 1, false);
            if (last && !remainder.isEmpty()) furnace.items.setStackInSlot(0, remainder);
            furnace.items.setStackInSlot(1, outputs[0]); furnace.items.setStackInSlot(2, outputs[1]);
            furnace.energy.setImaginaryEnergy(furnace.energy.getImaginaryEnergyStored() - COST);
            furnace.experience += recipe.value().getExperience();
            worked = true;
        }
        if (worked) furnace.activeUntil = (int)level.getGameTime() + 10;
        boolean active = (int)level.getGameTime() < furnace.activeUntil;
        if (active != state.getValue(FissionFurnaceBlock.ACTIVE)) level.setBlock(pos, state.setValue(FissionFurnaceBlock.ACTIVE, active), 3);
    }
    private static boolean insert(ItemStack[] outputs, ItemStack offered) {
        int remaining = offered.getCount();
        for (int i = 0; i < outputs.length; i++) {
            var current = outputs[i];
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, offered)) continue;
            int accepted = Math.min(remaining, Math.min(64, offered.getMaxStackSize()) - current.getCount());
            if (accepted <= 0) continue;
            outputs[i] = offered.copyWithCount(current.getCount() + accepted);
            remaining -= accepted;
            if (remaining == 0) return true;
        }
        return false;
    }
    public void awardExperience() {
        if (!(level instanceof ServerLevel server) || experience <= 0) return;
        int amount = (int)experience + (server.random.nextFloat() < experience % 1 ? 1 : 0);
        ExperienceOrb.award(server, worldPosition.getCenter(), amount); experience = 0; setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.fission_furnace"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new FissionFurnaceMenu(id, inv, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r); tag.put("Items", items.serializeNBT(r)); tag.putInt("Energy", energy.getImaginaryEnergyStored());
        tag.putFloat("Experience", experience); tag.putInt("FeRemainder", fe.getRemainder());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r); items.deserializeNBT(r, tag.getCompound("Items")); energy.setImaginaryEnergy(tag.getInt("Energy"));
        experience = Math.max(0, tag.getFloat("Experience")); fe.setRemainder(tag.getInt("FeRemainder"));
    }
}
