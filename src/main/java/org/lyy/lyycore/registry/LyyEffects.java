package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lyy.lyycore.LyyCore;

public final class LyyEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, LyyCore.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> CRYSTALLIZATION = EFFECTS.register("crystallization", () ->
            new MobEffect(MobEffectCategory.HARMFUL, 0xF89CD8) {}
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "crystallization"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.FLYING_SPEED, ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "crystallization_flight"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
}
