package org.lyy.lyycore.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import org.lyy.lyycore.registry.LyyEffects;

public final class DragonFireEffect extends MobEffect {
    private static final String OWNER = "lyycore:dragon_fire_owner";
    public DragonFireEffect() { super(MobEffectCategory.HARMFUL, 0xDC92F2); }
    public static void ignite(LivingEntity target, ServerPlayer player) {
        var current = target.getEffect(LyyEffects.DRAGON_FIRE);
        int amplifier = current == null ? 0 : Math.min(2, current.getAmplifier()+1);
        target.getPersistentData().putUUID(OWNER, player.getUUID());
        target.addEffect(new MobEffectInstance(LyyEffects.DRAGON_FIRE, 100, amplifier), player);
    }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration%20 == 0; }
    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (!target.level().isClientSide) {
            var tag = target.getPersistentData();
            var player = tag.hasUUID(OWNER) ? target.level().getPlayerByUUID(tag.getUUID(OWNER)) : null;
            float damage = amplifier >= 2 ? 400 : amplifier == 1 ? 100 : 50;
            if (player instanceof ServerPlayer server) CombatDamage.hit(server, target, CombatDamage.source(server, "dragon_laser"), damage);
            else target.hurt(target.damageSources().magic(), damage);
        }
        return true;
    }
}
