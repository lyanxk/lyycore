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

/** One entry point for elemental strikes: (base + attack) × global × element. */
public final class SpecialDamage {
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
    /** Combine all strikes into one hit; attack damage is included in every strike. */
    public static void burst(Player player, Entity direct, LivingEntity target, Element element, float base, int hits) {
        burstFromBase(player, direct, target, element,
                base + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), hits);
    }
    public static void burstFromBase(Player player, Entity direct, LivingEntity target, Element element, double base, int hits) {
        burstFromBase(player, direct, target, target, element, base, hits);
    }
    /**
     * The caller resolves healthOwner from target once. Damage still goes through target.hurt,
     * so multipart entities retain their own forwarding, hit-location scaling and protection.
     */
    public static void burstFromBase(Player player, Entity direct, Entity target, LivingEntity healthOwner,
                                     Element element, double base, int hits) {
        if (hits <= 0 || !healthOwner.isAlive() || healthOwner.isRemoved() || target.isRemoved()) return;
        // Multiply the complete formula before clamping, and leave vanilla hurt cooldown intact.
        float damage = scale(player, element, base * hits);
        if (damage > 0) target.hurt(source(player, direct, element), damage);
    }
}
