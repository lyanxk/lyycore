package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.ProductionLabBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyMenus;

public final class ProductionLabMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public ProductionLabMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (ProductionLabBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(2));
    }
    public ProductionLabMenu(int id, Inventory inventory, ProductionLabBlockEntity lab, ContainerData data) {
        super(LyyMenus.PRODUCTION_LAB.get(), id);
        this.access = ContainerLevelAccess.create(lab.getLevel(), lab.getBlockPos());
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(lab.items(), 0, 80, 45) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(LyyItems.RESEARCH_NOTES.get()) && !hasItem(); }
            @Override public int getMaxStackSize(ItemStack stack) { return stack.is(LyyItems.RESEARCH_NOTES.get()) ? 1 : stack.getMaxStackSize(); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 112 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 170));
    }
    public float progress() { return data.get(0) / 1000F; }
    public int status() { return data.get(1); }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, LyyBlocks.PRODUCTION_LAB.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
