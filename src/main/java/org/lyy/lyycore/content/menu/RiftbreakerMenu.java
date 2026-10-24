package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.ToolMining;
import org.lyy.lyycore.content.item.RiftbreakerItem;
import org.lyy.lyycore.registry.LyyMenus;

import java.util.function.IntSupplier;

/** Uses vanilla inventory-button packets; no separate serverbound network message is needed. */
public final class RiftbreakerMenu extends AbstractContainerMenu {
    public static final int EFFICIENCY_BUTTON = 0, TOGGLE_ENCHANTMENT = 256, MODE_BUTTON = 300, RADIUS_BUTTON = 400;
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack tool;
    private final DataSlot efficiency, silkTouch, mode, radius;

    public RiftbreakerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class));
    }
    public RiftbreakerMenu(int id, Inventory inventory, InteractionHand hand) {
        super(LyyMenus.RIFTBREAKER.get(), id);
        owner = inventory.player;
        this.hand = hand;
        tool = owner.getItemInHand(hand);
        efficiency = synced(() -> RiftbreakerItem.efficiency(tool));
        silkTouch = synced(() -> RiftbreakerItem.silkTouch(tool) ? 1 : 0);
        mode = synced(() -> RiftbreakerItem.mode(tool).ordinal());
        radius = synced(() -> RiftbreakerItem.radius(tool));
    }
    private DataSlot synced(IntSupplier value) {
        return addDataSlot(new DataSlot() {
            private int synced = value.getAsInt();
            @Override public int get() { return owner.level().isClientSide ? synced : value.getAsInt(); }
            @Override public void set(int value) { synced = value; }
        });
    }
    public int efficiency() { return efficiency.get(); }
    public boolean silkTouch() { return silkTouch.get() != 0; }
    public ToolMining.Pattern mode() { return ToolMining.Pattern.values()[Math.clamp(mode.get(), 0, 2)]; }
    public int radius() { return radius.get(); }
    public ItemStack tool() { return tool; }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (button >= RiftbreakerItem.MIN_EFFICIENCY && button <= RiftbreakerItem.MAX_EFFICIENCY)
            RiftbreakerItem.setEfficiency(tool, button);
        else if (button == TOGGLE_ENCHANTMENT) RiftbreakerItem.setSilkTouch(tool, !silkTouch());
        else if (button >= MODE_BUTTON && button < MODE_BUTTON + 3)
            RiftbreakerItem.setMode(tool, ToolMining.Pattern.values()[button - MODE_BUTTON]);
        else if (button >= RADIUS_BUTTON + 1 && button <= RADIUS_BUTTON + 5)
            RiftbreakerItem.setRadius(tool, button - RADIUS_BUTTON);
        else return false;
        player.getInventory().setChanged();
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && player.getItemInHand(hand) == tool && tool.getItem() instanceof RiftbreakerItem;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
