package org.lyy.lyycore.content.equipment;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import org.lyy.lyycore.content.finality.FinalityArmorItem;
import org.lyy.lyycore.registry.LyyEffects;

public final class SpacetimeAccelerationEffect extends MobEffect {
    private static final float[] HEALING = {1, 4, 16, 256};
    public SpacetimeAccelerationEffect() { super(MobEffectCategory.BENEFICIAL, 0xA8D8FF); }

    public static int level(LivingEntity entity) {
        var effect = entity.getEffect(LyyEffects.SPACETIME_ACCELERATION);
        return effect == null ? 0 : Math.min(4, effect.getAmplifier() + 1);
    }
    public static int extraDurationTicks(LivingEntity entity) {
        int level = FinalityArmorItem.active(entity, EquipmentSlot.HEAD, FinalityArmorItem.HEAD_DEBUFF_DECAY)
                ? 4 : level(entity);
        return level <= 1 ? 0 : (1 << (level - 1)) - 1;
    }
    public static void applyPeriodicEffects(ServerPlayer player) {
        int level = level(player);
        float healing = FinalityArmorItem.active(player, EquipmentSlot.HEAD, FinalityArmorItem.HEAD_HEALING)
                ? 256 : level > 0 ? HEALING[level - 1] : 0;
        if (healing > 0) player.heal(healing);
        if (level >= 2 || FinalityArmorItem.active(player, EquipmentSlot.HEAD, FinalityArmorItem.HEAD_EXHAUSTION))
            player.causeFoodExhaustion(.2806F);
    }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player && player.tickCount % 20 == 0)
            applyPeriodicEffects(player);
        return true;
    }
}
