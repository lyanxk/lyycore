package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.CelestialTime;
import org.lyy.lyycore.content.item.CelestialOrderItem;
import org.lyy.lyycore.content.item.WeatherBallItem;
import org.lyy.lyycore.registry.LyyMenus;

public final class CelestialOrderMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack tool;
    private final DataSlot iterations = DataSlot.standalone(), shifting = DataSlot.standalone();

    public CelestialOrderMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readEnum(InteractionHand.class)); }
    public CelestialOrderMenu(int id, Inventory inventory, InteractionHand hand) {
        super(LyyMenus.CELESTIAL_ORDER.get(), id);
        this.owner = inventory.player; this.hand = hand; tool = owner.getItemInHand(hand);
        iterations.set(CelestialOrderItem.iterations(tool));
        addDataSlot(iterations); addDataSlot(shifting);
    }
    public ItemStack tool() { return tool; }
    public int iterations() { return iterations.get(); }
    public boolean shifting() { return shifting.get() != 0; }

    @Override public void broadcastChanges() {
        if (owner instanceof ServerPlayer player) shifting.set(CelestialTime.active(player.server) ? 1 : 0);
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer server) || !stillValid(player)) return false;
        if (button >= 0 && button < 4) {
            if (!CelestialTime.shift(server.server, new int[]{-12, -6, 6, 12}[button])) return false;
        } else if (button >= 10 && button <= 12) {
            int count = new int[]{1, 10, 100}[button - 10];
            CelestialOrderItem.iterations(tool, count); iterations.set(count);
            player.getInventory().setChanged();
        } else if (button >= 20 && button <= 22) {
            boolean rain = button != 20, thunder = button == 22;
            server.server.overworld().setWeatherParameters(rain ? 0 : WeatherBallItem.WEATHER_DURATION,
                    rain ? WeatherBallItem.WEATHER_DURATION : 0, rain, thunder);
        } else return false;
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && player.getItemInHand(hand) == tool && tool.getItem() instanceof CelestialOrderItem;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
