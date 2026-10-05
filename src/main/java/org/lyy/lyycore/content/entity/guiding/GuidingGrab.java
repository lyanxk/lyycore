package org.lyy.lyycore.content.entity.guiding;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEntities;

/** Server-owned capture and its persistent, synchronized rope presentation. */
public final class GuidingGrab extends Entity {
    public enum Phase { EXTEND, WRAP, PULL, BOUND, RELEASE }
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(GuidingGrab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(GuidingGrab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GuidingGrab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> STARTED = SynchedEntityData.defineId(GuidingGrab.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> RELEASE_FROM = SynchedEntityData.defineId(GuidingGrab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RELEASE_TIME = SynchedEntityData.defineId(GuidingGrab.class, EntityDataSerializers.FLOAT);
    private UUID bossId, targetId;
    private boolean fast;
    private int age;

    public GuidingGrab(EntityType<? extends GuidingGrab> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1); builder.define(TARGET, -1); builder.define(PHASE, Phase.EXTEND.ordinal());
        builder.define(STARTED, 0L); builder.define(RELEASE_FROM, Phase.EXTEND.ordinal()); builder.define(RELEASE_TIME, 0F);
    }
    public Phase phase() { return Phase.values()[entityData.get(PHASE)]; }
    public Phase releasedFrom() { return Phase.values()[entityData.get(RELEASE_FROM)]; }
    public float releaseTime() { return entityData.get(RELEASE_TIME); }
    public Entity owner() { return level().getEntity(entityData.get(OWNER)); }
    public Entity target() { return level().getEntity(entityData.get(TARGET)); }
    public float phaseSeconds(float partial) { return Math.max(0, (level().getGameTime() - entityData.get(STARTED) + partial) / 20F); }
    private void phase(Phase phase) {
        if (phase() == phase) return;
        entityData.set(PHASE, phase.ordinal()); entityData.set(STARTED, level().getGameTime());
    }
    /** Null means that no projectile was admitted to the world. */
    public static UUID launch(GuidingBoss boss, Player target, boolean fast) {
        var grab = LyyEntities.GUIDING_GRAB.get().create(boss.level());
        if (grab == null) return null;
        grab.bossId = boss.getUUID(); grab.targetId = target.getUUID(); grab.fast = fast;
        grab.entityData.set(OWNER, boss.getId()); grab.entityData.set(TARGET, target.getId());
        grab.entityData.set(STARTED, boss.level().getGameTime());
        grab.setPos(boss.laserOrigin());
        return boss.level().addFreshEntity(grab) ? grab.getUUID() : null;
    }
    private void release() {
        entityData.set(RELEASE_FROM, phase().ordinal()); entityData.set(RELEASE_TIME, phaseSeconds(0));
        phase(Phase.RELEASE);
        if (bossId != null && level() instanceof ServerLevel server
                && server.getEntity(bossId) instanceof GuidingBoss boss) boss.grabMissed(getUUID());
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (phase() == Phase.RELEASE) { if (phaseSeconds(0) >= .2F) discard(); return; }
        if (phase() == Phase.EXTEND && bossId != null && server.getEntity(bossId) instanceof GuidingBoss owner
                && !owner.maintainsGrab(getUUID())) { discard(); return; }
        if (bossId == null || targetId == null || !(server.getEntity(bossId) instanceof GuidingBoss boss)
                || !boss.isAlive() || !boss.maintainsGrab(getUUID())
                || !(server.getEntity(targetId) instanceof Player target) || !target.isAlive()
                || target.isCreative() || target.isSpectator()) { release(); return; }
        entityData.set(OWNER, boss.getId()); entityData.set(TARGET, target.getId());
        if (phase() != Phase.EXTEND) {
            setPos(target.getBoundingBox().getCenter());
            phase(boss.wrapping() ? Phase.WRAP : boss.action() == GuidingBoss.Action.DEVOUR ? Phase.BOUND : Phase.PULL);
            return;
        }
        Vec3 start = position(), offset = target.getBoundingBox().getCenter().subtract(start);
        // Speeds are specified in blocks/second; entity movement runs at 20 ticks/second.
        Vec3 end = start.add(offset.normalize().scale(Math.min(offset.length(), (fast ? 10.0 : 3.0) / 20.0)));
        var bounds = target.getBoundingBox().inflate(.3);
        setPos(end);
        if (bounds.contains(start) || bounds.clip(start, end).isPresent()) {
            boss.captured(target);
            if (boss.pulling(target)) { setPos(target.getBoundingBox().getCenter()); phase(Phase.WRAP); }
            else release();
            return;
        }
        if (!fast && ++age >= 200) release();
    }
    @Override public void remove(RemovalReason reason) {
        // Permanent removal releases the action; a normal unload retains the grace period.
        if (reason.shouldDestroy() && bossId != null && level() instanceof ServerLevel server
                && server.getEntity(bossId) instanceof GuidingBoss boss) boss.grabMissed(getUUID());
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (bossId != null) tag.putUUID("Boss", bossId);
        if (targetId != null) tag.putUUID("Target", targetId);
        tag.putBoolean("Fast", fast); tag.putInt("Age", age);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        bossId = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        fast = tag.getBoolean("Fast"); age = tag.getInt("Age");
        // Captures are transient and cannot resume after a chunk/server reload.
        entityData.set(PHASE, Phase.RELEASE.ordinal()); entityData.set(STARTED, level().getGameTime() - 4);
    }
}
