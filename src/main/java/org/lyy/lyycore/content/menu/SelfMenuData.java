package org.lyy.lyycore.content.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import org.lyy.lyycore.content.*;
import org.lyy.lyycore.content.PlayerAttributes.Stat;
import org.lyy.lyycore.content.wings.WingsSettings;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.content.skills.SkillSettings;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.network.SkillNetwork;
import org.lyy.lyycore.content.equipment.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** Menu state and attack damage sync here; the other displayed attributes sync through vanilla. */
public final class SelfMenuData implements ContainerData {
    public static final int FLIGHT = -10000, PURSUIT = -10010, ALLOCATE = -10020, REFUND = -10030, ACCELERATION = -10200;
    public static final int SKILLS = -10040, STYLE_HUD = -10041, EQUIPMENT = -10050;
    public static final int ALLOCATE_TEN = -10060, REFUND_TEN = -10070;
    private final Player player;
    private static final int ATTACK_VALUE = 19;
    private final int[] client = new int[20];
    private int selectedSlot;
    private CustomData observedData;
    private net.minecraft.world.item.Item observedItem;
    private AccessoryData accessoryData;
    private AccessoryData accessory() {
        var stack = AccessoryEquipment.get((ServerPlayer)player).getStackInSlot(selectedSlot);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (accessoryData == null || observedData != data || observedItem != stack.getItem()) {
            observedData = data;
            observedItem = stack.getItem();
            accessoryData = AccessoryData.read(stack);
        }
        return accessoryData;
    }
    public SelfMenuData(Player player) { this.player = player; }
    private int value(int index) {
        if (index == 0) return PlayerAttributes.unlocked(player) ? 1 : 0;
        if (index == 1) return accessory().remaining();
        if (index >= 2 && index <= 5) return accessory().allocated(PlayerAttributes.Stat.values()[index - 2]);
        if (index >= 11 && index < 16) return accessory().allocated(PlayerAttributes.Stat.values()[index - 11 + 4]);
        return switch (index) {
            case 6 -> WingsSettings.pursuit(player);
            case 7 -> WingsSettings.maxPursuit(player);
            case 8 -> WingsSettings.flight(player) ? 1 : 0;
            case 9 -> WingsSettings.acceleration(player);
            case 10 -> AegisWings.level(player);
            case 16 -> selectedSlot;
            case 17 -> accessory().traits().stream().mapToInt(stat -> 1 << stat.ordinal()).reduce(0, (a, b) -> a | b);
            case 18 -> accessory().total();
            case ATTACK_VALUE -> Float.floatToIntBits((float)player.getAttributeValue(Stat.ATTACK.attribute));
            default -> 0;
        };
    }
    public int valueAt(int index) { return player.level().isClientSide ? client[index] : value(index); }
    public boolean unlocked() { return valueAt(0) != 0; }
    public boolean hasTrait(int stat) { return (valueAt(17) & 1 << stat) != 0; }
    public int selectedSlot() { return valueAt(16); }
    public int allocated(int stat) { return valueAt(stat < 4 ? stat + 2 : 11 + stat - 4); }
    public double attributeValue(Stat stat) {
        // Vanilla attack damage is not syncable, so the screen needs the server's value.
        return stat == Stat.ATTACK ? Float.intBitsToFloat(valueAt(ATTACK_VALUE)) : player.getAttributeValue(stat.attribute);
    }
    @Override public int get(int index) { return valueAt(index / 2) >>> (index % 2 * 16) & 65535; }
    @Override public void set(int index, int value) {
        int shift = index % 2 * 16;
        client[index / 2] = (client[index / 2] & ~(65535 << shift)) | ((value & 65535) << shift);
    }
    @Override public int getCount() { return client.length * 2; }
    public boolean click(ServerPlayer player, int id) {
        if (!PlayerAttributes.unlocked(player)) return false;
        if (id == SKILLS || id == STYLE_HUD) {
            if (id == SKILLS) SkillSystem.setEnabled(player, !SkillSettings.enabled(player));
            else {
                var modes = SkillSettings.HudMode.values();
                SkillSettings.set(player, SkillSettings.enabled(player), modes[(SkillSettings.hudMode(player).ordinal() + 1) % modes.length]);
            }
            SkillNetwork.sync(player);
            return true;
        }
        if (id == EQUIPMENT || id == EQUIPMENT - 1) { selectedSlot = EQUIPMENT - id; return true; }
        int statCount = PlayerAttributes.Stat.values().length;
        if (id <= ALLOCATE && id > ALLOCATE - statCount) return AccessoryEquipment.get(player).allocate(selectedSlot, Stat.values()[ALLOCATE - id], true, 1);
        if (id <= REFUND && id > REFUND - statCount) return AccessoryEquipment.get(player).allocate(selectedSlot, Stat.values()[REFUND - id], false, 1);
        if (id <= ALLOCATE_TEN && id > ALLOCATE_TEN - statCount) return AccessoryEquipment.get(player).allocate(selectedSlot, Stat.values()[ALLOCATE_TEN - id], true, 10);
        if (id <= REFUND_TEN && id > REFUND_TEN - statCount) return AccessoryEquipment.get(player).allocate(selectedSlot, Stat.values()[REFUND_TEN - id], false, 10);
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
