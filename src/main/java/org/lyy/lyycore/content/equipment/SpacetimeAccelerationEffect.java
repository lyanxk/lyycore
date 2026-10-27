package org.lyy.lyycore.content.equipment;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.lyy.lyycore.registry.LyyEffects;

public final class SpacetimeAccelerationEffect extends MobEffect {
    private static final float[] HEALING = {1, 4, 16, 256};
    public SpacetimeAccelerationEffect() { super(MobEffectCategory.BENEFICIAL, 0xA8D8FF); }

    public static int level(LivingEntity entity) {
        var effect = entity.getEffect(LyyEffects.SPACETIME_ACCELERATION);
        return effect == null ? 0 : Math.min(4, effect.getAmplifier() + 1);
    }
    public static int extraDurationTicks(LivingEntity entity) {
        int level = level(entity);
        return level <= 1 ? 0 : (1 << (level - 1)) - 1;
    }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            player.heal(HEALING[Math.min(3, amplifier)]);
            if (amplifier >= 1) player.causeFoodExhaustion(0.2806F);
        }
        return true;
    }
}
