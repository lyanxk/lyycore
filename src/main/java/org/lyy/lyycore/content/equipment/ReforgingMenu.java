package org.lyy.lyycore.content.equipment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.ReforgingTableBlockEntity;
import org.lyy.lyycore.content.menu.BlockEntityItemSlot;
import org.lyy.lyycore.registry.LyyMenus;

public final class ReforgingMenu extends AbstractContainerMenu {
    private final ReforgingTableBlockEntity table;
    private final DataSlot locks = DataSlot.standalone();
    private ItemStack observed = ItemStack.EMPTY;
    public ReforgingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (ReforgingTableBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }
    public ReforgingMenu(int id, Inventory inventory, ReforgingTableBlockEntity table) {
        super(LyyMenus.REFORGING.get(), id); this.table = table; addDataSlot(locks);
        addSlot(new BlockEntityItemSlot(table, table.items, 0, 42, 42));
        addSlot(new BlockEntityItemSlot(table, table.items, 1, 42, 82));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 44 + col * 18, 160 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 44 + col * 18, 218));
    }
    public ItemStack accessory() { return getSlot(0).getItem(); }
    public int locks() { return locks.get(); }
    public int cost() { return 1 << Integer.bitCount(locks()); }
    public boolean canReforge() {
        var data = AccessoryData.read(accessory());
        return accessory().getItem() instanceof AccessoryItem item && !data.traits().isEmpty()
                && Integer.bitCount(locks()) < data.traits().size()
                && getSlot(1).getItem().is(item.series.plate.get()) && getSlot(1).getItem().getCount() >= cost();
    }
    private void refreshLocks() {
        if (!ItemStack.matches(observed, accessory())) { locks.set(0); observed = accessory().copy(); }
    }
    @Override public void broadcastChanges() { refreshLocks(); super.broadcastChanges(); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || player.isSpectator() || !stillValid(player)) return false;
        refreshLocks();
        int count = AccessoryData.read(accessory()).traits().size();
        if (id >= 0 && id < count) locks.set(locks() ^ 1 << id);
        else if (id == -1 && canReforge()) {
            var result = accessory().copy();
            AccessoryData.reforge(result, locks(), server.getRandom());
            table.items.extractItem(1, cost(), false);
            table.items.setStackInSlot(0, result);
            // Reforging places retained traits first, so move their locks with them.
            locks.set((1 << Integer.bitCount(locks())) - 1);
            observed = result.copy();
            server.level().playSound(null, table.getBlockPos(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, .6F, 1.2F);
        } else return false;
        broadcastChanges(); return true;
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !table.isRemoved() && player.level() == table.getLevel() && player.distanceToSqr(table.getBlockPos().getCenter()) < 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem(); var copy = stack.copy();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (index < 2) { if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY; }
        else if (table.items.isItemValid(0, stack)) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
        else if (table.items.isItemValid(1, stack)) { if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY; }
        else if (!(index < 29 ? moveItemStackTo(stack, 29, 38, false) : moveItemStackTo(stack, 2, 29, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return copy;
    }
}
