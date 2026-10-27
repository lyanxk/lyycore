package org.lyy.lyycore.content.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.skills.FlightPermissions;
import org.lyy.lyycore.registry.LyyEffects;

@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeArmorEffects {
    private static final String EQUIPMENT_LEVEL = "lyycore:spacetime_armor_effect";
    private static final ResourceLocation FLIGHT = ResourceLocation.parse("lyycore:spacetime_acceleration");
    private SpacetimeArmorEffects() { }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        if (!player.isAlive()) return;
        if (player instanceof ServerPlayer server) {
            updateEquipment(server);
            FlightPermissions.update(server, FLIGHT, SpacetimeAccelerationEffect.level(player) >= 4);
        }
        int extra = SpacetimeAccelerationEffect.extraDurationTicks(player);
        if (extra == 0) return;
        // Client countdown follows the same rule; authoritative expiration remains vanilla/server-side.
        for (var effect : player.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;
            if (!player.level().isClientSide && (effect.is(LyyEffects.SPACETIME_LOCK) || effect.is(LyyEffects.TRIAL_BINDING))) continue;
            for (var current = effect; current != null; current = current.hiddenEffect) {
                if (!current.isInfiniteDuration() && current.getDuration() > 0)
                    current.duration = Math.max(1, current.getDuration() - extra);
            }
        }
    }

    @SubscribeEvent public static void afterTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player)
            FlightPermissions.update(player, FLIGHT, player.isAlive() && SpacetimeAccelerationEffect.level(player) >= 4);
    }

    private static boolean equipmentEffect(MobEffectInstance effect, int ownedLevel) {
        return effect != null && ownedLevel > 0 && effect.isInfiniteDuration() && effect.getAmplifier() == ownedLevel - 1;
    }

    private static void updateEquipment(ServerPlayer player) {
        int pieces = SpacetimeArmorItem.equippedPieces(player);
        var data = player.getPersistentData();
        int ownedLevel = data.getInt(EQUIPMENT_LEVEL);
        var current = player.getEffect(LyyEffects.SPACETIME_ACCELERATION);
        boolean owned = equipmentEffect(current, ownedLevel);
        if (!owned && ownedLevel > 0 && current != null) {
            current.getCures().clear();
            current.getEffect().value().fillEffectCures(current.getCures(), current);
            // A stronger command effect may have hidden the former equipment effect. Do not let
            // that stale armor level return after unequipping or changing pieces.
            for (var parent = current; parent.hiddenEffect != null; ) {
                if (equipmentEffect(parent.hiddenEffect, ownedLevel)) parent.hiddenEffect = parent.hiddenEffect.hiddenEffect;
                else parent = parent.hiddenEffect;
            }
        }
        if (pieces == 0) {
            data.remove(EQUIPMENT_LEVEL);
            if (owned) {
                var previous = activeHiddenEffect(current);
                if (previous == null) player.removeEffect(LyyEffects.SPACETIME_ACCELERATION);
                else player.forceAddEffect(previous, player);
            }
        } else if (!owned && current != null && current.getAmplifier() >= pieces - 1) {
            data.remove(EQUIPMENT_LEVEL); // Preserve higher levels supplied by commands.
        } else if (!owned || ownedLevel != pieces) {
            var previous = owned ? activeHiddenEffect(current) : current;
            if (previous != null && previous.getAmplifier() >= pieces - 1) {
                data.remove(EQUIPMENT_LEVEL);
                player.forceAddEffect(previous, player);
                return;
            }
            var effect = new MobEffectInstance(LyyEffects.SPACETIME_ACCELERATION, -1, pieces - 1, false, false, true);
            effect.hiddenEffect = previous;
            effect.getCures().clear();
            player.forceAddEffect(effect, player);
            data.putInt(EQUIPMENT_LEVEL, pieces);
        }
    }

    private static MobEffectInstance activeHiddenEffect(MobEffectInstance effect) {
        var hidden = effect.hiddenEffect;
        while (hidden != null && !hidden.isInfiniteDuration() && hidden.getDuration() <= 0) hidden = hidden.hiddenEffect;
        return hidden;
    }

    @SubscribeEvent public static void preventRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(LyyEffects.SPACETIME_ACCELERATION.getKey())
                && event.getEntity() instanceof ServerPlayer player && player.isAlive()
                && SpacetimeArmorItem.equippedPieces(player) > 0
                && equipmentEffect(event.getEffectInstance(), player.getPersistentData().getInt(EQUIPMENT_LEVEL)))
            event.setCanceled(true);
    }
}
