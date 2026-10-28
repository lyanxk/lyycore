package org.lyy.lyycore.content.finality;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import org.joml.Vector3f;

import java.util.*;

/**
 * Spawn parameters determine both client motion and server collision; no periodic position stream.
 */
public final class FinalityProjectile extends Entity {
    public enum Kind {
        STONE,
        CRYSTAL,
        SWORD,
        BEAM
    }

    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(FinalityProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START =
            SynchedEntityData.defineId(FinalityProjectile.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> ORIGIN =
            SynchedEntityData.defineId(FinalityProjectile.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> VELOCITY =
            SynchedEntityData.defineId(FinalityProjectile.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DELAY =
            SynchedEntityData.defineId(FinalityProjectile.class, EntityDataSerializers.INT);
    private FinalitySummon owner;
    private final Set<UUID> hit = new HashSet<>();

    public FinalityProjectile(EntityType<? extends FinalityProjectile> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0);
        b.define(START, 0L);
        b.define(ORIGIN, new Vector3f());
        b.define(VELOCITY, new Vector3f());
        b.define(DELAY, 0);
    }

    public Kind kind() {
        return Kind.values()[entityData.get(KIND)];
    }

    public Entity owner() {
        return owner;
    }

    public Vec3 velocity() {
        return new Vec3(entityData.get(VELOCITY));
    }

    public float age(float partial) {
        return (float) (level().getGameTime() - entityData.get(START)) + partial;
    }

    public int delay() {
        return entityData.get(DELAY);
    }

    public Vec3 at(float age) {
        return new Vec3(entityData.get(ORIGIN)).add(velocity().scale(Math.max(0, age - delay())));
    }

    public static void cast(ServerLevel level, FinalitySummon owner, ServerPlayer target) {
        Vec3 aim = target.getBoundingBox().getCenter();
        switch (owner.kind()) {
            case STONE -> {
                for (int i = 0; i < 32; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2,
                            r = Math.sqrt(level.random.nextDouble()) * 8;
                    Vec3 p =
                            FinalityTrials.clamp(
                                    new Vec3(
                                            aim.x + Math.cos(angle) * r,
                                            60,
                                            aim.z + Math.sin(angle) * r),
                                    0);
                    spawn(level, owner, Kind.STONE, p, new Vec3(0, -1, 0), 10 + i);
                }
            }
            case CRYSTAL -> {
                Vec3 forward = aim.subtract(owner.position()).normalize(),
                        right = forward.cross(new Vec3(0, 1, 0)).normalize(),
                        up = right.cross(forward).normalize();
                for (int i = 0; i < 16; i++)
                    spawn(
                            level,
                            owner,
                            Kind.CRYSTAL,
                            owner.position()
                                    .add(right.scale((i % 4 - 1.5) * .7))
                                    .add(up.scale((i / 4 - 1.5) * .7)),
                            forward.scale(.8),
                            0);
            }
            case SWORD -> {
                for (int layer = 0; layer < 2; layer++)
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        Vec3 p =
                                FinalityTrials.clamp(
                                        aim.add(
                                                Math.cos(angle) * 3,
                                                layer == 0 ? -1.5 : 1.5,
                                                Math.sin(angle) * 3),
                                        .5);
                        spawn(
                                level,
                                owner,
                                Kind.SWORD,
                                p,
                                aim.subtract(p).normalize().scale(.8),
                                20);
                    }
            }
            case BOOK -> {
                double angle = level.random.nextDouble() * Math.PI * 2;
                Vec3 offset = new Vec3(Math.cos(angle) * 8, 0, Math.sin(angle) * 8);
                Vec3 start = new Vec3(aim.x, 0, aim.z).add(offset);
                for (int i = 0; i < 3; i++)
                    spawn(
                            level,
                            owner,
                            Kind.BEAM,
                            start,
                            offset.normalize().scale(-.8),
                            12 + i * 8);
            }
            default -> {}
        }
    }

    private static void spawn(
            ServerLevel level,
            FinalitySummon owner,
            Kind kind,
            Vec3 pos,
            Vec3 velocity,
            int delay) {
        var e = FinalityContent.PROJECTILE.get().create(level);
        e.owner = owner;
        e.setPos(pos);
        e.entityData.set(KIND, kind.ordinal());
        e.entityData.set(START, level.getGameTime());
        e.entityData.set(ORIGIN, pos.toVector3f());
        e.entityData.set(VELOCITY, velocity.toVector3f());
        e.entityData.set(DELAY, delay);
        if (level.addFreshEntity(e)) FinalityTrials.get(level).attacks.add(e);
    }

    @Override
    public void tick() {
        // These effects have no fluid, fire, portal or block-inside behaviour.
        xo = getX();
        yo = getY();
        zo = getZ();
        tickCount++;
        float age = age(0);
        Vec3 previous = at(age - 1), next = at(age);
        setPos(next);
        if (!(level() instanceof ServerLevel level)) return;
        var trial = FinalityTrials.get(level);
        if (!trial.active()) {
            discard();
            return;
        }
        if (age < delay() && kind() != Kind.SWORD) return;
        AABB bounds =
                kind() == Kind.BEAM
                        ? new AABB(previous.x, 0, previous.z, next.x, 60, next.z).inflate(1, 0, 1)
                        : new AABB(previous, next).inflate(.3);
        for (var player : trial.players()) {
            if (hit.contains(player.getUUID()) || !bounds.intersects(player.getBoundingBox()))
                continue;
            boolean collision =
                    kind() == Kind.BEAM
                            ? beamHits(player, previous, next)
                            : player.getBoundingBox().inflate(.3).contains(previous)
                                    || player.getBoundingBox()
                                            .inflate(.3)
                                            .clip(previous, next)
                                            .isPresent();
            if (!collision) continue;
            hit.add(player.getUUID());
            trial.hit(player, this);
            if (kind() != Kind.BEAM || !trial.active()) {
                discard();
                return;
            }
        }
        boolean expired = kind() == Kind.STONE ? next.y <= 0 : age - delay() >= 20;
        if (expired
                || kind() != Kind.BEAM && next.x * next.x + next.z * next.z > 42 * 42
                || next.y < 0
                || next.y > 60) {
            if (kind() == Kind.STONE && next.y <= 0) {
                level.playSound(
                        null,
                        next.x,
                        0,
                        next.z,
                        SoundEvents.STONE_BREAK,
                        SoundSource.HOSTILE,
                        1,
                        .8F + random.nextFloat() * .2F);
                level.broadcastEntityEvent(this, (byte) 4);
                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        next.x,
                        .1,
                        next.z,
                        6,
                        .3,
                        .1,
                        .3,
                        .02);
                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        next.x,
                        .1,
                        next.z,
                        5,
                        .3,
                        .05,
                        .3,
                        .015);
            }
            discard();
        }
    }

    private static boolean beamHits(ServerPlayer player, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from), offset = player.position().subtract(from);
        double length = delta.horizontalDistanceSqr();
        double t =
                length == 0
                        ? 0
                        : Math.clamp((offset.x * delta.x + offset.z * delta.z) / length, 0, 1);
        Vec3 nearest = from.add(delta.scale(t));
        double dx = nearest.x - player.getX(),
                dz = nearest.z - player.getZ(),
                radius = 1 + player.getBbWidth() / 2;
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 4) org.lyy.lyycore.client.finality.FinalityClient.impact(position());
        else super.handleEntityEvent(event);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
