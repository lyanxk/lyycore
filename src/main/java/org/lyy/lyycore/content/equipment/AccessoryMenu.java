package org.lyy.lyycore.content.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public final class AccessoryMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public AccessoryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
    public AccessoryMenu(int id, Inventory inventory, BlockPos pos) {
        super(LyyMenus.ACCESSORIES.get(), id);
        access = ContainerLevelAccess.create(inventory.player.level(), pos);
        ItemStackHandler items = inventory.player instanceof ServerPlayer server ? AccessoryEquipment.get(server) : new ItemStackHandler(2);
        for (int i = 0; i < 2; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(items, i, 88 + i * 56, 54) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof AccessoryItem item && item.kind.ordinal() == slot; }
                @Override public void setChanged() { super.setChanged(); if (items instanceof AccessoryEquipment equipment) equipment.changed(); }
            });
        }
        int x = 44, y = 128;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, x + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, x + col * 18, y + 58));
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && stillValid(access, player, LyyBlocks.MIRROR.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem(); var copy = stack.copy();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (index < 2) { if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY; }
        else if (stack.getItem() instanceof AccessoryItem item) {
            if (!moveItemStackTo(stack, item.kind.ordinal(), item.kind.ordinal() + 1, false)) return ItemStack.EMPTY;
        } else if (!(index < 29 ? moveItemStackTo(stack, 29, 38, false) : moveItemStackTo(stack, 2, 29, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }
}
