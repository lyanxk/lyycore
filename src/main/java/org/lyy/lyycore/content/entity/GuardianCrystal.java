package org.lyy.lyycore.content.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class GuardianCrystal extends Projectile {
    private static final int HOMING_TICKS = 40;
    private int lifetimeTicks = 100;
    private LivingEntity homingTarget;

    public GuardianCrystal(EntityType<? extends GuardianCrystal> type, Level level) { super(type, level); setNoGravity(true); }
    public void setHomingTarget(LivingEntity target) { homingTarget = target; }
    public void setLifetimeTicks(int ticks) { lifetimeTicks = Math.max(1, ticks); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override public boolean isPickable() { return true; }
    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return !(entity instanceof GuardianCrystal) && !(entity instanceof GuardianSpikes) && super.canHitEntity(entity);
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && (getOwner() == null || !getOwner().isAlive())) { discard(); return; }
        if (!level().isClientSide) updateHoming();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (!level().isClientSide && hit.getType() != HitResult.Type.MISS) onHit(hit);
        if (!isRemoved()) setPos(position().add(getDeltaMovement()));
        if (!level().isClientSide && tickCount >= lifetimeTicks) discard();
    }
    private void updateHoming() {
        if (homingTarget == null) return;
        if (tickCount > HOMING_TICKS || !homingTarget.isAlive() || homingTarget.isRemoved()
                || homingTarget.level() != level()
                || homingTarget instanceof Player player && (player.isCreative() || player.isSpectator())) {
            homingTarget = null;
            return;
        }
        Vec3 direction = homingTarget.getEyePosition().subtract(position());
        if (direction.lengthSqr() < 1.0E-7) return;
        // Only steer during the first two seconds; retain speed and coast afterwards.
        setDeltaMovement(direction.normalize().scale(getDeltaMovement().length()));
        hasImpulse = true;
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (getOwner() instanceof LivingEntity owner) {
            DamageSource source = damageSources().mobProjectile(this, owner);
            boolean blocked = hit.getEntity() instanceof Player player && player.isDamageSourceBlocked(source);
            if (hit.getEntity() instanceof LivingEntity living) living.invulnerableTime = 0;
            hit.getEntity().hurt(source, 10);
            if (blocked && hit.getEntity() instanceof Player player) player.disableShield();
        }
        shatter();
    }
    @Override protected void onHitBlock(BlockHitResult hit) { shatter(); }
    @Override public boolean hurt(DamageSource source, float amount) {
        // Eggs and snowballs collide, but their zero damage does not break crystal.
        if (isInvulnerableTo(source) || amount <= 0) return false;
        if (!level().isClientSide) shatter();
        return true;
    }
    private void shatter() { playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1, 1.2F); discard(); }
}
