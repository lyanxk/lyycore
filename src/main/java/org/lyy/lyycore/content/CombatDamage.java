package org.lyy.lyycore.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Shared physical strike handling prevents delayed strikes from scheduling themselves. */
public final class CombatDamage {
    private static final ResourceKey<DamageType> STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("lyycore:skill_strike"));
    private static boolean extra;
    private CombatDamage() { }
    public static boolean extra() { return extra; }
    public static net.minecraft.world.damagesource.DamageSource source(ServerPlayer player, String type) {
        return player.damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("lyycore:"+type)), player);
    }
    public static boolean hit(ServerPlayer player, LivingEntity target, float amount) {
        return hit(player, target, player.damageSources().source(STRIKE, player), amount);
    }
    /** Independent skill hits keep armor and damage events, without sharing vanilla hurt cooldowns. */
    public static boolean hit(ServerPlayer player, LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (amount <= 0) return false;
        int invulnerability = target.invulnerableTime;
        boolean previous = extra;
        extra = true;
        boolean damaged;
        try { target.invulnerableTime = 0; damaged = target.hurt(source, amount); }
        finally { target.invulnerableTime = invulnerability; extra = previous; }
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 3, .2, .3, .2, .01);
        return damaged;
    }
}
