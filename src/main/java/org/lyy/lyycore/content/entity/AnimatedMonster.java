package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Synchronizes animation transitions once; clients advance the supplied clips locally. */
public abstract class AnimatedMonster extends Monster {
    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(AnimatedMonster.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> STARTED = SynchedEntityData.defineId(AnimatedMonster.class, EntityDataSerializers.LONG);

    protected AnimatedMonster(EntityType<? extends AnimatedMonster> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION, "idle");
        builder.define(STARTED, 0L);
    }
    public String animation() { return entityData.get(ANIMATION); }
    public float animationTime(float partial) { return Math.max(0, (level().getGameTime() - entityData.get(STARTED) + partial) / 20F); }
    protected boolean playing(String clip, float duration) { return animation().equals(clip) && animationTime(0) < duration; }
    protected void animate(String clip) { if (!animation().equals(clip)) restartAnimation(clip); }
    protected void restartAnimation(String clip) {
        entityData.set(ANIMATION, clip);
        entityData.set(STARTED, level().getGameTime());
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Animation", animation());
        tag.putLong("AnimationStarted", entityData.get(STARTED));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Animation")) {
            entityData.set(ANIMATION, tag.getString("Animation"));
            entityData.set(STARTED, tag.getLong("AnimationStarted"));
        }
    }
}
