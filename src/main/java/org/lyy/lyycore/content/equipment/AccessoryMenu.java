package org.lyy.lyycore.content.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.menu.MachineMenu;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public final class AccessoryMenu extends MachineMenu {
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
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof Accessory item && item.kind().ordinal() == slot; }
                @Override public void setChanged() { super.setChanged(); if (items instanceof AccessoryEquipment equipment) equipment.changed(); }
            });
        }
        addPlayerSlots(inventory, 44, 128);
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && stillValid(access, player, LyyBlocks.MIRROR.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 2) return moveStack(player, index, 2, slots.size(), true);
        return slots.get(index).getItem().getItem() instanceof Accessory item
                ? moveStack(player, index, item.kind().ordinal(), item.kind().ordinal() + 1, false) : movePlayerStack(player, index);
    }
}
