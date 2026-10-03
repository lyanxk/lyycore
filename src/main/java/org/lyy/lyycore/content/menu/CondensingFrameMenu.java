package org.lyy.lyycore.content.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.FrameProduction;
import org.lyy.lyycore.content.blocks.CondensingFrameBlock;
import org.lyy.lyycore.content.blockEntities.CondensingFrameBlockEntity;

public abstract class CondensingFrameMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 186;
    public static final int OUTPUT_X = 80, OUTPUT_Y = 39;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final net.minecraft.world.level.block.Block validBlock;

    protected CondensingFrameMenu(MenuType<?> type, int id, Inventory inventory, CondensingFrameBlockEntity be,
                                   IItemHandlerModifiable output, ContainerData data, int dataCount) {
        super(type, id);
        checkContainerDataCount(data, dataCount);
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.validBlock = be.getBlockState().getBlock();
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(output, 0, OUTPUT_X, OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public int getMaxStackSize() { return 64; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 162));
    }
    public FrameProduction production() {
        return ((CondensingFrameBlock) validBlock).production();
    }
    public boolean isMiniatureFactory() { return production() == FrameProduction.MINIATURE_FACTORY; }
    public int getEnergyIE() { return data.get(0) & 0xFFFF | (data.get(1) & 0xFFFF) << 16; }
    public int getOutputCount() { return data.get(2); }
    public boolean isWaterlogged() { return data.get(3) != 0; }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, validBlock); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy();
        ItemStack moving = original.copy();
        if (index == 0) {
            if (!moveItemStackTo(moving, 1, 37, true)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(moving, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 1, 28, false)) return ItemStack.EMPTY;
        slot.setByPlayer(moving);
        slot.onTake(player, original.copyWithCount(original.getCount() - moving.getCount()));
        return original;
    }
}
