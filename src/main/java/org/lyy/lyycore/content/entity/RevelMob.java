package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** The supplied meshes share animation playback, without per-frame network updates. */
public abstract class RevelMob extends Monster {
    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(RevelMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> ANIMATION_START = SynchedEntityData.defineId(RevelMob.class, EntityDataSerializers.LONG);
    private int animationTicks;

    protected RevelMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION, "idle");
        builder.define(ANIMATION_START, 0L);
    }
    public final String animation() { return entityData.get(ANIMATION); }
    public final float animationTime(float partial) { return (level().getGameTime() - entityData.get(ANIMATION_START) + partial) / 20F; }
    protected final void animate(String name, int ticks) {
        entityData.set(ANIMATION, name);
        entityData.set(ANIMATION_START, level().getGameTime());
        animationTicks = ticks;
    }
    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && animationTicks > 0 && --animationTicks == 0) animationFinished(animation());
    }
    protected void animationFinished(String name) { animate("idle", 0); }
    @Override protected boolean isAlwaysExperienceDropper() { return true; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Animation", animation());
        tag.putLong("AnimationStart", entityData.get(ANIMATION_START));
        tag.putInt("AnimationTicks", animationTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Animation")) entityData.set(ANIMATION, tag.getString("Animation"));
        entityData.set(ANIMATION_START, tag.getLong("AnimationStart"));
        animationTicks = Math.max(0, tag.getInt("AnimationTicks"));
    }
}
