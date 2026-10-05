package org.lyy.lyycore.content.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.registry.LyyEffects;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** All skill requests pass through the server's unlock checks and current-style dispatch. */
public final class SkillSystem {
    public static final ResourceLocation SPECIAL = ResourceLocation.fromNamespaceAndPath("lyycore", "special");
    private static final String FACTOR_UNLOCKED = "lyycore:factor_unlocked";
    private static final String UNLOCKED = "lyycore:skills_unlocked";
    private static final Map<StyleSystem.Style, Map<ResourceLocation, Skill>> BINDINGS = new EnumMap<>(StyleSystem.Style.class);
    private static final Map<ServerPlayer, Actions> ACTIONS = new WeakHashMap<>();
    private static final class Actions {
        int tick;
        final Set<ResourceLocation> consumed = new HashSet<>();
    }

    public record Skill(Predicate<ServerPlayer> available, Predicate<ServerPlayer> cast) {
        public Skill(ResourceLocation research, Predicate<ServerPlayer> cast) {
            this(player -> ResearchProgress.completed(player, research), cast);
        }
    }
    private SkillSystem() { }

    /** Register a key binding for a style when implementing its concrete research and effect. */
    public static void register(StyleSystem.Style style, ResourceLocation key, Skill skill) {
        if (style == StyleSystem.Style.BUILDING && key.equals(SPECIAL))
            throw new IllegalArgumentException("The special key is only used by mobility, technique and offense");
        if (BINDINGS.computeIfAbsent(style, ignored -> new HashMap<>()).putIfAbsent(key, skill) != null)
            throw new IllegalStateException("Duplicate skill binding: " + style + "/" + key);
    }
    public static boolean unlocked(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(UNLOCKED)
                || player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(FACTOR_UNLOCKED);
    }
    public static void unlockFromFactor(Player player) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(FACTOR_UNLOCKED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static void setUnlocked(Player player, boolean value) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(UNLOCKED, value);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static boolean canUse(ServerPlayer player) {
        return unlocked(player) && player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu && !player.hasEffect(LyyEffects.CRYSTALLIZATION);
    }
    public static boolean cast(ServerPlayer player, ResourceLocation key) {
        if (!canUse(player)) return false;
        var skill = BINDINGS.getOrDefault(StyleSystem.current(player), Map.of()).get(key);
        return skill != null && skill.available().test(player)
                && consumeAction(player, key.withSuffix("/" + StyleSystem.current(player).key)) && skill.cast().test(player);
    }

    /** Reserve before calling a handler, so reentrant calls and repeated input edges cannot double-cast. */
    public static boolean consumeAction(ServerPlayer player, ResourceLocation action) {
        var actions = ACTIONS.computeIfAbsent(player, ignored -> new Actions());
        int tick = player.server.getTickCount();
        if (actions.tick != tick) { actions.tick = tick; actions.consumed.clear(); }
        return actions.consumed.add(action);
    }

    static void forget(ServerPlayer player) { ACTIONS.remove(player); }
    static void clear() { ACTIONS.clear(); }
}
