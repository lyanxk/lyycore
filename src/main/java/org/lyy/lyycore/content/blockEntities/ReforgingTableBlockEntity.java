package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.equipment.*;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class ReforgingTableBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof AccessoryItem;
            for (var series : AccessoryItem.Series.values()) if (stack.is(series.plate.get())) return true;
            return false;
        }
        @Override protected void onContentsChanged(int slot) {
            if (slot == 0 && level != null && !level.isClientSide) AccessoryData.initialize(getStackInSlot(0), level.random);
            setChanged();
        }
    };
    public ReforgingTableBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.REFORGING_TABLE.get(), pos, state); }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ReforgingMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.put("Items", items.serializeNBT(registries)); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); items.deserializeNBT(registries, tag.getCompound("Items")); }
}
