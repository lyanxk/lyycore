package org.lyy.lyycore.content.wings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.ResearchProgress;

/** Missing settings preserve the strongest unlocked pursuit and the original flight behavior. */
public final class WingsSettings {
    private static final String KEY = "lyycore:wings_settings";
    private static final ResourceLocation BLAZING = ResourceLocation.parse("lyycore:research/blazing_pursuit");
    private static CompoundTag data(Player player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY); }
    public static int maxPursuit(Player player) {
        if (!AegisWings.unlocked(player)) return 0;
        return ResearchProgress.completed(player, BLAZING) ? 3 : AegisWings.level(player) >= 2 ? 2 : 1;
    }
    public static int pursuit(Player player) { var data = data(player); return data.contains("Pursuit") ? Math.clamp(data.getInt("Pursuit"), 0, maxPursuit(player)) : maxPursuit(player); }
    public static boolean flight(Player player) { return !data(player).getBoolean("FlightDisabled"); }
    public static int acceleration(Player player) { var data = data(player); return data.contains("Acceleration") ? Math.clamp(data.getInt("Acceleration"), 0, 100) : 100; }
    public static void set(Player player, int pursuit, boolean flight, int acceleration) {
        var data = new CompoundTag();
        data.putInt("Pursuit", Math.clamp(pursuit, 0, 3)); data.putBoolean("FlightDisabled", !flight);
        data.putInt("Acceleration", Math.clamp(acceleration, 0, 100));
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KEY, data); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
