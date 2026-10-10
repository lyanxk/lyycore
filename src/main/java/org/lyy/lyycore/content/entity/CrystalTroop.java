package org.lyy.lyycore.content.entity;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lyy.lyycore.content.raid.OtherworldRaids;

/** Shared target persistence and hovering; the two units differ only in attack and orbit. */
public final class CrystalTroop extends Monster {
    private static final EntityDataAccessor<Long> ATTACK = SynchedEntityData.defineId(CrystalTroop.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> BEAM = SynchedEntityData.defineId(CrystalTroop.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(CrystalTroop.class, EntityDataSerializers.BOOLEAN);
    private final boolean assault;
    private UUID raidId, targetId;
    private Vec3 hoverCenter, destination, chargeDirection = Vec3.ZERO;
    private double chargeRemaining;
    private boolean chargeHit, initializedTarget;
    private int attackCooldown = 40, moveCooldown;
    public CrystalTroop(EntityType<? extends Monster> type, Level level, boolean assault) {
        super(type, level); this.assault = assault; setNoGravity(true); xpReward = 0;
    }
    public static AttributeSupplier.Builder attributes(boolean assault) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, assault ? 500 : 300)
                .add(Attributes.ARMOR, assault ? 30 : 20).add(Attributes.ARMOR_TOUGHNESS, 20)
                .add(Attributes.MOVEMENT_SPEED, .2).add(Attributes.FOLLOW_RANGE, 96);
    }
    // Vanilla's armor attribute clamps at 30; this unit explicitly has 100 armor.
    @Override public int getArmorValue() { return assault ? 100 : super.getArmorValue(); }
    @Override protected void registerGoals() { }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(ATTACK, -100L); builder.define(BEAM, new Vector3f()); builder.define(CHARGING, false);
    }
    public void joinRaid(UUID id) { raidId = id; setPersistenceRequired(); }
    public UUID raidId() { return raidId; }
    public boolean assault() { return assault; }
    public boolean charging() { return entityData.get(CHARGING); }
    public int attackAge() { return (int)(level().getGameTime() - entityData.get(ATTACK)); }
    public Vec3 beamEnd() { return new Vec3(entityData.get(BEAM)); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void travel(Vec3 input) { } // Explicit movement below avoids ground friction/gravity changing the locked charge speed.
    @Override protected void customServerAiStep() {
        if (raidId != null && !OtherworldRaids.get((ServerLevel)level()).contains(raidId)) { discard(); return; }
        var target = targetId == null ? null : level().getPlayerByUUID(targetId);
        if (target != null && (!target.isAlive() || target.isSpectator() || target.isCreative())) {
            targetId = null; target = null; hoverCenter = position(); destination = null;
        }
        if (target == null && (!initializedTarget || tickCount % 20 == 0)) {
            target = level().getNearestPlayer(getX(), getY(), getZ(), initializedTarget ? 10 : -1, true);
            initializedTarget = true;
            if (target != null) targetId = target.getUUID();
        }
        setTarget(target);
        attackCooldown--;
        if (chargeRemaining > 0) { charge(); return; }
        if (target != null && attackCooldown <= 0) {
            attackCooldown = 40;
            if (assault) startCharge(target); else beam(target);
        }
        if (chargeRemaining > 0) { charge(); return; }
        hover(target);
    }
    private void hover(Player target) {
        if (hoverCenter == null) hoverCenter = position();
        if (--moveCooldown <= 0 || destination == null || position().distanceToSqr(destination) < .25) {
            moveCooldown = 20 + random.nextInt(20);
            if (target == null) destination = hoverCenter.add((random.nextDouble()-.5)*5, (random.nextDouble()-.5)*3, (random.nextDouble()-.5)*5);
            else if (assault) {
                double angle = random.nextDouble()*Math.PI*2, distance = 3+random.nextDouble()*3;
                destination = target.position().add(Math.cos(angle)*distance, .5+random.nextDouble(), Math.sin(angle)*distance);
            } else destination = new Vec3(Math.clamp(getX()+(random.nextDouble()-.5)*10, target.getX()-10, target.getX()+10),
                    target.getY()+5+random.nextDouble()*3, Math.clamp(getZ()+(random.nextDouble()-.5)*10, target.getZ()-10, target.getZ()+10));
        }
        if (target != null && !assault) destination = new Vec3(Math.clamp(destination.x, target.getX()-10, target.getX()+10), destination.y, Math.clamp(destination.z, target.getZ()-10, target.getZ()+10));
        var step = destination.subtract(position()); if (step.lengthSqr() > .0144) step = step.normalize().scale(.12);
        move(MoverType.SELF, step); setDeltaMovement(Vec3.ZERO);
        if (target != null) getLookControl().setLookAt(target, 180, 180);
    }
    private void beam(Player target) {
        setPos(getX(), target.getY()+8, getZ()); destination = null;
        entityData.set(BEAM, target.getBoundingBox().getCenter().toVector3f()); entityData.set(ATTACK, level().getGameTime());
        target.hurt(damageSources().indirectMagic(this, this), 20);
    }
    private void startCharge(Player target) {
        var offset = target.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter());
        chargeRemaining = offset.length()*2; chargeDirection = offset.normalize(); chargeHit = false;
        entityData.set(CHARGING, true); entityData.set(ATTACK, level().getGameTime());
        setYRot((float)Math.toDegrees(Math.atan2(-chargeDirection.x, chargeDirection.z))); yBodyRot = getYRot();
    }
    private void charge() {
        var step = chargeDirection.scale(Math.min(.2, chargeRemaining));
        if (!chargeHit) for (var player : level().getEntitiesOfClass(Player.class, getBoundingBox().expandTowards(step).inflate(.1), p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            if (player.getBoundingBox().inflate(getBbWidth()/2, getBbHeight()/2, getBbWidth()/2).clip(getBoundingBox().getCenter(), getBoundingBox().getCenter().add(step)).isPresent()
                    || getBoundingBox().intersects(player.getBoundingBox())) {
                player.hurt(damageSources().mobAttack(this), 60); chargeHit = true; break;
            }
        }
        var previous = position(); move(MoverType.SELF, step); setDeltaMovement(Vec3.ZERO);
        chargeRemaining -= .2;
        if (chargeRemaining <= 0 || position().distanceToSqr(previous) < .0001) {
            chargeRemaining = 0; entityData.set(CHARGING, false); destination = null;
        }
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); if (raidId != null) tag.putUUID("Raid", raidId); if (targetId != null) tag.putUUID("LockedPlayer", targetId);
        tag.putBoolean("InitializedTarget", initializedTarget); tag.putInt("Cooldown", attackCooldown);
        tag.putDouble("ChargeRemaining", chargeRemaining); tag.putBoolean("ChargeHit", chargeHit);
        tag.putDouble("ChargeX", chargeDirection.x); tag.putDouble("ChargeY", chargeDirection.y); tag.putDouble("ChargeZ", chargeDirection.z);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); raidId = tag.hasUUID("Raid") ? tag.getUUID("Raid") : null;
        targetId = tag.hasUUID("LockedPlayer") ? tag.getUUID("LockedPlayer") : null; initializedTarget = tag.getBoolean("InitializedTarget");
        attackCooldown = Math.clamp(tag.getInt("Cooldown"), 1, 40); chargeRemaining = Math.max(0, tag.getDouble("ChargeRemaining"));
        chargeDirection = new Vec3(tag.getDouble("ChargeX"), tag.getDouble("ChargeY"), tag.getDouble("ChargeZ")).normalize(); chargeHit = tag.getBoolean("ChargeHit");
        entityData.set(CHARGING, chargeRemaining > 0);
    }
}
