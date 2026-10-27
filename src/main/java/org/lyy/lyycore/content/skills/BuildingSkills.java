package org.lyy.lyycore.content.skills;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Building-style flight permission and its damage penalty share no combat/input state. */
@EventBusSubscriber(modid = "lyycore")
public final class BuildingSkills {
    private static final net.minecraft.resources.ResourceLocation FLIGHT = net.minecraft.resources.ResourceLocation.parse("lyycore:building_flight");
    private static final String DAMAGE_PENALTY_DISABLED = "lyycore:building_damage_penalty_disabled";
    private static boolean applyingPenalty;
    private BuildingSkills() { }

    /** Server-side, per-player switch for equipment/effects; enabled unless explicitly disabled. */
    public static boolean damagePenaltyEnabled(ServerPlayer player) {
        return !player.getPersistentData().getBoolean(DAMAGE_PENALTY_DISABLED)
                && org.lyy.lyycore.content.equipment.SpacetimeAccelerationEffect.level(player) < 4;
    }
    public static void setDamagePenaltyEnabled(ServerPlayer player, boolean enabled) {
        if (enabled) player.getPersistentData().remove(DAMAGE_PENALTY_DISABLED);
        else player.getPersistentData().putBoolean(DAMAGE_PENALTY_DISABLED, true);
    }

    public static void updateFlight(ServerPlayer player) {
        FlightPermissions.update(player, FLIGHT, BasicSkills.active(player, StyleSystem.Style.BUILDING));
    }
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if (applyingPenalty || !(event.getEntity() instanceof ServerPlayer player)
                || !BasicSkills.active(player, StyleSystem.Style.BUILDING) || !damagePenaltyEnabled(player)) return;
        int invulnerability = player.invulnerableTime;
        applyingPenalty = true;
        try { player.invulnerableTime = 0; player.hurt(player.damageSources().fellOutOfWorld(), player.getMaxHealth() / 4); }
        finally { player.invulnerableTime = invulnerability; applyingPenalty = false; }
    }
}
