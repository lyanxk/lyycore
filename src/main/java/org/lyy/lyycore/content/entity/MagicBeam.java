package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lyy.lyycore.registry.LyyEntities;

/** Short-lived visual for already resolved ray attacks; damage stays on the server. */
public final class MagicBeam extends Entity {
    private static final EntityDataAccessor<Vector3f> DELTA = SynchedEntityData.defineId(MagicBeam.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> RIFLE = SynchedEntityData.defineId(MagicBeam.class, EntityDataSerializers.BOOLEAN);
    public MagicBeam(EntityType<? extends MagicBeam> type, Level level) { super(type, level); }
    public static void show(ServerLevel level, Vec3 start, Vec3 end) {
        show(level, start, end, false);
    }
    public static void show(ServerLevel level, Vec3 start, Vec3 end, boolean rifle) {
        var beam = LyyEntities.MAGIC_BEAM.get().create(level);
        if (beam == null) return;
        beam.setPos(start); beam.entityData.set(DELTA, end.subtract(start).toVector3f());
        beam.entityData.set(RIFLE, rifle); level.addFreshEntity(beam);
    }
    public Vec3 delta() { return new Vec3(entityData.get(DELTA)); }
    public boolean rifle() { return entityData.get(RIFLE); }
    public int lifetime() { return rifle() ? 8 : 6; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DELTA, new Vector3f()); builder.define(RIFLE, false);
    }
    @Override public void tick() { super.tick(); if (!level().isClientSide && tickCount >= lifetime()) discard(); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
