package org.lyy.lyycore.content;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
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
    public static float amount(Player player, Element element, float base) {
        return scale(player, element, base + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
    }
    /** Apply multipliers to a complete base formula without adding attack damage again. */
    public static float scale(Player player, Element element, double base) {
        double value = base * player.getAttributeValue(LyyAttributes.DAMAGE) * player.getAttributeValue(element.attribute);
        return (float)Math.clamp(value, 0, Float.MAX_VALUE);
    }
    public static DamageSource source(Player player, Entity direct, Element element) {
        return player.damageSources().source(element.type, direct, player);
    }
    public static boolean hit(Player player, Entity direct, Entity target, Element element, float base) {
        return target.hurt(source(player, direct, element), amount(player, element, base));
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
     * Settle the combined formula through one target.hurt call, preserving hurt cooldown,
     * multipart forwarding and hit-location scaling. Returns that call's acceptance result,
     * not the amount of health lost. No hit is attempted for an invalid target or zero damage.
     */
    public static boolean combinedHitFromBase(Player player, Entity direct, Entity target, LivingEntity healthOwner,
                                              Element element, double basePerStrike, int strikeCount) {
        if (strikeCount <= 0 || !healthOwner.isAlive() || healthOwner.isRemoved() || target.isRemoved()) return false;
        float damage = scale(player, element, basePerStrike * strikeCount);
        return damage > 0 && target.hurt(source(player, direct, element), damage);
    }

    /** @deprecated Compatibility alias for {@link #combinedHit}; this never performs separate hits. */
    @Deprecated
    public static void burst(Player player, Entity direct, LivingEntity target, Element element, float base, int hits) {
        combinedHit(player, direct, target, element, base, hits);
    }
    /** @deprecated Use {@link #combinedHitFromBase} for explicit single-hit settlement and its result. */
    @Deprecated
    public static void burstFromBase(Player player, Entity direct, LivingEntity target, Element element, double base, int hits) {
        combinedHitFromBase(player, direct, target, element, base, hits);
    }
    /** @deprecated Use {@link #combinedHitFromBase} while keeping the actual hit part separate from its owner. */
    @Deprecated
    public static void burstFromBase(Player player, Entity direct, Entity target, LivingEntity healthOwner,
                                     Element element, double base, int hits) {
        combinedHitFromBase(player, direct, target, healthOwner, element, base, hits);
    }
}
