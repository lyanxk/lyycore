package org.lyy.lyycore.content.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Machine slots precede the player's 27 inventory slots and nine hotbar slots. */
public abstract class MachineMenu extends AbstractContainerMenu {
    protected MachineMenu(MenuType<?> type, int id) { super(type, id); }

    protected void addPlayerSlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, x + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, x + col * 18, y + 58));
    }

    protected ItemStack moveStack(Player player, int index, int start, int end, boolean reverse) {
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var original = slot.getItem().copy();
        var remaining = original.copy();
        if (!moveItemStackTo(remaining, start, end, reverse)) return ItemStack.EMPTY;
        // Bulk-output handlers return a snapshot; commit the remainder through the slot.
        slot.set(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        slot.onTake(player, original.copyWithCount(original.getCount() - remaining.getCount()));
        return original;
    }

    protected ItemStack movePlayerStack(Player player, int index) {
        int hotbar = slots.size() - 9;
        return index < hotbar ? moveStack(player, index, hotbar, slots.size(), false)
                : moveStack(player, index, hotbar - 27, hotbar, false);
    }
}
