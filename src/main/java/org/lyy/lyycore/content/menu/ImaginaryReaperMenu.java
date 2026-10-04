package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.item.ImaginaryReaperItem;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyMenus;

import java.util.function.IntSupplier;

public final class ImaginaryReaperMenu extends AbstractContainerMenu {
    public static final int EFFICIENCY_BUTTON = 0, DAMAGE_BUTTON = 256, TOGGLE_ENCHANTMENT = 512;
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack tool;
    private final DataSlot efficiency, damage, silkTouch;

    public ImaginaryReaperMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public ImaginaryReaperMenu(int id, Inventory inventory, InteractionHand hand) {
        super(LyyMenus.IMAGINARY_REAPER.get(), id);
        this.owner = inventory.player;
        this.hand = hand;
        this.tool = owner.getItemInHand(hand);
        efficiency = syncedValue(() -> ImaginaryReaperItem.efficiency(tool));
        damage = syncedValue(() -> ImaginaryReaperItem.damage(tool));
        silkTouch = syncedValue(() -> ImaginaryReaperItem.silkTouch(tool) ? 1 : 0);
    }

    private DataSlot syncedValue(IntSupplier value) {
        return addDataSlot(new DataSlot() {
            private int synced = value.getAsInt();
            @Override public int get() { return owner.level().isClientSide ? synced : value.getAsInt(); }
            @Override public void set(int value) { synced = value; }
        });
    }

    public int efficiency() { return efficiency.get(); }
    public int damage() { return damage.get(); }
    public boolean silkTouch() { return silkTouch.get() != 0; }
    public ItemStack tool() { return tool; }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (button >= EFFICIENCY_BUTTON + ImaginaryReaperItem.MIN_EFFICIENCY
                && button <= EFFICIENCY_BUTTON + ImaginaryReaperItem.MAX_EFFICIENCY) {
            ImaginaryReaperItem.setEfficiency(tool, button - EFFICIENCY_BUTTON);
        } else if (button >= DAMAGE_BUTTON + ImaginaryReaperItem.MIN_DAMAGE
                && button <= DAMAGE_BUTTON + ImaginaryReaperItem.MAX_DAMAGE) {
            ImaginaryReaperItem.setAttackDamage(tool, button - DAMAGE_BUTTON);
        } else if (button == TOGGLE_ENCHANTMENT) {
            ImaginaryReaperItem.setSilkTouch(tool, !silkTouch());
        } else return false;
        player.getInventory().setChanged();
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && player.getItemInHand(hand) == tool && tool.is(LyyItems.IMAGINARY_REAPER.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
