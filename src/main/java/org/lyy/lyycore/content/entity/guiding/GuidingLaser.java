package org.lyy.lyycore.content.entity.guiding;

import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.lyy.lyycore.registry.LyyEntities;

/** Independent telegraphs let supporters begin a new aim every two seconds. */
public final class GuidingLaser extends Entity {
    private static final EntityDataAccessor<Vector3f> DIRECTION = SynchedEntityData.defineId(GuidingLaser.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> FIRING = SynchedEntityData.defineId(GuidingLaser.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TRACKING = SynchedEntityData.defineId(GuidingLaser.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> STARTED = SynchedEntityData.defineId(GuidingLaser.class, EntityDataSerializers.LONG);
    public static final double LENGTH = 40;
    private UUID ownerId, targetId;
    private boolean tracking;
    private int age;
    public GuidingLaser(EntityType<? extends GuidingLaser> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DIRECTION, new Vector3f(0, 0, 1)); builder.define(FIRING, false);
        builder.define(TRACKING, false); builder.define(STARTED, 0L);
    }
    public static void fire(Mob owner, LivingEntity target, boolean tracking) {
        var beam = LyyEntities.GUIDING_LASER.get().create(owner.level());
        if (beam == null) return;
        beam.ownerId = owner.getUUID(); beam.targetId = target.getUUID(); beam.tracking = tracking;
        beam.entityData.set(TRACKING, tracking); beam.entityData.set(STARTED, owner.level().getGameTime());
        beam.setPos(origin(owner)); beam.aim(target.getBoundingBox().getCenter().subtract(beam.position()).normalize());
        if (owner instanceof GuidingGuard guard) guard.laserAnimation("aim");
        owner.level().addFreshEntity(beam);
    }
    public Vec3 direction() { return new Vec3(entityData.get(DIRECTION)); }
    public boolean firing() { return entityData.get(FIRING); }
    public boolean tracking() { return entityData.get(TRACKING); }
    public float animationTime(float partial) { return Math.max(0, (level().getGameTime() - entityData.get(STARTED) + partial) / 20F); }
    private static Vec3 origin(Mob owner) {
        if (owner instanceof GuidingBoss boss) return boss.laserOrigin();
        if (owner instanceof GuidingGuard guard) return guard.laserOrigin();
        return owner.getEyePosition();
    }
    private void aim(Vec3 direction) { entityData.set(DIRECTION, direction.toVector3f()); }
    public static Vec3 turnTowards(Vec3 current, Vec3 desired, double limit) {
        double angle = Math.acos(Math.clamp(current.dot(desired), -1, 1));
        if (angle <= limit) return desired;
        var perpendicular = desired.subtract(current.scale(current.dot(desired))).normalize();
        if (perpendicular.lengthSqr() < .00001) perpendicular = current.cross(Math.abs(current.y) > .9 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize();
        return current.scale(Math.cos(limit)).add(perpendicular.scale(Math.sin(limit))).normalize();
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (ownerId == null || targetId == null || !(server.getEntity(ownerId) instanceof Mob owner) || !owner.isAlive()
                || owner instanceof GuidingGuard guard && guard.down()
                || !(server.getEntity(targetId) instanceof Player target) || !target.isAlive()) { discard(); return; }
        setPos(origin(owner));
        if (!tracking && owner instanceof GuidingGuard guard) {
            if (age == 60) guard.laserAnimation("aim_locked");
            if (age == 66) guard.laserAnimation("fire");
        }
        Vec3 desired = target.getBoundingBox().getCenter().subtract(position()).normalize();
        if (tracking && age >= 20) aim(turnTowards(direction(), desired, Math.toRadians(2.5)));
        else if (!tracking && age < 60) aim(desired);
        boolean firing = tracking ? age >= 20 : age >= 66;
        entityData.set(FIRING, firing);
        if (firing && (tracking ? (age - 20) % GuidingBoss.TRACKING_DAMAGE_INTERVAL == 0 : age == 66)) {
            Vec3 end = position().add(direction().scale(LENGTH));
            for (var victim : server.getEntitiesOfClass(Player.class, new AABB(position(), end).inflate(.3),
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
                var bounds = victim.getBoundingBox().inflate(.25);
                if (bounds.contains(position()) || bounds.clip(position(), end).isPresent())
                    victim.hurt(damageSources().indirectMagic(this, owner), tracking ? GuidingBoss.TRACKING_DAMAGE : 40);
            }
        }
        if (++age >= (tracking ? 140 : 70)) discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (targetId != null) tag.putUUID("Target", targetId);
        tag.putInt("Age", age); tag.putBoolean("Tracking", tracking);
        var direction = direction(); tag.putDouble("Dx", direction.x); tag.putDouble("Dy", direction.y); tag.putDouble("Dz", direction.z);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        age = tag.getInt("Age"); tracking = tag.getBoolean("Tracking");
        entityData.set(TRACKING, tracking); entityData.set(STARTED, level().getGameTime() - age);
        aim(new Vec3(tag.getDouble("Dx"), tag.getDouble("Dy"), tag.getDouble("Dz")).normalize());
    }
}
