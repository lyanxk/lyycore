package org.lyy.lyycore.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.EnderCompanions;
import org.lyy.lyycore.registry.LyyBlocks;
import java.util.Comparator;
import java.util.UUID;

public final class EnderCompanion extends PathfinderMob {
    private static final double ACQUIRE_RANGE = 5, CHASE_RANGE = 10;
    private static final float CHASE_MOVEMENT_MULTIPLIER = 3;
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(EnderCompanion.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(EnderCompanion.class, EntityDataSerializers.BOOLEAN);
    private UUID owner;
    private BlockPos sentry = BlockPos.ZERO;
    private int attackCooldown, wanderCooldown;
    public float walkTime, previousWalkTime, flightBlend, previousFlightBlend;

    public EnderCompanion(EntityType<? extends EnderCompanion> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 10);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAGE, 0);
        builder.define(FLYING, false);
    }
    public int stage() { return entityData.get(STAGE); }
    public boolean flying() { return entityData.get(FLYING); }
    public BlockPos sentry() { return sentry; }
    public void bind(ServerPlayer player, BlockPos pos) {
        owner = player.getUUID();
        sentry = pos.immutable();
        setGrowth(EnderCompanions.age(player));
    }
    public void setGrowth(int age) {
        int stage = age >= EnderCompanions.MAX_AGE ? 2 : age >= 5 * EnderCompanions.DAY_TICKS ? 1 : 0;
        if (stage == stage()) return;
        entityData.set(STAGE, stage);
        if (stage == 0) setFlying(false);
        wanderCooldown = 0;
    }
    private void setFlying(boolean flying) {
        if (flying == flying()) return;
        navigation.stop();
        entityData.set(FLYING, flying);
        setNoGravity(flying);
        if (flying) {
            moveControl = new FlyingMoveControl(this, 15, true);
            var flight = new FlyingPathNavigation(this, level());
            flight.setCanFloat(true);
            flight.setCanPassDoors(true);
            navigation = flight;
        } else {
            moveControl = new MoveControl(this);
            navigation = new GroundPathNavigation(this, level());
        }
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        return stage() == 0 ? EntityDimensions.scalable(0.7F, 0.8F) : EntityDimensions.scalable(1.1F, 1.35F);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAGE.equals(key)) refreshDimensions();
    }
    @Override public void tick() {
        if (level() instanceof ServerLevel level) {
            ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
            if (player == null || !player.isAlive() || player.level() != level || !EnderCompanions.ownsActive(player, this)) {
                discard();
                return;
            }
            if (tickCount % 20 == 0 && level.hasChunkAt(sentry) && !level.getBlockState(sentry).is(LyyBlocks.ENDER_SENTRY.get())) {
                EnderCompanions.recall(player);
                return;
            }
            EnderCompanions.tick(player, this);
        }
        super.tick();
        if (level().isClientSide) {
            previousWalkTime = walkTime;
            double distance = Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
            // Asset walk cycles were authored at these ground speeds (blocks/second).
            walkTime += (float) (distance / (stage() == 0 ? 0.143 : 0.161));
            previousFlightBlend = flightBlend;
            flightBlend = Mth.clamp(flightBlend + (flying() ? 0.1F : -0.1F), 0, 1);
        }
    }
    private boolean validTarget(LivingEntity target, double range) {
        return target != null && target instanceof Enemy && target.isAlive() && !target.isInvulnerable()
                && target.level() == level() && target.distanceToSqr(sentry.getCenter()) <= range * range;
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) attackCooldown--;
        boolean withinChaseRange = distanceToSqr(sentry.getCenter()) <= CHASE_RANGE * CHASE_RANGE;
        if (getTarget() != null && (!withinChaseRange || !validTarget(getTarget(), CHASE_RANGE))) {
            setTarget(null);
            navigation.stop();
            wanderCooldown = 0;
        }
        // Only query the sentry's small neighbourhood, twice per second.
        // Keep an acquired target even after it leaves the initial five-block range.
        if (getTarget() == null && withinChaseRange && tickCount % 10 == 0) {
            setTarget(level().getEntitiesOfClass(LivingEntity.class, new AABB(sentry).inflate(ACQUIRE_RANGE),
                            candidate -> validTarget(candidate, ACQUIRE_RANGE)).stream()
                    .filter(this::hasLineOfSight).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
        }
        LivingEntity target = getTarget();
        if (target != null) {
            if (stage() > 0) setFlying(true);
            getLookControl().setLookAt(target, 30, 30);
            if (tickCount % 10 == 0) navigation.moveTo(target, 1.2);
            double reach = 1.0 + (getBbWidth() + target.getBbWidth()) / 2;
            if (attackCooldown == 0 && distanceToSqr(target) <= reach * reach && hasLineOfSight(target)) {
                target.hurt(damageSources().mobAttack(this), stage() == 0 ? 5 : 10);
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                attackCooldown = 20;
            }
            return;
        }
        if (--wanderCooldown > 0) return;
        wanderCooldown = 60 + random.nextInt(60);
        boolean fly = stage() > 0 && random.nextInt(4) != 0;
        setFlying(fly);
        // Return toward the sentry after a chase; only hover while already near home.
        if (fly && distanceToSqr(sentry.getCenter()) <= ACQUIRE_RANGE * ACQUIRE_RANGE
                && random.nextInt(3) == 0) { navigation.stop(); return; }
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = random.nextDouble() * 3;
        BlockPos destination = sentry.offset((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
        if (!level().hasChunkAt(destination)) return;
        if (fly) destination = destination.above(1 + random.nextInt(3));
        else {
            destination = destination.above(3);
            while (destination.getY() > sentry.getY() - 3 && level().getBlockState(destination.below()).getCollisionShape(level(), destination.below()).isEmpty())
                destination = destination.below();
        }
        navigation.moveTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0.65);
    }
    @Override public void travel(Vec3 input) {
        float chaseMultiplier = getTarget() != null ? CHASE_MOVEMENT_MULTIPLIER : 1;
        // Scale movement once: ground navigation speed affects both input and acceleration.
        if (!flying()) { super.travel(input.scale(chaseMultiplier)); return; }
        // Air movement uses the controller's acceleration and a stable drag; gravity is disabled.
        if (isControlledByLocalInstance()) {
            moveRelative(0.08F * chaseMultiplier, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.8));
        }
        calculateEntityAnimation(false);
    }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("Sentry", sentry.asLong());
        tag.putInt("Stage", stage());
        tag.putBoolean("Flying", flying());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        sentry = BlockPos.of(tag.getLong("Sentry"));
        entityData.set(STAGE, Math.clamp(tag.getInt("Stage"), 0, 2));
        setFlying(stage() > 0 && tag.getBoolean("Flying"));
    }
}
