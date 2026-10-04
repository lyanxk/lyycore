package org.lyy.lyycore.content.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.registry.LyyEffects;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** All skill requests pass through the server's unlock checks and current-style dispatch. */
public final class SkillSystem {
    public static final ResourceLocation SPECIAL = ResourceLocation.fromNamespaceAndPath("lyycore", "special");
    private static final String UNLOCKED = "lyycore:skills_unlocked";
    private static final Map<StyleSystem.Style, Map<ResourceLocation, Skill>> BINDINGS = new EnumMap<>(StyleSystem.Style.class);

    public record Skill(ResourceLocation research, Predicate<ServerPlayer> cast) { }
    private SkillSystem() { }

    /** Register a key binding for a style when implementing its concrete research and effect. */
    public static void register(StyleSystem.Style style, ResourceLocation key, Skill skill) {
        if (style == StyleSystem.Style.BUILDING && key.equals(SPECIAL))
            throw new IllegalArgumentException("The special key is only used by mobility, technique and offense");
        if (BINDINGS.computeIfAbsent(style, ignored -> new HashMap<>()).putIfAbsent(key, skill) != null)
            throw new IllegalStateException("Duplicate skill binding: " + style + "/" + key);
    }
    public static boolean unlocked(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(UNLOCKED);
    }
    public static void setUnlocked(Player player, boolean value) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(UNLOCKED, value);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static boolean cast(ServerPlayer player, ResourceLocation key) {
        if (!unlocked(player) || !player.isAlive() || player.isSpectator()
                || player.containerMenu != player.inventoryMenu || player.hasEffect(LyyEffects.CRYSTALLIZATION)) return false;
        var skill = BINDINGS.getOrDefault(StyleSystem.current(player), Map.of()).get(key);
        return skill != null && ResearchProgress.completed(player, skill.research()) && skill.cast().test(player);
    }
}
