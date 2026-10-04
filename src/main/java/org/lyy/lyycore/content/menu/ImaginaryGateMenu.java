package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;

public class ImaginaryGateMenu extends AbstractContainerMenu {
    private final ImaginaryGateBlockEntity gate;
    private final ContainerData data;
    public ImaginaryGateMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (ImaginaryGateBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(1));
    }
    public ImaginaryGateMenu(int id, Inventory inventory, ImaginaryGateBlockEntity gate, ContainerData data) {
        this(LyyMenus.IMAGINARY_GATE.get(), id, inventory, gate, data);
    }
    protected ImaginaryGateMenu(MenuType<?> type, int id, Inventory inventory, ImaginaryGateBlockEntity gate, ContainerData data) {
        super(type, id);
        this.gate = gate;
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(gate.items(), 0, 80, 39) {
            @Override public void set(ItemStack stack) {
                gate.setOffering(inventory.player, stack);
                setChanged();
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 162));
    }
    public int status() { return data.get(0); }
    @Override public boolean stillValid(Player player) {
        return gate.getLevel().getBlockEntity(gate.getBlockPos()) == gate
                && player.distanceToSqr(gate.getBlockPos().getCenter().add(0, 2, 0)) <= 100;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (stack.is(LyyBlocks.CRYSTAL_BLOCK.get().asItem())) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
