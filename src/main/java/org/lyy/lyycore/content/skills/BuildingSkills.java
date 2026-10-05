package org.lyy.lyycore.content.skills;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Building-style flight permission and its damage penalty share no combat/input state. */
@EventBusSubscriber(modid = "lyycore")
public final class BuildingSkills {
    private static final String FLIGHT = "lyycore:building_flight";
    private static boolean applyingPenalty;
    private BuildingSkills() { }
    public static void updateFlight(ServerPlayer player) {
        var abilities = player.getAbilities();
        boolean enabled = BasicSkills.active(player, StyleSystem.Style.BUILDING);
        boolean owned = player.getPersistentData().getBoolean(FLIGHT);
        if (enabled && !abilities.mayfly) {
            abilities.mayfly = true;
            player.getPersistentData().putBoolean(FLIGHT, true);
            player.onUpdateAbilities();
        } else if (!enabled && owned) {
            player.getPersistentData().remove(FLIGHT);
            if (!player.isCreative() && !player.isSpectator()) { abilities.mayfly = abilities.flying = false; player.onUpdateAbilities(); }
        }
    }
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if (applyingPenalty || !(event.getEntity() instanceof ServerPlayer player)
                || !BasicSkills.active(player, StyleSystem.Style.BUILDING)) return;
        int invulnerability = player.invulnerableTime;
        applyingPenalty = true;
        try { player.invulnerableTime = 0; player.hurt(player.damageSources().fellOutOfWorld(), player.getMaxHealth() / 4); }
        finally { player.invulnerableTime = invulnerability; applyingPenalty = false; }
    }
}
