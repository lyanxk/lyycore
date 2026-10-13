package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EmberfallRing extends Entity {
    private static final EntityDataAccessor<Long> CREATED = SynchedEntityData.defineId(EmberfallRing.class,EntityDataSerializers.LONG);
    public EmberfallRing(EntityType<? extends EmberfallRing> type, Level level) { super(type,level); noPhysics=true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(CREATED,0L); }
    public void prepare(Vec3 pos) { setPos(pos); entityData.set(CREATED,level().getGameTime()); }
    public float age(float partial) { return level().getGameTime()-entityData.get(CREATED)+partial; }
    @Override public void tick() {
        super.tick(); if (level().isClientSide) return;
        if (age(0) >= 18) discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
}
