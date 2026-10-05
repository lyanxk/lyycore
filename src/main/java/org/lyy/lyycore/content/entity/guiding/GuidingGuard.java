package org.lyy.lyycore.content.entity.guiding;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEntities;

/** Both guards share ownership and down/revive rules; only their attack style differs. */
public final class GuidingGuard extends GuidingMob {
    private static final EntityDataAccessor<Boolean> DOWN = SynchedEntityData.defineId(GuidingGuard.class, EntityDataSerializers.BOOLEAN);
    private UUID encounter;
    private int downTicks, conviction, shotDelay = 40;
    private boolean consumed;
    public GuidingGuard(EntityType<? extends GuidingGuard> type, Level level) { super(type, level); setPersistenceRequired(); xpReward = 0; }
    public boolean ranged() { return getType() == LyyEntities.FANATICAL_SUPPORTER.get(); }
    public static AttributeSupplier.Builder attributes(boolean ranged) {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, ranged ? 200 : 300)
                .add(Attributes.ATTACK_DAMAGE, 50).add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 20);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(DOWN, false); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, false) {
            @Override public boolean canUse() { return !ranged() && !down() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !ranged() && !down() && super.canContinueToUse(); }
        });
    }
    public void bind(UUID encounter) { this.encounter = encounter; }
    public boolean down() { return entityData.get(DOWN); }
    public int conviction() { return conviction; }
    private void standing(boolean value) {
        if (!(level() instanceof ServerLevel server) || encounter == null) return;
        var data = GuidingEncounter.get(server); var battle = data.battle(encounter);
        if (battle == null) return;
        if (value) battle.standing.add(getUUID()); else battle.standing.remove(getUUID());
        data.setDirty();
    }
    @Override public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        var data = GuidingEncounter.get(server); var battle = encounter == null ? null : data.battle(encounter);
        if (battle == null || battle.phase == GuidingEncounter.Phase.ENDED) { discard(); return; }
        if (battle.phase == GuidingEncounter.Phase.ABSORB || battle.phase == GuidingEncounter.Phase.DEMAND) {
            animate("absorbed");
            setNoAi(true);
            if (server.getEntity(battle.boss) instanceof GuidingBoss boss) {
                var offset = boss.getBoundingBox().getCenter().subtract(position());
                noPhysics = true; setNoGravity(true);
                setPos(position().add(offset.scale(.2)));
                if (offset.lengthSqr() < 1 || battle.phase == GuidingEncounter.Phase.DEMAND) consume();
            }
            return;
        }
        if (down()) {
            if (!playing("down", .75F)) animate("downed");
            getNavigation().stop(); setTarget(null);
            if (--downTicks <= 0) {
                entityData.set(DOWN, false); setNoAi(false); setHealth(getMaxHealth()); standing(true);
                restartAnimation("revive");
            }
            return;
        }
        if (!(server.getEntity(battle.boss) instanceof GuidingBoss boss)) { getNavigation().stop(); setTarget(null); return; }
        var target = boss.getTarget();
        setTarget(target instanceof Player && target.isAlive() ? target : null);
        if (!playing("revive", 1.05F) && !playing("attack", .72F) && !playing("conviction", .6F)
                && !playing("fire", .5F) && !(ranged() && getTarget() != null && (animation().equals("aim") || animation().equals("aim_locked")))) {
            if (animation().equals("attack") && conviction > 0) restartAnimation("conviction");
            else animate(getDeltaMovement().horizontalDistanceSqr() > .0001 ? (!ranged() && getTarget() != null ? "run" : "walk") : "idle");
        }
        if (getTarget() == null || !ranged()) return;
        getLookControl().setLookAt(target, 30, 30);
        // Skeleton-like approach and lateral movement, with independently timed aim lines.
        if (distanceToSqr(target) > 225) getNavigation().moveTo(target, 1);
        else { getNavigation().stop(); getMoveControl().strafe(-.15F, tickCount / 40 % 2 == 0 ? .35F : -.35F); }
        if (--shotDelay <= 0) { shotDelay = 40; GuidingLaser.fire(this, target, false); }
    }
    @Override public boolean doHurtTarget(Entity target) {
        if (down() || ranged() || !(target instanceof Player)) return false;
        restartAnimation("attack");
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            heal(20); conviction = Math.min(10, conviction + 1);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(50 + conviction * 5);
        }
        return hit;
    }
    @Override public boolean hurt(DamageSource source, float amount) { return !down() && super.hurt(source, amount); }
    @Override public void die(DamageSource source) {
        if (consumed) { super.die(source); return; }
        setHealth(1); downTicks = 1200; entityData.set(DOWN, true);
        restartAnimation("down");
        setNoAi(true); getNavigation().stop(); setTarget(null); standing(false);
    }
    public void consume() { consumed = true; animate("absorbed"); setHealth(0); super.die(damageSources().genericKill()); standing(false); }
    public Vec3 laserOrigin() { return position().add(new Vec3(0, 21 / 16.0, 20 / 16.0).yRot((float)Math.toRadians(-yBodyRot))); }
    void laserAnimation(String clip) {
        if (!down() && !animation().equals("absorbed") && !playing("revive", 1.05F)
                && (clip.equals("fire") || !playing("fire", .5F))) restartAnimation(clip);
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && encounter != null && level() instanceof ServerLevel server)
            GuidingEncounter.get(server).removeGuard(encounter, getUUID());
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (encounter != null) tag.putUUID("Encounter", encounter);
        tag.putInt("DownTicks", downTicks); tag.putInt("Conviction", conviction); tag.putInt("ShotDelay", shotDelay);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); encounter = tag.hasUUID("Encounter") ? tag.getUUID("Encounter") : null;
        downTicks = Math.clamp(tag.getInt("DownTicks"), 0, 1200); conviction = Math.clamp(tag.getInt("Conviction"), 0, 10);
        shotDelay = Math.clamp(tag.getInt("ShotDelay"), 0, 40); entityData.set(DOWN, downTicks > 0); setNoAi(downTicks > 0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(50 + conviction * 5);
    }
}
