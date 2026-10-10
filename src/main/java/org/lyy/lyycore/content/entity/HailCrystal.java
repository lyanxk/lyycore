package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Cosmetic flight only; HailItem owns damage. It intentionally passes through blocks. */
public final class HailCrystal extends Entity {
    private static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(HailCrystal.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> CREATED = SynchedEntityData.defineId(HailCrystal.class, EntityDataSerializers.LONG);
    public static final int FLIGHT_TICKS = 6;
    public HailCrystal(EntityType<? extends HailCrystal> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(END, new Vector3f()); builder.define(CREATED, 0L);
    }
    public void prepare(Vec3 start, Vec3 end) {
        setPos(start); entityData.set(END, end.subtract(start).toVector3f()); entityData.set(CREATED, level().getGameTime());
    }
    public Vec3 start() { return position(); }
    public Vec3 end() { return position().add(new Vec3(entityData.get(END))); }
    public float age(float partial) { return level().getGameTime() - entityData.get(CREATED) + partial; }
    @Override public void tick() {
        super.tick();
        // Renderers interpolate from absolute start time, so delayed tracking never extends the flight.
        if (!level().isClientSide && age(0) >= FLIGHT_TICKS + 2) discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
}
