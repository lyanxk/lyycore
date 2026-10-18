package org.lyy.lyycore.content.trial;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.lyy.lyycore.registry.LyyEntities;

/** Fixed telegraphs and moving projectiles share timing and the trial's hit settlement. */
public final class TrialAttack extends Entity {
    public enum Kind { SHOT, SPIKE, BARRAGE, THREADS }
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAIT = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> AIM = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> GROUND = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> IMPACT = SynchedEntityData.defineId(TrialAttack.class, EntityDataSerializers.LONG);
    private UUID owner, target;
    private final Set<UUID> touched = new HashSet<>();
    private SpacetimeTrials.Battle battle;

    public TrialAttack(EntityType<? extends TrialAttack> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0); b.define(START, 0L); b.define(DELAY, 0); b.define(WAIT, 0); b.define(PHASE, 0);
        b.define(AIM, new Vector3f(0,0,1)); b.define(GROUND, 0F); b.define(IMPACT, -1L);
    }
    public Kind kind() { return Kind.values()[entityData.get(KIND)]; }
    public int phase() { return entityData.get(PHASE); }
    public int delay() { return entityData.get(DELAY); }
    public int waitTicks() { return entityData.get(WAIT); }
    public float age(float partial) { return level().getGameTime() - entityData.get(START) + partial; }
    public float impactAge(float partial) { return level().getGameTime() - entityData.get(IMPACT) + partial; }
    public Vec3 aim() { return new Vec3(entityData.get(AIM)); }
    public Vec3 destination() { return position().add(aim()); }
    public double ground() { return getY() + entityData.get(GROUND); }
    public static final int FIELD_TICKS = 160, FLIGHT_TICKS = 200;

    static void cast(ServerLevel level, TrialGuide guide, SpacetimeTrials.Battle battle,
                     ServerPlayer target, Kind kind, boolean aerial) {
        Vec3 forward = Vec3.directionFromRotation(0, guide.getYRot());
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 behind = guide.position().add(forward.scale(-2.5)).add(0,1,0);
        if (kind == Kind.THREADS) {
            spawn(level, guide, battle, target, kind, target.position().add(0,1,0), 0, 0);
            return;
        }
        int count = kind == Kind.SHOT ? 4 : 32;
        for (int i = 0; i < count; i++) {
            if (SpacetimeTrials.get(level).battle(battle.boss) == null) break;
            Vec3 position;
            int delay;
            if (kind == Kind.SPIKE) {
                // A jittered disk avoids regular rows without concentrating every spike at the center.
                double radius = 4 * Math.sqrt((i + level.random.nextDouble()) / count);
                double angle = i * 2.399963229728653 + level.random.nextDouble() * .5;
                position = target.position().add(Math.cos(angle)*radius, 0, Math.sin(angle)*radius);
                position = new Vec3(Math.clamp(position.x, battle.bounds.minX+.5, battle.bounds.maxX-.5),
                        battle.bounds.minY, Math.clamp(position.z, battle.bounds.minZ+.5, battle.bounds.maxZ-.5));
                delay = aerial ? 0 : 10;
            } else {
                int columns = kind == Kind.SHOT ? 4 : 8;
                position = behind.add(right.scale((i % columns - (columns-1)/2.0) * .85))
                        .add(0, kind == Kind.SHOT ? 1.3 : (i / columns - 1.5) * .85, 0);
                delay = (aerial ? 0 : kind == Kind.SHOT ? 12 : 10) + i*2;
            }
            spawn(level, guide, battle, target, kind, position, delay, kind == Kind.SHOT && !aerial ? 8 : 0);
        }
    }
    private static void spawn(ServerLevel level, TrialGuide guide, SpacetimeTrials.Battle battle, ServerPlayer target,
                              Kind kind, Vec3 position, int delay, int wait) {
        var attack = LyyEntities.TRIAL_ATTACK.get().create(level);
        if (attack == null) return;
        attack.owner = guide.getUUID(); attack.target = target.getUUID(); attack.battle = battle;
        attack.setPos(position); attack.setYRot(guide.getYRot());
        attack.entityData.set(KIND, kind.ordinal()); attack.entityData.set(START, level.getGameTime());
        attack.entityData.set(DELAY, delay); attack.entityData.set(WAIT, wait);
        attack.entityData.set(AIM, target.position().subtract(position).toVector3f());
        if (kind == Kind.SPIKE) {
            var floor = level.clip(new ClipContext(new Vec3(position.x, target.getY()+.5, position.z), position,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, guide));
            attack.entityData.set(GROUND, (float)(floor.getLocation().y - position.y));
        }
        if (level.addFreshEntity(attack)) {
            battle.attacks.add(attack);
            attack.step(level); // Aerial casts start on this tick, including stagger index zero.
        }
    }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) step(level);
        else if (kind() == Kind.BARRAGE && phase() == 1) setPos(position().add(getDeltaMovement()));
    }
    private void step(ServerLevel level) {
        if (owner == null || SpacetimeTrials.get(level).battle(owner) != battle || battle == null) { discard(); return; }
        if (phase() == 2) {
            if (impactAge(0) >= 13) discard();
            return;
        }
        switch (kind()) {
            case SHOT -> shot(level);
            case SPIKE -> spike(level);
            case BARRAGE -> barrage(level);
            case THREADS -> threads(level);
        }
    }
    private ServerPlayer target(ServerLevel level) {
        var player = level.getServer().getPlayerList().getPlayer(target);
        return player != null && player.isAlive() && player.level() == level ? player : null;
    }
    private void shot(ServerLevel level) {
        if (phase() == 0 && age(0) >= delay()) {
            var player = target(level);
            if (player != null) entityData.set(AIM, player.position().subtract(position()).toVector3f());
            entityData.set(PHASE, 1);
        }
        if (phase() == 1 && age(0) >= delay()+waitTicks()) {
            var point = destination();
            for (var player : battle.players(level)) if (inCylinder(player.getBoundingBox(), point.x, point.z, point.y, point.y+2))
                hit(level, player, .6, false);
            impact();
        }
    }
    private void spike(ServerLevel level) {
        if (age(0) < delay()) return;
        for (var player : battle.players(level)) if (inCylinder(player.getBoundingBox(), getX(), getZ(), battle.bounds.minY, battle.bounds.maxY))
            hit(level, player, .4, false);
        impact();
    }
    /** Cylinder/AABB overlap, shared by both instantaneous attacks (diameter one block). */
    private static boolean inCylinder(AABB box, double x, double z, double bottom, double top) {
        double dx = x - Math.clamp(x, box.minX, box.maxX), dz = z - Math.clamp(z, box.minZ, box.maxZ);
        return box.maxY > bottom && box.minY < top && dx*dx + dz*dz <= .25;
    }
    private void barrage(ServerLevel level) {
        if (age(0) < delay()) return;
        if (phase() == 0) entityData.set(PHASE, 1);
        float flying = age(0) - delay();
        if (flying >= FLIGHT_TICKS || !battle.bounds.inflate(8).contains(position())) { discard(); return; }
        var player = target(level);
        if (flying < 12 && player != null) {
            entityData.set(AIM, player.getBoundingBox().getCenter().subtract(position()).normalize().toVector3f());
            setDeltaMovement(aim().scale(.5)); hasImpulse = true;
        } else if (getDeltaMovement().lengthSqr() < .00001) setDeltaMovement(aim().normalize().scale(.5));
        Vec3 start = position(), end = start.add(getDeltaMovement());
        var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double nearest = block.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceToSqr(block.getLocation());
        ServerPlayer victim = null;
        Vec3 hitPosition = block.getLocation();
        for (var candidate : battle.players(level)) {
            var box = candidate.getBoundingBox().inflate(.15);
            var intersection = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (intersection.isPresent() && start.distanceToSqr(intersection.get()) < nearest) {
                nearest = start.distanceToSqr(intersection.get()); victim = candidate; hitPosition = intersection.get();
            }
        }
        if (nearest != Double.MAX_VALUE) {
            setPos(hitPosition);
            if (victim != null) hit(level, victim, .4, false);
            impact();
        } else setPos(end);
    }
    public Vec3 threadAnchor(int index) {
        double angle = Math.toRadians(getYRot()) + index*Math.PI/3;
        return position().add(6*Math.cos(angle), 0, 6*Math.sin(angle));
    }
    private void threads(ServerLevel level) {
        if (age(0) >= FIELD_TICKS) { discard(); return; }
        for (var player : battle.players(level)) {
            if (touched.contains(player.getUUID())) continue;
            var box = player.getBoundingBox().inflate(.06);
            boolean touching = false;
            for (int i=0; i<6 && !touching; i++) for (int j=i+1; j<6; j++)
                if (box.contains(threadAnchor(i)) || box.clip(threadAnchor(i), threadAnchor(j)).isPresent()) { touching = true; break; }
            if (touching) {
                touched.add(player.getUUID());
                hit(level, player, 0, true); // Restraint has no trial-damage multiplier; contact still deals one physical hit.
            }
        }
    }
    private void hit(ServerLevel level, ServerPlayer player, double multiplier, boolean bind) {
        SpacetimeTrials.get(level).hit(level, owner, player, multiplier, bind);
    }
    private void impact() {
        entityData.set(PHASE, 2); entityData.set(IMPACT, level().getGameTime());
        setDeltaMovement(Vec3.ZERO); hasImpulse = true;
    }
    @Override public void remove(RemovalReason reason) {
        if (battle != null) battle.attacks.remove(this);
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
}
