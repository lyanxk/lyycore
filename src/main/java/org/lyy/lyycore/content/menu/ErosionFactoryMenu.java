package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class ErosionFactoryMenu extends AbstractContainerMenu {
    private final ErosionFactoryBlockEntity factory;
    private final ContainerData data;
    public ErosionFactoryMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (ErosionFactoryBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
    }
    public ErosionFactoryMenu(int id, Inventory inv, ErosionFactoryBlockEntity factory, ContainerData data) {
        super(LyyMenus.EROSION_FACTORY.get(), id); this.factory = factory; this.data = data; addDataSlots(data);
        addSlot(new BlockEntityItemSlot(factory, factory.items, 0, 24, 40));
        addSlot(new BlockEntityItemSlot(factory, factory.items, 1, 76, 40));
        addSlot(new BlockEntityItemSlot(factory, factory.items, 2, 136, 40) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inv, 9+row*9+col, 8+col*18, 96+row*18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8+col*18, 154));
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    @Override public boolean stillValid(Player p) { return p.isAlive() && !factory.isRemoved() && p.level() == factory.getLevel() && p.distanceToSqr(factory.getBlockPos().getCenter()) < 64; }
    @Override public ItemStack quickMoveStack(Player p, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int destination = factory.items.isItemValid(0, stack) ? 0 : 1;
            if (!moveItemStackTo(stack, destination, destination + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(p, original.copyWithCount(original.getCount()-stack.getCount()));
        return original;
    }
}
