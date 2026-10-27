package org.lyy.lyycore.content;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.entity.PartEntity;
import org.lyy.lyycore.registry.LyyAttributes;

/** Elemental scaling and single-hit settlement, including combined strike formulas. */
public final class SpecialDamage {
    public static boolean isSpecial(DamageSource source) {
        return source.is(Element.PHYSICAL.type) || source.is(Element.ICE.type)
                || source.is(Element.FIRE.type) || source.is(Element.LIGHTNING.type);
    }
    public enum Element {
        PHYSICAL(LyyAttributes.PHYSICAL), ICE(LyyAttributes.ICE), FIRE(LyyAttributes.FIRE), LIGHTNING(LyyAttributes.LIGHTNING);
        public final Holder<Attribute> attribute;
        public final ResourceKey<DamageType> type;
        Element(Holder<Attribute> attribute) {
            this.attribute = attribute;
            type = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("lyycore:special_" + name().toLowerCase(java.util.Locale.ROOT)));
        }
    }
    /** Apply multipliers to a complete base formula without adding attack damage again. */
    public static float scale(Player player, Element element, double base) {
        double value = base * player.getAttributeValue(LyyAttributes.DAMAGE) * player.getAttributeValue(element.attribute);
        return (float)Math.clamp(value, 0, Float.MAX_VALUE);
    }
    public static DamageSource source(Player player, Entity direct, Element element) {
        return player.damageSources().source(element.type, direct, player);
    }
    /** Resolve the life state without replacing the actual hit location. */
    @Nullable public static LivingEntity healthOwner(Entity target) {
        if (target instanceof LivingEntity living) return living;
        return target instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity living ? living : null;
    }
    /** Combine the strike formulas into one hit, adding attack damage to each strike's base. */
    public static boolean combinedHit(Player player, Entity direct, LivingEntity target, Element element,
                                      float basePerStrike, int strikeCount) {
        return combinedHitFromBase(player, direct, target, element,
                basePerStrike + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), strikeCount);
    }
    /** The supplied per-strike base already includes every additive term. */
    public static boolean combinedHitFromBase(Player player, Entity direct, LivingEntity target, Element element,
                                              double basePerStrike, int strikeCount) {
        return combinedHitFromBase(player, direct, target, target, element, basePerStrike, strikeCount);
    }
    /**
     * Lethal formulas execute hostile targets directly. Other hits use one target.hurt call,
     * preserving hurt cooldown, multipart forwarding and hit-location scaling.
     */
    public static boolean combinedHitFromBase(Player player, Entity direct, Entity target, LivingEntity healthOwner,
                                              Element element, double basePerStrike, int strikeCount) {
        if (strikeCount <= 0) return false;
        float damage = scale(player, element, basePerStrike * strikeCount);
        return hit(target, source(player, direct, element), damage);
    }

    /** The execution threshold uses health before the hit and the fully scaled formula. */
    public static boolean hit(Entity target, DamageSource source, float damage) {
        if (damage <= 0 || target.isRemoved() || target.level().isClientSide) return false;
        LivingEntity living = healthOwner(target);
        if (living != null && !living.isAlive()) return false;
        // Phantom implements Enemy too; players and neutral creatures never qualify.
        if (living instanceof Enemy && living.getHealth() < damage) {
            forceKill(living, source, damage);
            return true;
        }
        return target.hurt(source, damage);
    }

    /** Infinity-sword style death settlement, retaining kill credit, loot and death observers. */
    private static void forceKill(LivingEntity target, DamageSource source, float damage) {
        ServerLevel level = (ServerLevel) target.level();
        if (source.getEntity() instanceof Player player) {
            target.setLastHurtByPlayer(player);
            target.lastHurtByPlayerTime = 100;
            target.setLastHurtByMob(player);
        }
        target.getCombatTracker().recordDamage(source, damage);
        target.setHealth(0);
        // Preserve mob-specific death animations and cleanup whenever normal death succeeds.
        target.die(source);
        if (target.isRemoved() || target.dead) return;
        // If normal death was blocked, finish directly without posting the death event again.
        target.setHealth(0);
        LivingEntity killer = target.getKillCredit();
        if (target.deathScore >= 0 && killer != null)
            killer.awardKillScore(target, target.deathScore, source);
        if (target.isSleeping()) target.stopSleeping();
        target.dead = true;
        target.getCombatTracker().recheckStatus();
        Entity attacker = source.getEntity();
        if (attacker == null || attacker.killedEntity(level, target)) {
            target.gameEvent(GameEvent.ENTITY_DIE);
            target.dropAllDeathLoot(level, source);
        }
        level.broadcastEntityEvent(target, (byte) 3);
        target.setPose(Pose.DYING);
    }
}
