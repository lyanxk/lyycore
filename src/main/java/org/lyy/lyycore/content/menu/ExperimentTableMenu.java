package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.ExperimentTableBlockEntity;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import org.lyy.lyycore.content.research.ExperimentDefinition;
import org.lyy.lyycore.registry.*;

public final class ExperimentTableMenu extends AbstractContainerMenu {
    private final ExperimentTableBlockEntity table;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public ExperimentTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (ExperimentTableBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(3));
    }
    public ExperimentTableMenu(int id, Inventory inventory, ExperimentTableBlockEntity table, ContainerData data) {
        super(LyyMenus.EXPERIMENT_TABLE.get(), id);
        this.table = table; this.data = data;
        access = ContainerLevelAccess.create(table.getLevel(), table.getBlockPos());
        addDataSlots(data);
        addSlot(new BlockEntityItemSlot(table, table.items(), 0, 203, 56));
        addSlot(new BlockEntityItemSlot(table, table.items(), 1, 203, 95));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 45 + col * 18, 155 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 45 + col * 18, 213));
    }
    public int reserve(int element) { return data.get(element); }
    public ExperimentDefinition experiment() { return ResearchNotesItem.experiment(getSlot(1).getItem(), table.getLevel()); }
    public int[] positions(ExperimentDefinition experiment) { return ResearchNotesItem.experimentPositions(getSlot(1).getItem(), experiment); }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, LyyBlocks.EXPERIMENT_TABLE.get()); }
    @Override public boolean clickMenuButton(Player player, int action) { return stillValid(player) && table.act(action); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
