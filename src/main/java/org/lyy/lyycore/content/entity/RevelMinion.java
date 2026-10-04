package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Persistent ownership lets the boss manage its own summons without scanning the world. */
public abstract class RevelMinion extends RevelMob {
    private UUID bossId;
    private int missingBossTicks;
    private boolean reportedRemoval;

    protected RevelMinion(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 5;
    }
    public final void bind(LifeRevel boss) { bossId = boss.getUUID(); }
    public final LifeRevel boss() {
        return bossId != null && level() instanceof ServerLevel server && server.getEntity(bossId) instanceof LifeRevel boss ? boss : null;
    }
    @Override protected final void customServerAiStep() {
        LifeRevel boss = boss();
        if (boss == null) {
            var summons = RevelSummons.get((ServerLevel) level());
            if (summons.ended(bossId)) {
                if (summons.killed(bossId)) kill(); else discard();
                return;
            }
            // A known owner can remain unloaded indefinitely without killing its summons.
            if (summons.active(bossId)) { missingBossTicks = 0; setDeltaMovement(Vec3.ZERO); return; }
            // Chunk loading may recreate the minion before its owner. Do not destroy it on that first tick.
            if (++missingBossTicks >= 200) discard();
            return;
        }
        missingBossTicks = 0;
        if (!boss.isAlive()) { kill(); return; }
        boss.minionAvailable(this);
        updateWithBoss(boss);
    }
    protected abstract void updateWithBoss(LifeRevel boss);

    @Override public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91));
    }
    protected final void face(Vec3 point) {
        Vec3 direction = point.subtract(getEyePosition());
        setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        yBodyRot = yHeadRot = getYRot();
    }
    private void reportRemoval(boolean died) {
        if (reportedRemoval || level().isClientSide) return;
        reportedRemoval = true;
        LifeRevel boss = boss();
        RevelSummons.get((ServerLevel) level()).removed(bossId, getUUID(), died, boss == null);
        if (boss != null) boss.minionRemoved(this, died);
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (dead) reportRemoval(true);
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() || reason == RemovalReason.CHANGED_DIMENSION) reportRemoval(false);
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (bossId != null) tag.putUUID("RevelBoss", bossId);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bossId = tag.hasUUID("RevelBoss") ? tag.getUUID("RevelBoss") : null;
    }
}
