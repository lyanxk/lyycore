package org.lyy.lyycore.content.skills;

import org.lyy.lyycore.content.combat.ActionRestrictions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.lyy.lyycore.content.research.ResearchProgress;
import org.lyy.lyycore.content.item.SpecialWeapon;
import org.lyy.lyycore.content.wings.WingsScoop;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.Set;

/** All skill requests pass through the server's unlock checks and current-style dispatch. */
public final class SkillSystem {
    private static final ResourceLocation SPECIAL = ResourceLocation.fromNamespaceAndPath("lyycore", "special");
    private static final String FACTOR_UNLOCKED = "lyycore:factor_unlocked";
    private static final String UNLOCKED = "lyycore:skills_unlocked";
    private static final Map<ServerPlayer, Actions> ACTIONS = new WeakHashMap<>();
    private static final class Actions {
        int tick;
        final Set<ResourceLocation> consumed = new HashSet<>();
    }

    private SkillSystem() { }

    public static boolean unlocked(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(UNLOCKED)
                || player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(FACTOR_UNLOCKED);
    }
    public static boolean enabled(Player player) { return unlocked(player) && SkillSettings.enabled(player); }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        SkillSettings.set(player, enabled, SkillSettings.hudMode(player));
        if (!enabled) {
            SkillInput.forget(player);
            GuardSkill.reset(player);
            MovementSkill.forget(player);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new org.lyy.lyycore.network.SkillNetwork.Hover(0));
        }
        BuildingSkills.updateFlight(player);
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
        return enabled(player) && player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu && !ActionRestrictions.frozen(player);
    }
    /** SkillInput has already checked whether this press can be used. */
    static void castSpecial(ServerPlayer player) {
        var style = StyleSystem.current(player);
        if (!consumeAction(player, SPECIAL.withSuffix("/" + style.key))) return;
        switch (style) {
            case BUILDING -> { }
            case MOBILITY -> MovementSkill.cast(player);
            case TECHNIQUE -> {
                if (ResearchProgress.completed(player, BasicSkills.RESEARCH)) GuardSkill.start(player);
            }
            case OFFENSE -> {
                var stack = player.getMainHandItem();
                if (stack.getItem() instanceof SpecialWeapon weapon) {
                    if (SkillInput.sneaking(player)) weapon.useUltimate(player, stack);
                    else weapon.useSkill(player, stack);
                } else if (SkillInput.forwardSpecial(player) && ResearchProgress.completed(player, OffenseSkills.SCOOP_RESEARCH)) {
                    WingsScoop.start(player);
                }
            }
        }
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
