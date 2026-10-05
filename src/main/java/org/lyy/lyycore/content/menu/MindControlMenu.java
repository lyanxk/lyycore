package org.lyy.lyycore.content.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.MindControlBeaconBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class MindControlMenu extends AbstractContainerMenu {
    private final MindControlBeaconBlockEntity beacon;
    private final ContainerData data;
    public MindControlMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (MindControlBeaconBlockEntity)inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(2));
    }
    public MindControlMenu(int id, Inventory inventory, MindControlBeaconBlockEntity beacon, ContainerData data) {
        super(LyyMenus.MIND_CONTROL.get(), id);
        this.beacon = beacon; this.data = data; addDataSlots(data);
    }
    public int controlledCount() { return data.get(0); }
    public int mode() { return data.get(1); }
    @Override public boolean stillValid(Player player) {
        return beacon != null && !beacon.isRemoved() && player.distanceToSqr(beacon.getBlockPos().getCenter()) < 100
                && (player.level().isClientSide || beacon.canUse(player));
    }
    @Override public boolean clickMenuButton(Player player, int id) { return stillValid(player) && beacon.command(player, id); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
