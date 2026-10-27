package org.lyy.lyycore.content.trial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.SummoningReservations;
import org.lyy.lyycore.content.entity.AnimatedMonster;
import org.lyy.lyycore.registry.LyyItems;

/** A stationary trial conductor; all hit geometry belongs to its spawned attacks. */
public final class TrialGuide extends AnimatedMonster {
    private static final double BASE_HEALTH = 200_000;
    private static final EntityDataAccessor<BlockPos> ALTAR = SynchedEntityData.defineId(TrialGuide.class, EntityDataSerializers.BLOCK_POS);
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private long nextAlternating, nextBarrage, nextThreads, ultimateStarted = -1;
    private boolean spikesNext, ultimateUsed;
    private int ultimateRounds;

    public TrialGuide(EntityType<? extends TrialGuide> type, Level level) {
        super(type, level);
        setNoGravity(true); setPersistenceRequired(); xpReward = 20000;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(ALTAR, BlockPos.ZERO);
    }
    public static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 100).add(Attributes.ARMOR, 0).add(Attributes.ARMOR_TOUGHNESS, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.MOVEMENT_SPEED, 0);
    }
    @Override public double getAttributeValue(Holder<Attribute> attribute) {
        // This trial fixes maximum health from its initial roster. AttributeFix's default
        // one-million cap would truncate larger multiplayer trials; do not change other entities' caps.
        return attribute.equals(Attributes.MAX_HEALTH) ? getAttributeBaseValue(attribute) : super.getAttributeValue(attribute);
    }
    @Override protected void registerGoals() { }
    public void begin(BlockPos altar, int players, float yaw) {
        entityData.set(ALTAR, altar.immutable());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH * (1 + .75 * (players - 1)));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(100 * (1 + .25 * (players - 1)));
        setHealth(getMaxHealth());
        var pos = anchor();
        moveTo(pos.x, pos.y, pos.z, yaw, 0); yBodyRot = yBodyRotO = yaw;
        restartAnimation("idle");
        updateBody();
        resetSchedule();
    }
    public Vec3 anchor() { return Vec3.atBottomCenterOf(entityData.get(ALTAR).above(3)); }
    public TrialGuidePose pose(float partial) {
        return TrialGuidePose.sample(animation(), animationTime(partial), net.minecraft.util.Mth.rotLerp(partial, yBodyRotO, yBodyRot));
    }
    private void updateBody() {
        var bounds = TrialGuidePose.sample(animation(), animationTime(0), yBodyRot).worldBounds(anchor(), yBodyRot);
        var center = bounds.getCenter();
        setPos(center.x, bounds.minY, center.z); setBoundingBox(bounds);
    }
    private void resetSchedule() {
        long now = level().getGameTime();
        nextAlternating = now + 30; nextBarrage = now + 80; nextThreads = now + 240;
    }
    public boolean departing() { return animation().equals("fold") || animation().equals("depart"); }
    public void depart() {
        if (departing()) return;
        // Departure starts from the closed cube; finish folding if the ultimate has spread the shell.
        restartAnimation(animation().equals("execution") ? "fold" : "depart");
        bar.removeAllPlayers();
    }
    @Override public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        updateBody();
        if (!(level() instanceof ServerLevel level)) return;
        if (departing()) {
            if (animation().equals("fold")) {
                if (animationTime(0) >= 1.2F) restartAnimation("depart");
            } else if (animationTime(0) >= 3.2F) remove(RemovalReason.DISCARDED);
            return;
        }
        var battle = SpacetimeTrials.get(level).battle(getUUID());
        if (battle == null) { depart(); return; } // Loaded after a restart: never resume or award loot.
        if (!isAlive()) return;
        long now = level.getGameTime();
        if (!ultimateUsed && getHealth() < getMaxHealth() * .35F) {
            ultimateUsed = true; ultimateStarted = now; ultimateRounds = 0; restartAnimation("execution");
        }
        if (tickCount % 20 == 0) heal(1000);
        bar.setProgress(getHealth() / getMaxHealth());
        if (ultimateStarted >= 0) {
            if (ultimateRounds < 3 && now >= ultimateStarted + ultimateRounds * 80L) {
                for (var kind : TrialAttack.Kind.values()) cast(level, battle, kind);
                ultimateRounds++;
            }
            if (animationTime(0) >= 12) animate("idle");
            if (ultimateRounds == 3 && battle.attacksFinished(now)) {
                ultimateStarted = -1; animate("idle"); resetSchedule();
            }
            return;
        }
        if (now >= nextAlternating) {
            cast(level, battle, spikesNext ? TrialAttack.Kind.SPIKE : TrialAttack.Kind.SHOT);
            spikesNext = !spikesNext; nextAlternating = now + 30;
        }
        if (now >= nextBarrage) { cast(level, battle, TrialAttack.Kind.BARRAGE); nextBarrage = now + 80; }
        if (now >= nextThreads) { cast(level, battle, TrialAttack.Kind.THREADS); nextThreads = now + 240; }
    }
    private void cast(ServerLevel level, SpacetimeTrials.Battle battle, TrialAttack.Kind kind) {
        var players = battle.players(level);
        if (players.isEmpty()) return;
        var target = players.get(random.nextInt(players.size()));
        var direction = target.position().subtract(position());
        setYRot((float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90));
        yBodyRot = getYRot(); setYHeadRot(getYRot());
        TrialAttack.cast(level, this, battle, target, kind, battle.aerial(level));
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (departing() || !SpecialDamage.isSpecial(source)) return false;
        float before = getHealth();
        boolean accepted = super.hurt(source, amount);
        if (accepted && isAlive() && getHealth() < before) {
            if (!ultimateUsed && getHealth() < getMaxHealth() * .35F && level() instanceof ServerLevel level) {
                var battle = SpacetimeTrials.get(level).battle(getUUID());
                if (battle != null) {
                    ultimateUsed = true; ultimateStarted = level.getGameTime(); ultimateRounds = 1;
                    restartAnimation("execution");
                    for (var kind : TrialAttack.Kind.values()) cast(level, battle, kind);
                }
            }
            heal(100);
        }
        return accepted;
    }
    @Override public void kill() {
        // Operator removal remains possible, but is never a successful trial.
        setHealth(0); die(damageSources().genericKill());
    }
    @Override public void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (SpacetimeTrials.get(level).end(level, getUUID(), SpecialDamage.isSpecial(source)))
            super.dropAllDeathLoot(level, source);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(LyyItems.SPACETIME_TRIAL_PROOF.get());
    }
    @Override protected boolean isAlwaysExperienceDropper() { return true; }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (dead && level() instanceof ServerLevel level) {
            SpacetimeTrials.get(level).end(level, getUUID(), false);
            depart();
        }
    }
    @Override protected void tickDeath() { deathTime++; } // The authored departure controls removal.
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); if (!departing()) bar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bar.removePlayer(player); }
    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            SpacetimeTrials.get(level).end(level, getUUID(), false);
            SummoningReservations.get(level).retire(getUUID());
        }
        bar.removeAllPlayers(); super.remove(reason);
    }
    @Override public void travel(Vec3 movement) { setDeltaMovement(Vec3.ZERO); }
    @Override public void move(MoverType type, Vec3 movement) { }
    @Override public void push(double x, double y, double z) { }
    @Override public void knockback(double strength, double x, double z) { }
    // Ordinary fire is filtered by hurt(); EntityType.fireImmune would also reject special fire damage.
    @Override public void setRemainingFireTicks(int ticks) { super.setRemainingFireTicks(0); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putLong("Altar", entityData.get(ALTAR).asLong()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(ALTAR, BlockPos.of(tag.getLong("Altar")));
        // Trial state intentionally does not survive a server restart.
        restartAnimation("depart");
    }
}
