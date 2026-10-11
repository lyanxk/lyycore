package org.lyy.lyycore.content.menu;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.OtherworldChestBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class OtherworldChestMenu extends AbstractContainerMenu {
    public static final int TAKE_ONE = -1, TAKE_STACK = -2;
    private final OtherworldChestBlockEntity chest;
    private final ContainerData data;
    public OtherworldChestMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (OtherworldChestBlockEntity)inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(3));
    }
    public OtherworldChestMenu(int id, Inventory inventory, OtherworldChestBlockEntity chest, ContainerData data) {
        super(LyyMenus.OTHERWORLD_CHEST.get(), id); this.chest = chest; this.data = data; addDataSlots(data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 106 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 164));
        chest.startOpen(inventory.player);
    }
    public boolean isFor(OtherworldChestBlockEntity target) { return chest == target; }
    @Override public void removed(Player player) { super.removed(player); chest.stopOpen(player); }
    public List<ItemStack> products() { return chest.products(); }
    public int selectedIndex() { return data.get(2); }
    public ItemStack selected() { int index = selectedIndex(); return index >= 0 && index < products().size() ? products().get(index) : ItemStack.EMPTY; }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || !stillValid(player) || player.isSpectator()) return false;
        boolean changed = id >= 0 ? chest.select(id) : (id == TAKE_ONE || id == TAKE_STACK) && chest.take(player, id == TAKE_STACK);
        if (changed) broadcastChanges(); return changed;
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !chest.isRemoved() && player.level() == chest.getLevel() && player.distanceToSqr(chest.getBlockPos().getCenter()) < 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        if (!(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); return copy;
    }
}
