package org.lyy.lyycore.content;

import org.lyy.lyycore.content.research.ResearchProgress;
import org.lyy.lyycore.registry.LyyAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;

/** Shared attribute definitions and source-keyed modifiers for equipment, skills and research. */
public final class PlayerAttributes {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/self");
    public enum Stat {
        LIFE(Attributes.MAX_HEALTH, 2), ARMOR(Attributes.ARMOR, 2), TOUGHNESS(Attributes.ARMOR_TOUGHNESS, 1), ATTACK(Attributes.ATTACK_DAMAGE, 2),
        DAMAGE(LyyAttributes.DAMAGE, 0.02), PHYSICAL(LyyAttributes.PHYSICAL, 0.02), ICE(LyyAttributes.ICE, 0.02),
        FIRE(LyyAttributes.FIRE, 0.02), LIGHTNING(LyyAttributes.LIGHTNING, 0.02);
        public final Holder<Attribute> attribute;
        public final double perPoint;
        Stat(Holder<Attribute> attribute, double perPoint) { this.attribute = attribute; this.perPoint = perPoint; }
    }
    public static boolean unlocked(Player player) { return ResearchProgress.completed(player, RESEARCH); }
    public static void setPersistentBonus(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation source, double amount) {
        var instance = player.getAttribute(attribute);
        instance.removeModifier(source);
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(source, amount, AttributeModifier.Operation.ADD_VALUE));
        if (attribute.equals(Attributes.MAX_HEALTH)) clampHealth(player);
    }
    private static void clampHealth(ServerPlayer player) {
        float maximum = player.getMaxHealth();
        if (player.getHealth() > maximum) player.setHealth(maximum);
    }
}
