package org.lyy.lyycore.content.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public class EnergyCellMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 186;
    public static final int CHARGE_X = 16, CHARGE_Y = 42;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 104, HOTBAR_Y = 162;
    public final EnergyCellBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    private static final int MACHINE_SLOTS = 1;
    private static final int INV_START = MACHINE_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    // Server constructor
    public EnergyCellMenu(int id, Inventory inv, EnergyCellBlockEntity be, ContainerData data) {
        super(LyyMenus.IMAGINARY_ENERGY_CELL.get(), id);
        checkContainerDataCount(data, 4);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(be.getItemHandler(), 0, CHARGE_X, CHARGE_Y));
        addPlayerSlots(inv);
    }

    // Client constructor (from network)
    public EnergyCellMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(4));
    }

    private static EnergyCellBlockEntity getBlockEntity(Inventory inv, BlockPos pos) {
        Level level = inv.player.level();
        if (level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) return cell;
        throw new IllegalStateException("BlockEntity at " + pos + " is not EnergyCellBlockEntity");
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inv, i, INVENTORY_X + i * 18, HOTBAR_Y));
    }

    public int getEnergy()   { return combineWords(data.get(0), data.get(1)); }
    public int getCapacity() { return combineWords(data.get(2), data.get(3)); }

    private static int combineWords(int low, int high) {
        return low & 0xFFFF | (high & 0xFFFF) << 16;
    }

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
