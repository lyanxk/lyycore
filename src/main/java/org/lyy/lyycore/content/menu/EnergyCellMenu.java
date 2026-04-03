package org.lyy.lyycore.content.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public class EnergyCellMenu extends AbstractContainerMenu {
    public final EnergyCellBlockEntity be;
    private final ContainerLevelAccess access;

    private static final int MACHINE_SLOTS = 1;
    private static final int INV_START = MACHINE_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    // Server constructor
    public EnergyCellMenu(int id, Inventory inv, EnergyCellBlockEntity be) {
        super(LyyMenus.IMAGINARY_ENERGY_CELL.get(), id);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        addSlot(new SlotItemHandler(be.getItemHandler(), 0, 80, 34));
        addPlayerSlots(inv);
    }

    // Client constructor (from network)
    public EnergyCellMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(LyyMenus.IMAGINARY_ENERGY_CELL.get(), id);
        BlockPos pos = buf.readBlockPos();
        Level level = inv.player.level();
        this.be = level.getBlockEntity(pos) instanceof EnergyCellBlockEntity c ? c : null;
        this.access = ContainerLevelAccess.create(level, pos);
        if (be != null) addSlot(new SlotItemHandler(be.getItemHandler(), 0, 80, 34));
        addPlayerSlots(inv);
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }

    public int getEnergy()   { return be != null ? be.getIEnergyStored() : 0; }
    public int getCapacity() { return be != null ? be.getMaxIEnergyStored() : 0; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, LyyBlocks.IMAGINARY_ENERGY_CELL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (index < INV_END) {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false))
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false))
                if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
