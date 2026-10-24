package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public class ImaginaryGateMenu extends MachineMenu {
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
        addPlayerSlots(inventory, 8, 104);
    }
    public int status() { return data.get(0); }
    @Override public boolean stillValid(Player player) {
        return gate.getLevel().getBlockEntity(gate.getBlockPos()) == gate
                && player.distanceToSqr(gate.getBlockPos().getCenter().add(0, 2, 0)) <= 100;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index == 0) return moveStack(player, index, 1, slots.size(), true);
        return gate.acceptsOffering(slots.get(index).getItem())
                ? moveStack(player, index, 0, 1, false) : movePlayerStack(player, index);
    }
}
