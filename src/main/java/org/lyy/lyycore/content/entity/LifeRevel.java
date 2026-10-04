package org.lyy.lyycore.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lyy.lyycore.registry.LyyEntities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LifeRevel extends RevelMob {
    public static final int LIFETIME = 180 * 20, DANCER_INTERVAL = 20 * 20, BLIND_INTERVAL = 45 * 20;
    public static final int CHARGE_TICKS = 40, LOCK_TICKS = 10;
    public static final double RANGE = 10, LASER_RADIUS = 0.21;
    private static final EntityDataAccessor<Vector3f> BEAM = SynchedEntityData.defineId(LifeRevel.class, EntityDataSerializers.VECTOR3);
    private final Set<UUID> dancers = new HashSet<>(), blinds = new HashSet<>();
    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
    private int lifetime, dancerTicks = DANCER_INTERVAL, blindTicks = -1, dancerDeaths, pendingLasers;
    private int laserTicks, recoveryTicks;
    private boolean initialized, cleaningUp;
    private Vec3 lockedAim;

    public LifeRevel(EntityType<? extends LifeRevel> type, Level level) {
        super(type, level);
        xpReward = 500;
    }
    public static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 400).add(Attributes.ARMOR, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, RANGE);
    }
    @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM, new Vector3f(0, 0, 10));
    }
    public void beginSummoning(Player player) { if (validTarget(player)) setTarget(player); }
    public Vec3 laserOrigin() { return position().add(0, 2.05, 0); }
    public Vec3 beam() { return new Vec3(entityData.get(BEAM)); }
    public Set<UUID> dancers() { return Set.copyOf(dancers); }
    public Set<UUID> blinds() { return Set.copyOf(blinds); }
    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel server) RevelSummons.get(server).register(getUUID(), dancers, blinds);
    }
    @Override protected void animationFinished(String name) {
        if (name.equals("summon")) animate("summon_recover", 16);
        else super.animationFinished(name);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) return;
        if (++lifetime >= LIFETIME) { discard(); return; }
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0) bossBar.setName(Component.translatable("boss.lyycore.life_revel.time",
                getDisplayName(), (LIFETIME - lifetime + 19) / 20));
    }
    @Override protected void customServerAiStep() {
        if (!initialized) {
            initialized = true;
            animate("summon", 68);
            summonBlinds();
            summonDancers();
        }
        if (!validTarget(getTarget())) setTarget(null);
        if (getTarget() == null && tickCount % 10 == 0) {
            Player nearest = level().getNearestPlayer(getX(), getY(), getZ(), RANGE, entity -> entity instanceof Player p && validTarget(p));
            setTarget(nearest);
        }
        updateSummons();
        updateLaser();
        LivingEntity target = getTarget();
        if (target != null) getLookControl().setLookAt(target, 30, 30);
        if (target == null || laserTicks > 0 || recoveryTicks > 18 || animation().equals("summon")) getNavigation().stop();
        else if (tickCount % 10 == 0) {
            if (distanceToSqr(target) > 9) getNavigation().moveTo(target, 1);
            else getNavigation().stop();
        }
    }
    private boolean validTarget(LivingEntity target) {
        return target != null && target.isAlive() && distanceToSqr(target) <= RANGE * RANGE && canAttack(target)
                && !(target instanceof RevelMob) && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }
    private void updateSummons() {
        // A missing loaded entity is not proof of death. Consume explicit lifecycle notifications only.
        if (tickCount % 20 == 0) reconcileSummons();
        if (--dancerTicks <= 0) {
            dancerTicks = DANCER_INTERVAL;
            if (dancers.size() < 8) summonDancers();
        }
        if (blinds.isEmpty() && blindTicks >= 0 && --blindTicks <= 0) summonBlinds();
    }
    private void reconcileSummons() {
        RevelSummons.get((ServerLevel) level()).drain(getUUID()).forEach(this::minionRemoved);
    }
    void minionAvailable(RevelMinion minion) {
        if (cleaningUp || !isAlive()) return;
        // Reattach survivors from older saves whose owner previously forgot unloaded UUIDs.
        if (minion instanceof RevelDancer) dancers.add(minion.getUUID());
        else if (minion instanceof RevelBlind) blinds.add(minion.getUUID());
        RevelSummons.get((ServerLevel) level()).track(getUUID(), minion.getUUID());
    }
    private void summonBlinds() {
        for (int corner = 0; corner < 4; corner++) {
            var blind = LyyEntities.REVEL_BLIND.get().create(level());
            if (blind == null) continue;
            blind.beginSummoning(corner);
            if (addMinion(blind, RevelBlind.formationOffset(corner))) blinds.add(blind.getUUID());
        }
        blindTicks = blinds.isEmpty() ? BLIND_INTERVAL : -1;
    }
    private void summonDancers() {
        for (int i = 0; i < 4; i++) {
            var dancer = LyyEntities.REVEL_DANCER.get().create(level());
            // Alternate with the blinds' diagonal formation so the opening wave does not overlap them.
            double angle = i * Math.PI / 2;
            Vec3 offset = new Vec3(Math.cos(angle) * 3, 0.6, Math.sin(angle) * 3);
            if (dancer != null && addMinion(dancer, offset)) dancers.add(dancer.getUUID());
        }
        if (canPlayIdleAction()) animate("summon_strikers", 22);
    }
    private boolean addMinion(RevelMinion minion, Vec3 offset) {
        minion.bind(this);
        Vec3 desired = position().add(offset);
        for (int height = 0; height < 4; height++) {
            minion.setPos(desired.add(0, height, 0));
            if (level().hasChunkAt(BlockPos.containing(minion.position())) && level().noCollision(minion)
                    && level().addFreshEntity(minion)) {
                minionAvailable(minion);
                return true;
            }
        }
        return false;
    }
    public void minionRemoved(RevelMinion minion, boolean died) {
        minionRemoved(minion.getUUID(), died);
    }
    private void minionRemoved(UUID id, boolean died) {
        boolean dancer = dancers.remove(id);
        boolean blind = blinds.remove(id);
        if (cleaningUp || !isAlive()) return;
        if (dancer && died) {
            if (++dancerDeaths == 3) { dancerDeaths = 0; pendingLasers++; }
            if (dancers.isEmpty()) dancerTicks = Math.max(0, dancerTicks - 100);
        }
        if (blind && blinds.isEmpty()) blindTicks = BLIND_INTERVAL;
    }
    private boolean canPlayIdleAction() { return laserTicks == 0 && recoveryTicks == 0 && animation().equals("idle"); }
    public void showRegeneration() { if (canPlayIdleAction()) animate("regeneration", 48); }

    private void aimAt(Vec3 target) {
        Vec3 direction = target.subtract(laserOrigin()).normalize();
        if (direction.lengthSqr() < 0.001) direction = getLookAngle();
        entityData.set(BEAM, direction.scale(RANGE).toVector3f());
        setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        yBodyRot = yHeadRot = getYRot();
    }
    private void updateLaser() {
        if (recoveryTicks > 0) {
            if (--recoveryTicks == 18) animate("laser_recover", 18);
            return;
        }
        if (laserTicks == 0) {
            if (pendingLasers == 0 || !validTarget(getTarget()) || animation().equals("summon")) return;
            pendingLasers--;
            laserTicks = CHARGE_TICKS + LOCK_TICKS;
            lockedAim = null;
            animate("laser_charge", laserTicks);
            playSound(SoundEvents.BEACON_POWER_SELECT, 1.5F, 1.5F);
            aimAt(getTarget().getBoundingBox().getCenter());
            return;
        }
        if (laserTicks > LOCK_TICKS) {
            if (!validTarget(getTarget())) { laserTicks = 0; animate("idle", 0); return; }
            aimAt(getTarget().getBoundingBox().getCenter());
            if (laserTicks == LOCK_TICKS + 1) lockedAim = getTarget().getBoundingBox().getCenter();
        } else if (lockedAim != null) aimAt(lockedAim);
        if (--laserTicks == 0) {
            fireLaser();
            animate("laser_fire", 24);
            recoveryTicks = 42;
        }
    }
    private void fireLaser() {
        Vec3 start = laserOrigin(), end = start.add(beam());
        DamageSource source = damageSources().indirectMagic(this, this);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(LASER_RADIUS),
                entity -> entity != this && entity.isAlive() && !entity.isInvulnerableTo(source))) {
            AABB bounds = target.getBoundingBox().inflate(LASER_RADIUS);
            // Deliberately do not raycast blocks: this attack passes through walls.
            if (bounds.contains(start) || bounds.clip(start, end).isPresent()) target.hurt(source, 18);
        }
        playSound(SoundEvents.GUARDIAN_ATTACK, 2, 0.6F);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && source.getEntity() instanceof LivingEntity attacker && validTarget(attacker)) setTarget(attacker);
        return damaged;
    }
    private void clearMinions(boolean killed) {
        if (cleaningUp || !(level() instanceof ServerLevel server)) return;
        cleaningUp = true;
        RevelSummons.get(server).end(getUUID(), killed);
        for (Set<UUID> group : List.of(dancers, blinds)) for (UUID id : List.copyOf(group)) {
            if (server.getEntity(id) instanceof RevelMinion minion) {
                if (killed && minion.isAlive()) minion.kill(); else minion.discard();
            }
        }
        dancers.clear();
        blinds.clear();
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (dead) clearMinions(true);
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) clearMinions(false);
        bossBar.removeAllPlayers();
        super.remove(reason);
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }

    private static ListTag saveIds(Set<UUID> ids) {
        ListTag result = new ListTag();
        for (UUID id : ids) result.add(NbtUtils.createUUID(id));
        return result;
    }
    private static void loadIds(CompoundTag tag, String key, Set<UUID> ids) {
        ids.clear();
        for (Tag id : tag.getList(key, Tag.TAG_INT_ARRAY)) ids.add(NbtUtils.loadUUID(id));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Dancers", saveIds(dancers)); tag.put("Blinds", saveIds(blinds));
        tag.putInt("Lifetime", lifetime); tag.putBoolean("Initialized", initialized);
        tag.putInt("DancerTicks", dancerTicks); tag.putInt("BlindTicks", blindTicks);
        tag.putInt("DancerDeaths", dancerDeaths); tag.putInt("PendingLasers", pendingLasers);
        tag.putInt("LaserTicks", laserTicks); tag.putInt("RecoveryTicks", recoveryTicks);
        if (lockedAim != null) tag.put("LockedAim", newDoubleList(lockedAim.x, lockedAim.y, lockedAim.z));
        Vec3 beam = beam(); tag.put("Beam", newDoubleList(beam.x, beam.y, beam.z));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadIds(tag, "Dancers", dancers); loadIds(tag, "Blinds", blinds);
        lifetime = Math.clamp(tag.getInt("Lifetime"), 0, LIFETIME);
        initialized = tag.getBoolean("Initialized");
        dancerTicks = tag.contains("DancerTicks") ? Math.clamp(tag.getInt("DancerTicks"), 0, DANCER_INTERVAL) : DANCER_INTERVAL;
        blindTicks = Math.clamp(tag.getInt("BlindTicks"), -1, BLIND_INTERVAL);
        dancerDeaths = Math.clamp(tag.getInt("DancerDeaths"), 0, 2);
        pendingLasers = Math.max(0, tag.getInt("PendingLasers"));
        laserTicks = Math.clamp(tag.getInt("LaserTicks"), 0, CHARGE_TICKS + LOCK_TICKS);
        recoveryTicks = Math.clamp(tag.getInt("RecoveryTicks"), 0, 42);
        var aim = tag.getList("LockedAim", Tag.TAG_DOUBLE);
        lockedAim = aim.size() == 3 ? new Vec3(aim.getDouble(0), aim.getDouble(1), aim.getDouble(2)) : null;
        var beam = tag.getList("Beam", Tag.TAG_DOUBLE);
        if (beam.size() == 3) entityData.set(BEAM, new Vector3f((float) beam.getDouble(0), (float) beam.getDouble(1), (float) beam.getDouble(2)));
    }
}
