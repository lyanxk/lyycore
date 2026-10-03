package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.ImaginaryCraftingTableBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public final class ImaginaryCraftingMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 240;
    public static final int INVENTORY_LABEL_Y = 147, INVENTORY_Y = 159, HOTBAR_Y = 217;
    public static final int[][] POSITIONS = {{80, 62}, {80, 18}, {111, 31}, {124, 62}, {111, 93},
            {80, 106}, {49, 93}, {36, 62}, {49, 31}};
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ImaginaryCraftingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (ImaginaryCraftingTableBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(1));
    }
    public ImaginaryCraftingMenu(int id, Inventory inventory, ImaginaryCraftingTableBlockEntity table, ContainerData data) {
        super(LyyMenus.IMAGINARY_CRAFTING.get(), id);
        this.access = ContainerLevelAccess.create(table.getLevel(), table.getBlockPos());
        this.data = data;
        addDataSlots(data);
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new SlotItemHandler(table.items(), slot, POSITIONS[slot][0], POSITIONS[slot][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return progress() == 0; }
                @Override public boolean mayPickup(Player player) { return progress() == 0; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
    }
    public int progress() { return data.get(0); }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, LyyBlocks.IMAGINARY_CRAFTING_TABLE.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
