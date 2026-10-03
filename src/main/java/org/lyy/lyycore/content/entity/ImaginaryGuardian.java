package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEntities;

import java.util.Comparator;
import java.util.UUID;

public final class ImaginaryGuardian extends Monster {
    public static final int SUMMON_TICKS = 220;
    private static final int ATTACK_INTERVAL = 80;
    private static final int BARRAGE_INTERVAL = 40;
    private static final EntityDataAccessor<Integer> SUMMONING = SynchedEntityData.defineId(ImaginaryGuardian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPINNING = SynchedEntityData.defineId(ImaginaryGuardian.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
    private Vec3 anchor;
    private UUID summoner;
    private int attackTicks;
    private int barrageTicks;

    public ImaginaryGuardian(EntityType<? extends ImaginaryGuardian> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300).add(Attributes.ARMOR, 5)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 20);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SUMMONING, 0);
        builder.define(SPINNING, 0);
    }
    public int summonTicks() { return entityData.get(SUMMONING); }
    public int spinTicks() { return entityData.get(SPINNING); }
    public void beginSummoning(Player player) {
        summoner = player.getUUID();
        anchor = position();
        setHealth(1);
        entityData.set(SUMMONING, SUMMON_TICKS);
    }
    public boolean isParticipant(Player player) { return player.getUUID().equals(summoner) || getTarget() == player; }
    @Override public boolean isPushable() { return false; }
    @Override public void travel(Vec3 movement) { setDeltaMovement(Vec3.ZERO); }

    @Override public void aiStep() {
        super.aiStep();
        if (anchor == null) anchor = position();
        setPos(anchor);
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide || !isAlive()) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (summoner != null && level().getPlayerByUUID(summoner) instanceof Player player && !player.isAlive()) {
            discard();
            return;
        }
        if (summonTicks() > 0) {
            int remaining = summonTicks() - 1;
            entityData.set(SUMMONING, remaining);
            setHealth(Math.max(1, getMaxHealth() * (SUMMON_TICKS - remaining) / SUMMON_TICKS));
            if (remaining == 0) {
                entityData.set(SPINNING, 20);
                repelNearbyPlayers();
            }
            return;
        }
        if (spinTicks() > 0) entityData.set(SPINNING, spinTicks() - 1);
        LivingEntity target = getTarget();
        if (target instanceof Player player && !player.isAlive()) { discard(); return; }
        if (target != null && (!target.isAlive() || distanceToSqr(target) > 400
                || target instanceof Player player && (player.isCreative() || player.isSpectator()))) setTarget(null);
        if (getTarget() == null) {
            setTarget(level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6),
                    entity -> (entity instanceof Player || entity instanceof Enemy)
                            && !(entity instanceof ImaginaryGuardian) && entity.isAlive()
                            && canAttack(entity) && !isAlliedTo(entity) && distanceToSqr(entity) <= 36)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
        }
        target = getTarget();
        if (target == null) {
            attackTicks = 0;
            barrageTicks = 0;
            if (tickCount % 20 == 0) heal(10);
            return;
        }
        getLookControl().setLookAt(target, 360, 360);
        if (++barrageTicks >= BARRAGE_INTERVAL) {
            barrageTicks = 0;
            fireRadialBarrage();
        }
        if (++attackTicks >= ATTACK_INTERVAL) {
            attackTicks = 0;
            fireVolley(target);
            if (getHealth() < getMaxHealth() / 2) castSpikes(target.position());
        }
    }
    private void repelNearbyPlayers() {
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2, 0.7F);
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(6), p -> !p.isSpectator())) {
            Vec3 offset = player.position().subtract(position()).multiply(1, 0, 1);
            if (offset.lengthSqr() > 36) continue;
            Vec3 direction = offset.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : offset.normalize();
            double strength = Math.max(1.2, (7 - offset.length()) * 0.7);
            player.setDeltaMovement(direction.scale(strength).add(0, 0.8, 0));
            player.hurtMarked = true;
        }
    }
    private void fireVolley(LivingEntity target) {
        playSound(SoundEvents.AMETHYST_CLUSTER_PLACE, 2, 1.2F);
        for (int i = 0; i < 4; i++) {
            var crystal = LyyEntities.GUARDIAN_CRYSTAL.get().create(level());
            if (crystal == null) continue;
            double angle = i * Math.PI / 2;
            crystal.setOwner(this);
            crystal.setHomingTarget(target);
            crystal.setPos(getX() + Math.cos(angle), getY() + 1.8 + Math.sin(angle) * 0.6, getZ());
            Vec3 aim = target.getEyePosition().subtract(crystal.position());
            crystal.shoot(aim.x, aim.y, aim.z, 0.35F, 0);
            level().addFreshEntity(crystal);
        }
    }
    private void fireRadialBarrage() {
        playSound(SoundEvents.AMETHYST_CLUSTER_PLACE, 2, 1.2F);
        for (int i = 0; i < 8; i++) {
            var crystal = LyyEntities.GUARDIAN_CRYSTAL.get().create(level());
            if (crystal == null) continue;
            double angle = i * Math.PI / 4;
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            crystal.setOwner(this);
            crystal.setLifetimeTicks(40);
            crystal.setPos(getX() + x, getY() + 1.8, getZ() + z);
            crystal.shoot(x, 0, z, 0.35F, 0);
            level().addFreshEntity(crystal);
        }
    }
    private void castSpikes(Vec3 target) {
        Vec3 offset = target.subtract(position()).multiply(1, 0, 1);
        Vec3 direction = offset.normalize();
        int segments = Math.max(1, (int) Math.ceil(Math.min(20, offset.length()) / 1.5));
        for (int i = 0; i < segments; i++) {
            Vec3 point = position().add(direction.scale((i + 1) * 1.5));
            var spikes = LyyEntities.GUARDIAN_SPIKES.get().create(level());
            if (spikes == null) continue;
            var ground = net.minecraft.core.BlockPos.containing(point.x, target.y + 2, point.z);
            for (int search = 0; search < 6 && level().getBlockState(ground.below()).getCollisionShape(level(), ground.below()).isEmpty(); search++) ground = ground.below();
            spikes.configure(this, 20 + i * 4);
            spikes.moveTo(point.x, ground.getY(), point.z, (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)), 0);
            level().addFreshEntity(spikes);
        }
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (summonTicks() > 0) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this) setTarget(attacker);
        return hurt;
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
    @Override public void remove(RemovalReason reason) { bossBar.removeAllPlayers(); super.remove(reason); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Summoning", summonTicks());
        tag.putInt("AttackTicks", attackTicks);
        tag.putInt("BarrageTicks", barrageTicks);
        if (summoner != null) tag.putUUID("Summoner", summoner);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        anchor = position();
        entityData.set(SUMMONING, Math.clamp(tag.getInt("Summoning"), 0, SUMMON_TICKS));
        attackTicks = Math.clamp(tag.getInt("AttackTicks"), 0, ATTACK_INTERVAL - 1);
        barrageTicks = Math.clamp(tag.getInt("BarrageTicks"), 0, BARRAGE_INTERVAL - 1);
        summoner = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
    }
}
