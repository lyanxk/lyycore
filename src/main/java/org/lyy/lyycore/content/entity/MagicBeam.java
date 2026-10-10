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
    public MagicBeam(EntityType<? extends MagicBeam> type, Level level) { super(type, level); }
    public static void show(ServerLevel level, Vec3 start, Vec3 end) {
        var beam = LyyEntities.MAGIC_BEAM.get().create(level);
        if (beam == null) return;
        beam.setPos(start); beam.entityData.set(DELTA, end.subtract(start).toVector3f()); level.addFreshEntity(beam);
    }
    public Vec3 delta() { return new Vec3(entityData.get(DELTA)); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(DELTA, new Vector3f()); }
    @Override public void tick() { super.tick(); if (!level().isClientSide && tickCount >= 6) discard(); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
