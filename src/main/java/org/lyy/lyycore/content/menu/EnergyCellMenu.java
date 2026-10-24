package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public class EnergyCellMenu extends MachineMenu {
    public static final int WIDTH = 176, HEIGHT = 186;
    public static final int CHARGE_X = 16, CHARGE_Y = 42;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 104, HOTBAR_Y = 162;
    public final EnergyCellBlockEntity be;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Server constructor
    public EnergyCellMenu(int id, Inventory inv, EnergyCellBlockEntity be, ContainerData data) {
        super(LyyMenus.IMAGINARY_ENERGY_CELL.get(), id);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(be.getItemHandler(), 0, CHARGE_X, CHARGE_Y));
        addPlayerSlots(inv, INVENTORY_X, INVENTORY_Y);
    }

    // Client constructor (from network)
    public EnergyCellMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (EnergyCellBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(4));
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

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index == 0) return moveStack(player, index, 1, slots.size(), true);
        var moved = moveStack(player, index, 0, 1, false);
        return moved.isEmpty() ? movePlayerStack(player, index) : moved;
    }
}
