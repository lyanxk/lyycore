package org.lyy.lyycore.content.finality;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.equipment.SpacetimeAccelerationEffect;
import org.lyy.lyycore.content.skills.FlightPermissions;

@EventBusSubscriber(modid = "lyycore")
public final class FinalityEquipment {
    private static final ResourceLocation SOURCE = FinalityContent.id("finality_armor");
    private static boolean reflecting;
    private static final String NIGHT_VISION = "lyycore:finality_night_vision";

    public static boolean helmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof FinalityArmorItem;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isAlive()
                && helmet(player)
                && SpacetimeAccelerationEffect.level(player) == 0
                && player.tickCount % 20 == 0) {
            SpacetimeAccelerationEffect.applyPeriodicEffects(player);
        }
        updateNightVision(player);
        FlightPermissions.update(
                player,
                SOURCE,
                player.isAlive() && FinalityArmorItem.active(player, EquipmentSlot.CHEST, FinalityArmorItem.CHEST_FLIGHT));
        double speed =
                FinalityArmorItem.active(player, EquipmentSlot.FEET, 0)
                        ? FinalityArmorItem.speed(player.getItemBySlot(EquipmentSlot.FEET)) - 1
                        : 0;
        modifier(
                player.getAttribute(Attributes.MOVEMENT_SPEED),
                speed,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(
                player.getAttribute(Attributes.STEP_HEIGHT),
                FinalityArmorItem.active(player, EquipmentSlot.FEET, 1) ? .4 : 0,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static boolean ownsNightVision(
            ServerPlayer player, net.minecraft.world.effect.MobEffectInstance effect) {
        return player.getPersistentData().getBoolean(NIGHT_VISION)
                && effect != null
                && effect.isInfiniteDuration()
                && effect.getAmplifier() == 0;
    }

    private static void updateNightVision(ServerPlayer player) {
        var type = net.minecraft.world.effect.MobEffects.NIGHT_VISION;
        var current = player.getEffect(type);
        boolean owned = ownsNightVision(player, current);
        boolean enabled =
                player.isAlive() && FinalityArmorItem.active(player, EquipmentSlot.HEAD, FinalityArmorItem.HEAD_NIGHT_VISION);
        if (!enabled) {
            player.getPersistentData().remove(NIGHT_VISION);
            if (!owned) return;
            var previous = current.hiddenEffect;
            while (previous != null
                    && !previous.isInfiniteDuration()
                    && previous.getDuration() <= 0) previous = previous.hiddenEffect;
            if (previous == null) player.removeEffect(type);
            else player.forceAddEffect(previous, player);
            return;
        }
        if (owned) return;
        player.getPersistentData().remove(NIGHT_VISION);
        // Let an existing potion run normally. Cover its final seconds before vanilla starts
        // flickering.
        if (current != null && (current.isInfiniteDuration() || current.getDuration() > 220))
            return;
        var effect =
                new net.minecraft.world.effect.MobEffectInstance(type, -1, 0, false, false, false);
        effect.hiddenEffect = current;
        player.forceAddEffect(effect, player);
        if (player.getEffect(type) == effect)
            player.getPersistentData().putBoolean(NIGHT_VISION, true);
    }

    @SubscribeEvent
    public static void nightVisionAdded(
            net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !event.getEffectInstance().is(net.minecraft.world.effect.MobEffects.NIGHT_VISION)
                || !ownsNightVision(player, event.getOldEffectInstance())) return;
        var current = event.getOldEffectInstance();
        var incoming = event.getEffectInstance();
        if (incoming.isInfiniteDuration() || incoming.getAmplifier() > 0) {
            // Do not leave an infinite equipment effect hidden beneath a replacement from commands.
            current.duration = 0;
            player.getPersistentData().remove(NIGHT_VISION);
        } else if (current.hiddenEffect == null) {
            current.hiddenEffect = new net.minecraft.world.effect.MobEffectInstance(incoming);
        } else current.hiddenEffect.update(incoming);
    }

    private static void modifier(
            AttributeInstance attribute, double value, AttributeModifier.Operation operation) {
        var previous = attribute.getModifier(SOURCE);
        if (previous != null && previous.amount() == value) return;
        attribute.removeModifier(SOURCE);
        if (value != 0)
            attribute.addTransientModifier(new AttributeModifier(SOURCE, value, operation));
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && FinalityArmorItem.active(player, EquipmentSlot.CHEST, FinalityArmorItem.CHEST_PREVENT_TELEPORT))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var attacker = event.getSource().getEntity();
        if (!reflecting
                && FinalityArmorItem.active(player, EquipmentSlot.LEGS, 0)
                && attacker instanceof LivingEntity target
                && target != player) {
            reflecting = true;
            try {
                SpecialDamage.combinedHitFromBase(
                        player, player, target, SpecialDamage.Element.PHYSICAL, 10_000, 1);
            } finally {
                reflecting = false;
            }
        }
        if (FinalityArmorItem.active(player, EquipmentSlot.CHEST, FinalityArmorItem.CHEST_CANCEL_DAMAGE)) event.setCanceled(true);
    }
}
