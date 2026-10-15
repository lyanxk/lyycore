package org.lyy.lyycore.content;

import org.lyy.lyycore.content.research.ResearchChangedEvent;
import org.lyy.lyycore.content.research.ResearchProgress;
import org.lyy.lyycore.registry.LyyAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Shared attribute definitions and source-keyed modifiers for equipment, skills and research. */
@EventBusSubscriber(modid = "lyycore")
public final class PlayerAttributes {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/self");
    private static final ResourceLocation ALLOCATION = ResourceLocation.parse("lyycore:allocated_points");
    public enum Stat {
        LIFE(Attributes.MAX_HEALTH, 2), ARMOR(Attributes.ARMOR, 2), TOUGHNESS(Attributes.ARMOR_TOUGHNESS, 1), ATTACK(Attributes.ATTACK_DAMAGE, 2),
        DAMAGE(LyyAttributes.DAMAGE, 0.02), PHYSICAL(LyyAttributes.PHYSICAL, 0.02), ICE(LyyAttributes.ICE, 0.02),
        FIRE(LyyAttributes.FIRE, 0.02), LIGHTNING(LyyAttributes.LIGHTNING, 0.02);
        public final Holder<Attribute> attribute;
        public final double perPoint;
        Stat(Holder<Attribute> attribute, double perPoint) { this.attribute = attribute; this.perPoint = perPoint; }
    }
    public static boolean unlocked(Player player) { return ResearchProgress.completed(player, RESEARCH); }
    public static void update(ServerPlayer player) {
        // Older versions stored personal allocations. Points now belong to equipped accessories.
        for (var stat : Stat.values()) {
            var instance = player.getAttribute(stat.attribute);
            if (instance != null) instance.removeModifier(ALLOCATION);
        }
        clampHealth(player);
    }
    public static void setPersistentBonus(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation source, double amount) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Attribute bonus must be finite");
        replaceBonus(player.getAttribute(attribute), source, amount, AttributeModifier.Operation.ADD_VALUE, true);
        if (attribute.equals(Attributes.MAX_HEALTH)) clampHealth(player);
    }
    /** Future equipment/research bonuses use stable source IDs so refreshes never stack twice. */
    public static void setBonus(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation source,
                                double amount, AttributeModifier.Operation operation) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Attribute bonus must be finite");
        replaceBonus(player.getAttribute(attribute), source, amount, operation, false);
        if (attribute.equals(Attributes.MAX_HEALTH)) clampHealth(player);
    }
    private static void replaceBonus(AttributeInstance instance, ResourceLocation source, double amount,
                                     AttributeModifier.Operation operation, boolean permanent) {
        if (instance == null) return;
        // Remove first even when the amount matches, to migrate an old transient allocation.
        instance.removeModifier(source);
        if (amount == 0) return;
        var modifier = new AttributeModifier(source, amount, operation);
        if (permanent) instance.addPermanentModifier(modifier);
        else instance.addTransientModifier(modifier);
    }
    private static void clampHealth(ServerPlayer player) {
        float maximum = player.getMaxHealth();
        if (player.getHealth() > maximum) player.setHealth(maximum);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { update((ServerPlayer)event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { update((ServerPlayer)event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { update((ServerPlayer)event.getEntity()); }
    @SubscribeEvent public static void researchChanged(ResearchChangedEvent event) {
        if (event.id.equals(RESEARCH)) update(event.player);
    }
}
