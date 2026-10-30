package org.lyy.lyycore.content.finality;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import org.lyy.lyycore.content.menu.BlockEntityItemSlot;
import org.lyy.lyycore.content.menu.MachineMenu;

public final class GenesisMenu extends MachineMenu {
    public final GenesisBlockEntity machine;
    private final SimpleContainer display = new SimpleContainer(2);
    private final ContainerData data;

    public GenesisMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(
                id,
                inventory,
                (GenesisBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public GenesisMenu(int id, Inventory inventory, GenesisBlockEntity machine) {
        super(FinalityContent.GENESIS_MENU.get(), id);
        this.machine = machine;
        addSlot(new BlockEntityItemSlot(machine, machine.template, 0, 26, 38));
        for (int i = 0; i < 2; i++)
            addSlot(
                    new Slot(display, i, 80 + i * 54, 38) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }
                    });
        addPlayerSlots(inventory, 8, 120);
        data =
                inventory.player.level().isClientSide
                        ? new SimpleContainerData(2)
                        : new ContainerData() {
                            public int get(int i) {
                                return machine.energy.getImaginaryEnergyStored() >>> (i * 16)
                                        & 65535;
                            }

                            public void set(int i, int value) {}

                            public int getCount() {
                                return 2;
                            }
                        };
        addDataSlots(data);
        machine.startOpen(inventory.player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        machine.stopOpen(player);
    }

    public int energy() {
        return (data.get(0) & 65535) | (data.get(1) & 65535) << 16;
    }

    @Override
    public void broadcastChanges() {
        if (!machine.getLevel().isClientSide) {
            display.setItem(0, machine.selected().copyWithCount(1));
            display.setItem(1, machine.ghost().copyWithCount(1));
        }
        super.broadcastChanges();
    }

    public void ghost(ItemStack stack, Player player) {
        if (!player.level().isClientSide && stillValid(player) && !player.isSpectator()) {
            machine.ghost(stack);
            broadcastChanges();
        }
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot == 1 || slot == 2) {
            if (!player.level().isClientSide && !player.isSpectator() && stillValid(player)) {
                if (slot == 1 && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE))
                    machine.take(player, type == ClickType.QUICK_MOVE);
                else if (slot == 2 && type == ClickType.PICKUP && getCarried().isEmpty())
                    machine.ghost(ItemStack.EMPTY);
                broadcastChanges();
            }
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive()
                && !machine.isRemoved()
                && player.level() == machine.getLevel()
                && player.distanceToSqr(machine.getBlockPos().getCenter()) < 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == 0) return moveStack(player, index, 3, slots.size(), true);
        if (index > 2) return moveStack(player, index, 0, 1, false);
        return ItemStack.EMPTY;
    }
}
