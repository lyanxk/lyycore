package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RevelDancer extends RevelMinion {
    private int actionTicks, dashTicks;
    private Vec3 destination;
    private boolean attacking;

    public RevelDancer(EntityType<? extends RevelDancer> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 10);
    }
    @Override protected void updateWithBoss(LifeRevel boss) {
        LivingEntity target = boss.getTarget();
        setTarget(target);
        if (target == null || !target.isAlive()) {
            dashTicks = 0;
            Vec3 home = boss.position().add(0, 0.5, 0).subtract(position());
            setDeltaMovement(getDeltaMovement().lerp(home.normalize().scale(home.length() > 4 ? 0.2 : 0), 0.2));
            return;
        }
        face(target.getEyePosition());
        Vec3 look = target.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter());
        setXRot((float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance())));
        if (--actionTicks <= 0) chooseAction(target);
        if (dashTicks > 0) {
            dash();
            return;
        }
        Vec3 radial = position().subtract(target.position()).multiply(1, 0, 1);
        Vec3 direction = radial.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : radial.normalize();
        double strafe = ((tickCount + getId() * 7) / 30 % 2 == 0 ? 1 : -1) * getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.45;
        Vec3 drift = new Vec3(-direction.z, 0, direction.x).scale(strafe)
                .add(direction.scale(Mth.clamp((4 - radial.length()) * 0.04, -0.12, 0.12)))
                .add(0, Mth.clamp((target.getY() + 0.6 - getY()) * 0.15, -0.15, 0.15), 0);
        setDeltaMovement(getDeltaMovement().lerp(drift, 0.3));
    }
    private void chooseAction(LivingEntity target) {
        actionTicks = 30;
        attacking = distanceToSqr(target) < 36 && random.nextBoolean();
        if (attacking) {
            destination = target.position().add(0, target.getBbHeight() / 2 - getBbHeight() / 2, 0);
            animate("attack", 13);
        } else {
            double angle = random.nextDouble() * Math.PI * 2;
            destination = target.position().add(Math.cos(angle) * 4, 0.6, Math.sin(angle) * 4);
            animate("flight", 16);
        }
        dashTicks = 16;
    }
    private void dash() {
        Vec3 offset = destination.subtract(position());
        Vec3 velocity = offset.normalize().scale(Math.min(attacking ? 0.9 : 0.65, offset.length()));
        setDeltaMovement(velocity);
        if (--dashTicks == 0 || offset.lengthSqr() < 0.04 || horizontalCollision) {
            dashTicks = 0;
            setDeltaMovement(getDeltaMovement().scale(0.25));
        }
    }
    @Override public void travel(Vec3 input) {
        Vec3 before = getBoundingBox().getCenter();
        super.travel(input);
        LivingEntity target = getTarget();
        if (level().isClientSide || !attacking || target == null || !target.isAlive()) return;
        Vec3 after = getBoundingBox().getCenter();
        var hitbox = target.getBoundingBox().inflate(getBbWidth() / 2);
        // Use the movement that actually happened after block collision, then retreat
        // from the point of impact rather than reversing before reaching the target.
        if (hitbox.contains(after) || hitbox.clip(before, after).isPresent()) {
            doHurtTarget(target);
            Vec3 direction = after.subtract(before).normalize();
            if (direction.lengthSqr() < 0.001) direction = target.position().subtract(position()).normalize();
            destination = position().subtract(direction);
            attacking = false;
            dashTicks = 4;
            setDeltaMovement(Vec3.ZERO);
        }
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide && isAlive() && getTarget() != null) {
            chooseAction(getTarget());
            dash();
        }
        return damaged;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ActionTicks", actionTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        actionTicks = Math.clamp(tag.getInt("ActionTicks"), 0, 30);
    }
}
