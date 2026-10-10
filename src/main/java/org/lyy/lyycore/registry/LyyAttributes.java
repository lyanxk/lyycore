package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Elemental and global multipliers extend the native player attribute map. */
@EventBusSubscriber(modid = "lyycore")
public final class LyyAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, "lyycore");
    public static final DeferredHolder<Attribute, Attribute> DAMAGE = register("damage_multiplier", 1);
    public static final DeferredHolder<Attribute, Attribute> PHYSICAL = register("physical_multiplier", 1);
    public static final DeferredHolder<Attribute, Attribute> ICE = register("ice_multiplier", 1);
    public static final DeferredHolder<Attribute, Attribute> FIRE = register("fire_multiplier", 1);
    public static final DeferredHolder<Attribute, Attribute> LIGHTNING = register("lightning_multiplier", 1);
    private static DeferredHolder<Attribute, Attribute> register(String name, double base) {
        return ATTRIBUTES.register(name, () -> new RangedAttribute("attribute.lyycore." + name, base, 0, 1_000_000).setSyncable(true));
    }
    @SubscribeEvent public static void attach(EntityAttributeModificationEvent event) {
        for (var attribute : ATTRIBUTES.getEntries()) event.add(EntityType.PLAYER, attribute);
    }
}
