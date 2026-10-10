package org.lyy.lyycore.content.menu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.FissionFurnaceBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;
public final class FissionFurnaceMenu extends AbstractContainerMenu {
    private final FissionFurnaceBlockEntity furnace;
    private final ContainerData data;
    public FissionFurnaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (FissionFurnaceBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
    }
    public FissionFurnaceMenu(int id, Inventory inv, FissionFurnaceBlockEntity furnace, ContainerData data) {
        super(LyyMenus.FISSION_FURNACE.get(), id); this.furnace = furnace; this.data = data; addDataSlots(data);
        addSlot(new SlotItemHandler(furnace.items, 0, 35, 40));
        for (int i = 1; i <= 2; i++) addSlot(new SlotItemHandler(furnace.items, i, 88 + i * 22, 40) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); furnace.awardExperience(); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inv, 9+row*9+col, 8+col*18, 96+row*18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8+col*18, 154));
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    @Override public boolean stillValid(Player p) { return !furnace.isRemoved() && p.level() == furnace.getLevel() && p.distanceToSqr(furnace.getBlockPos().getCenter()) < 64; }
    @Override public ItemStack quickMoveStack(Player p, int index) {
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        if (!(index < 3 ? moveItemStackTo(stack, 3, slots.size(), true) : moveItemStackTo(stack, 0, 1, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(p, copy.copyWithCount(copy.getCount()-stack.getCount())); return copy;
    }
}
