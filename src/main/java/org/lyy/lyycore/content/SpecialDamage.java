package org.lyy.lyycore.content;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
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
        double value = (base + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))
                * player.getAttributeValue(LyyAttributes.DAMAGE) * player.getAttributeValue(element.attribute);
        return (float)Math.clamp(value, 0, Float.MAX_VALUE);
    }
    public static DamageSource source(Player player, Entity direct, Element element) {
        return player.damageSources().source(element.type, direct, player);
    }
    public static boolean hit(Player player, Entity direct, Entity target, Element element, float base) {
        return target.hurt(source(player, direct, element), amount(player, element, base));
    }
    /** Resolve independent hits immediately, including normal damage events and player attribution. */
    public static void burst(Player player, Entity direct, net.minecraft.world.entity.LivingEntity target, Element element, float base, int hits) {
        int previous = target.invulnerableTime;
        try {
            for (int i = 0; i < hits && target.isAlive(); i++) {
                target.invulnerableTime = 0;
                hit(player, direct, target, element, base);
            }
        } finally { target.invulnerableTime = previous; }
    }
}
