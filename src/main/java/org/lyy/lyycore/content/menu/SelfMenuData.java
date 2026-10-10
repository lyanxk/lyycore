package org.lyy.lyycore.content.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import org.lyy.lyycore.content.*;
import org.lyy.lyycore.content.wings.WingsSettings;
import org.lyy.lyycore.network.WingsNetwork;

/** The memory menu owns this small synchronized state only; attribute values sync via vanilla attributes. */
public final class SelfMenuData implements ContainerData {
    public static final int FLIGHT = -10000, PURSUIT = -10010, ALLOCATE = -10020, REFUND = -10030, ACCELERATION = -10200;
    private final Player player;
    private final int[] client = new int[12];
    public SelfMenuData(Player player) { this.player = player; }
    private int value(int index) {
        if (index == 0) return PlayerAttributes.unlocked(player) ? 1 : 0;
        if (index == 1) return PlayerAttributes.remaining(player);
        if (index >= 2 && index <= 5) return PlayerAttributes.allocated(player, PlayerAttributes.Stat.values()[index - 2]);
        return switch (index) {
            case 6 -> WingsSettings.pursuit(player);
            case 7 -> WingsSettings.maxPursuit(player);
            case 8 -> WingsSettings.flight(player) ? 1 : 0;
            case 9 -> WingsSettings.acceleration(player);
            case 10 -> AegisWings.level(player);
            default -> 0;
        };
    }
    public int valueAt(int index) { return player.level().isClientSide ? client[index] : value(index); }
    public boolean unlocked() { return valueAt(0) != 0; }
    @Override public int get(int index) { return valueAt(index / 2) >>> (index % 2 * 16) & 65535; }
    @Override public void set(int index, int value) {
        int shift = index % 2 * 16;
        client[index / 2] = (client[index / 2] & ~(65535 << shift)) | ((value & 65535) << shift);
    }
    @Override public int getCount() { return client.length * 2; }
    public boolean click(ServerPlayer player, int id) {
        if (!PlayerAttributes.unlocked(player)) return false;
        if (id <= ALLOCATE && id > ALLOCATE - 4) return PlayerAttributes.allocate(player, ALLOCATE - id, true);
        if (id <= REFUND && id > REFUND - 4) return PlayerAttributes.allocate(player, REFUND - id, false);
        if (!AegisWings.unlocked(player)) return false;
        int pursuit = WingsSettings.pursuit(player), acceleration = WingsSettings.acceleration(player);
        boolean flight = WingsSettings.flight(player);
        if (id == FLIGHT) flight = !flight;
        else if (id <= PURSUIT && id >= PURSUIT - WingsSettings.maxPursuit(player)) pursuit = PURSUIT - id;
        else if (id <= ACCELERATION && id >= ACCELERATION - 100 && AegisWings.level(player) >= 2) acceleration = ACCELERATION - id;
        else return false;
        WingsSettings.set(player, pursuit, flight, acceleration); WingsNetwork.sync(player); return true;
    }
}
