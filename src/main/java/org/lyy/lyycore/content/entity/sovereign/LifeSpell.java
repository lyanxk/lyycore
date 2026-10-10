package org.lyy.lyycore.content.entity.sovereign;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.lyy.lyycore.registry.LyyEntities;

/** Persistent, bounded-lifetime spell actors. Only the owning live phase can deal damage. */
public final class LifeSpell extends Entity {
    public enum Kind {
        MISSILE("cast_projectile", 40, 16), SPIKE("cast_ground_spikes", 58, 18),
        METEOR("cast_meteor", 70, 43), CRYSTAL("", 0, 0);
        public final String animation;
        public final int animationTicks, impactTick;
        Kind(String animation, int animationTicks, int impactTick) {
            this.animation = animation; this.animationTicks = animationTicks; this.impactTick = impactTick;
        }
    }
    public static final int CRYSTAL_TRACK_TICKS = 60, CRYSTAL_LOCK_TICKS = 60, CRYSTAL_FIRE_TICKS = 32;
    public static final int CRYSTAL_CYCLE_TICKS = CRYSTAL_TRACK_TICKS + CRYSTAL_LOCK_TICKS + CRYSTAL_FIRE_TICKS;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(LifeSpell.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> STARTED = SynchedEntityData.defineId(LifeSpell.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> AIM = SynchedEntityData.defineId(LifeSpell.class, EntityDataSerializers.VECTOR3);
    private UUID owner, target;
    private int age, attacks;
    private boolean homing;
    private Vec3 locked = Vec3.ZERO;
    public LifeSpell(EntityType<? extends LifeSpell> type, Level level) { super(type, level); }
    public Kind kind() { return Kind.values()[entityData.get(KIND)]; }
    public float animationTicks(float partial) { return Math.max(0, level().getGameTime() - entityData.get(STARTED) + partial); }
    public Vec3 aim() { return new Vec3(entityData.get(AIM)); }
    public static UUID launch(LifeSovereign boss, ServerPlayer target, Kind kind, Vec3 direction) {
        return launch(boss, target, kind, direction, true);
    }
    public static UUID launch(LifeSovereign boss, ServerPlayer target, Kind kind, Vec3 direction, boolean homing) {
        var spell = LyyEntities.LIFE_SPELL.get().create(boss.level()); if (spell == null) return null;
        spell.owner = boss.getUUID(); spell.target = target.getUUID(); spell.entityData.set(KIND, kind.ordinal());
        spell.entityData.set(STARTED, boss.level().getGameTime());
        spell.homing = homing;
        spell.setPos(kind == Kind.SPIKE || kind == Kind.METEOR ? target.position() : boss.getEyePosition());
        if (kind == Kind.MISSILE && boss.phase() == LifeSovereign.Phase.USURPER) {
            // Palm position at the authored 0.8-second release key, scaled like the mage renderer.
            Vec3 palm = new Vec3(-7.231160325, 37.151836798, -11.138495556).scale(.76 / 16)
                    .yRot((float)Math.toRadians(180 - boss.getYRot()));
            spell.setPos(boss.position().add(palm));
        }
        if (kind == Kind.MISSILE) spell.setDeltaMovement((direction == null ? target.getEyePosition().subtract(spell.position()).normalize() : direction).scale(.45));
        return boss.level().addFreshEntity(spell) ? spell.getUUID() : null;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0); builder.define(STARTED, 0L); builder.define(AIM, new Vector3f(0, -1, 0));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        var server = (ServerLevel)level();
        if (owner == null || !(server.getEntity(owner) instanceof LifeSovereign boss) || !boss.active()) { discard(); return; }
        ServerPlayer victim = target != null && server.getPlayerByUUID(target) instanceof ServerPlayer player ? player : null;
        if (victim == null || !victim.isAlive()) {
            victim = boss.battle().players(server).stream().findFirst().orElse(null);
            if (victim == null) { entityData.set(STARTED, level().getGameTime() - age); return; }
            target = victim.getUUID();
        }
        age++;
        switch (kind()) {
            case MISSILE -> missile(server, boss, victim);
            case SPIKE, METEOR -> areaSpell(server, boss);
            case CRYSTAL -> crystal(server, boss, victim);
        }
    }
    private void missile(ServerLevel server, LifeSovereign boss, ServerPlayer target) {
        if (homing && (boss.phase() == LifeSovereign.Phase.COCOON || age <= 20)) {
            var desired = target.getEyePosition().subtract(position()).normalize().scale(.45);
            setDeltaMovement(getDeltaMovement().lerp(desired, .25).normalize().scale(.45));
        }
        var from = position(); var to = from.add(getDeltaMovement());
        var block = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var end = block.getLocation();
        for (var player : boss.battle().players(server)) {
            var bounds = player.getBoundingBox().inflate(.25);
            if (bounds.contains(from) || bounds.clip(from, end).isPresent()) {
                boss.magicHit(player, boss.phase() == LifeSovereign.Phase.USURPER ? 24 : 30); discard(); return;
            }
        }
        setPos(to);
        if (block.getType() != HitResult.Type.MISS || age >= 160) discard();
    }
    private void areaSpell(ServerLevel server, LifeSovereign boss) {
        int delay = kind().impactTick;
        double radius = kind() == Kind.SPIKE ? 1 : 2.5;
        if (age < delay) return; // The spell model supplies the target warning throughout the windup.
        if (age == delay) {
            var bounds = new AABB(position().add(-radius, -1, -radius), position().add(radius, 4, radius));
            for (var player : boss.battle().players(server)) if (bounds.intersects(player.getBoundingBox())) boss.magicHit(player, kind() == Kind.SPIKE ? 30 : 36);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY()+1, getZ(), kind() == Kind.SPIKE ? 1 : 4, radius/2, .4, radius/2, 0);
        }
        // Keep the actor for the impact/recovery animation, without repeating its damage.
        if (age >= kind().animationTicks) discard();
    }
    private void crystal(ServerLevel server, LifeSovereign boss, ServerPlayer target) {
        int step = (age-1)%CRYSTAL_CYCLE_TICKS;
        if (step < CRYSTAL_TRACK_TICKS) {
            var destination = target.position().add(0, 3, 0);
            var delta = destination.subtract(position());
            setPos(position().add(delta.normalize().scale(Math.min(.3, delta.length()))));
        } else {
            if (step == CRYSTAL_TRACK_TICKS) {
                locked = target.getBoundingBox().getCenter();
                entityData.set(AIM, locked.subtract(position()).toVector3f());
            }
        }
        if (step == CRYSTAL_TRACK_TICKS + CRYSTAL_LOCK_TICKS) {
            var direction = locked.subtract(position()).normalize();
            var end = server.clip(new ClipContext(position(), position().add(direction.scale(48)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
            entityData.set(AIM, end.subtract(position()).toVector3f());
        }
        // The beam reaches full width at the 0.16-second key; resolve its one hit there.
        if (step == CRYSTAL_TRACK_TICKS + CRYSTAL_LOCK_TICKS + 3) {
            var end = position().add(aim());
            for (var player : boss.battle().players(server)) {
                var bounds = player.getBoundingBox().inflate(.3);
                if (bounds.contains(position()) || bounds.clip(position(), end).isPresent()) boss.magicHit(player, 45);
            }
            attacks++;
        }
        if (step == CRYSTAL_CYCLE_TICKS - 1 && attacks >= 3) { boss.crystalFinished(getUUID()); discard(); }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner); if (target != null) tag.putUUID("Target", target);
        tag.putInt("Kind", kind().ordinal()); tag.putInt("Age", age); tag.putInt("Attacks", attacks); tag.putBoolean("Homing", homing);
        tag.putDouble("X", locked.x); tag.putDouble("Y", locked.y); tag.putDouble("Z", locked.z);
        var aim = aim(); tag.putDouble("AimX", aim.x); tag.putDouble("AimY", aim.y); tag.putDouble("AimZ", aim.z);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; target = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        entityData.set(KIND, Math.clamp(tag.getInt("Kind"), 0, 3)); age = tag.getInt("Age"); attacks = tag.getInt("Attacks"); homing = tag.getBoolean("Homing");
        locked = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        entityData.set(STARTED, level().getGameTime() - age);
        entityData.set(AIM, new Vector3f((float)tag.getDouble("AimX"), (float)tag.getDouble("AimY"), (float)tag.getDouble("AimZ")));
    }
}
