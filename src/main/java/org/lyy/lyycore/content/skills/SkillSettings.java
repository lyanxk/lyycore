package org.lyy.lyycore.content.skills;

import net.minecraft.world.entity.player.Player;

/** Player preferences are separate from research unlocks and survive respawning. */
public final class SkillSettings {
    public enum HudMode { ON, ON_SWITCH, OFF }
    private static final String DISABLED = "lyycore:skills_disabled", HUD = "lyycore:style_hud";

    private SkillSettings() { }

    public static boolean enabled(Player player) {
        return !player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(DISABLED);
    }

    public static HudMode hudMode(Player player) {
        int value = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(HUD);
        return value >= 0 && value < HudMode.values().length ? HudMode.values()[value] : HudMode.ON;
    }

    public static void set(Player player, boolean enabled, HudMode hudMode) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        data.putBoolean(DISABLED, !enabled);
        data.putInt(HUD, hudMode.ordinal());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
    }
}
