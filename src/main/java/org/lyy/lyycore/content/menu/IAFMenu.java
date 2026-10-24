package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.lyy.lyycore.content.blockEntities.ImaginaryAlloyForgeBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public class IAFMenu extends MachineMenu {
    public static final int INPUT_A = 0, INPUT_B = 1, CATALYST = 2, OUTPUT = 3;
    public static final int WIDTH = 176, HEIGHT = 186;
    public static final int INPUT_X = 38, INPUT_A_Y = 28, INPUT_B_Y = 52;
    public static final int CATALYST_X = 68, CATALYST_Y = 40, OUTPUT_X = 132, OUTPUT_Y = 40;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 104, HOTBAR_Y = 162;

    private final ImaginaryAlloyForgeBlockEntity blockEntity;
    private final ContainerData data;

    // Server constructor
    public IAFMenu(int windowId, Inventory playerInv, ImaginaryAlloyForgeBlockEntity be, ContainerData data) {
        super(LyyMenus.IAF_MENU.get(), windowId);
        this.blockEntity = be;
        this.data = data;
        addDataSlots(data);

        IItemHandler handler = be.getItemHandler();
        addSlot(new MachineSlot(handler, INPUT_A, INPUT_X, INPUT_A_Y));
        addSlot(new MachineSlot(handler, INPUT_B, INPUT_X, INPUT_B_Y));
        addSlot(new MachineSlot(handler, CATALYST, CATALYST_X, CATALYST_Y));
        addSlot(new MachineSlot(handler, OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        addPlayerSlots(playerInv, INVENTORY_X, INVENTORY_Y);
    }

    // Client constructor (from network)
    public IAFMenu(int windowId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(windowId, playerInv, (ImaginaryAlloyForgeBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    public int getProgress()    { return combineWords(data.get(0), data.get(1)); }
    public int getMaxProgress() { return combineWords(data.get(2), data.get(3)); }
    public int getEnergy()      { return combineWords(data.get(4), data.get(5)); }
    public int getMaxEnergy()   { return combineWords(data.get(6), data.get(7)); }

    private static int combineWords(int low, int high) {
        return low & 0xFFFF | (high & 0xFFFF) << 16;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !blockEntity.isRemoved()
                && player.level() == blockEntity.getLevel()
                && player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0;
    }

    @Override public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 4) return moveStack(player, index, 4, slots.size(), true);
        var moved = moveStack(player, index, 0, 3, false);
        return moved.isEmpty() ? movePlayerStack(player, index) : moved;
    }

    private class MachineSlot extends SlotItemHandler {
        public MachineSlot(IItemHandler handler, int index, int x, int y) { super(handler, index, x, y); }

        @Override public void setChanged() {
            super.setChanged();
            // Menu transfers can mutate the existing stack without calling the handler.
            blockEntity.inventoryChanged();
        }
    }

}
