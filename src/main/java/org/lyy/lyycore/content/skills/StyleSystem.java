package org.lyy.lyycore.content.skills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Style selection is independent of the skill bindings and survives death. */
public final class StyleSystem {
    public enum Style {
        BUILDING("building"), MOBILITY("mobility"), TECHNIQUE("technique"), OFFENSE("offense");
        public final String key;
        Style(String key) { this.key = key; }
        public Style next() { return values()[(ordinal() + 1) % values().length]; }
    }
    private static final String KEY = "lyycore:style";
    private StyleSystem() { }
    public static Style current(Player player) {
        String saved = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getString(KEY);
        for (Style style : Style.values()) if (style.key.equals(saved)) return style;
        return Style.BUILDING;
    }
    public static void select(Player player, Style style) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putString(KEY, style.key);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
