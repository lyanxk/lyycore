package org.lyy.lyycore.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The server fixes the endpoint; the owning client applies traction before vanilla travel. */
public final class FrostChainHook extends Projectile {
    public static final String ACTIVE_TAG = "FrostChain";
    public static final double SPEED = 60.0 / 20;
    public static final double REACH = 20;
    public static final double ACCELERATION = 128.0 / (20 * 20);
    private static final EntityDataAccessor<Integer> ATTACHED_AT = SynchedEntityData.defineId(FrostChainHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(FrostChainHook.class, EntityDataSerializers.INT);
    private double flownDistance;
    private BlockPos anchorBlock;
    private Vec3 previousCenter;

    public FrostChainHook(EntityType<? extends FrostChainHook> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ATTACHED_AT, -1);
        builder.define(FACE, -1);
    }

    public boolean attached() { return entityData.get(ATTACHED_AT) >= 0; }
    public float attachedAge(float partial) {
        return attached() ? level().getGameTime() - entityData.get(ATTACHED_AT) + partial : 0;
    }
    public Vec3 forward() {
        int face = entityData.get(FACE);
        // Projectile.shoot uses the opposite yaw/pitch signs to living-entity view angles.
        return face < 0 ? Vec3.directionFromRotation(-getXRot(), -getYRot())
                : Vec3.atLowerCornerOf(Direction.from3DDataValue(face).getNormal()).scale(-1);
    }
    public Vec3 pullGoal() {
        // Stop outside a solid face so the player's body need not enter the block.
        return entityData.get(FACE) < 0 ? position() : position().subtract(forward().scale(0.8));
    }

    public boolean reachedGoal(Player player, Vec3 previous) {
        Vec3 center = player.getBoundingBox().getCenter();
        Vec3 goal = pullGoal();
        double radius = Math.max(player.getBbWidth() / 2, 0.4);
        if (center.distanceToSqr(goal) <= radius * radius) return true;
        if (previous == null) return false;
        Vec3 movement = center.subtract(previous);
        if (movement.lengthSqr() < 1.0E-7) return false;
        double along = Math.clamp(goal.subtract(previous).dot(movement) / movement.lengthSqr(), 0, 1);
        return previous.add(movement.scale(along)).distanceToSqr(goal) <= radius * radius;
    }

    public static FrostChainHook active(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.hasUUID(ACTIVE_TAG)) {
            var entity = player.serverLevel().getEntity(data.getUUID(ACTIVE_TAG));
            if (entity instanceof FrostChainHook hook && !hook.isRemoved() && hook.getOwner() == player) return hook;
            data.remove(ACTIVE_TAG);
        }
        return null;
    }

    public void launch(ServerPlayer player) {
        setOwner(player);
        setPos(player.getEyePosition());
        Vec3 aim = player.getLookAngle();
        shoot(aim.x, aim.y, aim.z, (float) SPEED, 0);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!attached()) setPos(position().add(getDeltaMovement()));
            return;
        }
        if (!(getOwner() instanceof ServerPlayer player) || !player.isAlive() || player.isRemoved()
                || player.level() != level() || player.isSpectator() || player.isPassenger()) {
            discard();
            return;
        }
        if (!attached()) { fly(); return; }
        Vec3 center = player.getBoundingBox().getCenter();
        if (reachedGoal(player, previousCenter)
                || previousCenter != null && center.distanceToSqr(previousCenter) > 64
                || anchorBlock != null && (!level().hasChunkAt(anchorBlock)
                    || level().getBlockState(anchorBlock).getCollisionShape(level(), anchorBlock).isEmpty())) {
            discard();
            return;
        }
        previousCenter = center;
        player.fallDistance = 0;
        player.connection.aboveGroundTickCount = 0;
    }

    private void fly() {
        double stepLength = Math.min(SPEED, REACH - flownDistance);
        Vec3 end = position().add(getDeltaMovement().normalize().scale(stepLength));
        if (!level().hasChunkAt(BlockPos.containing(end))) { discard(); return; }
        var hit = level().clip(new ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            anchorBlock = hit.getBlockPos();
            attach(hit.getLocation(), hit.getDirection().get3DDataValue());
        } else {
            setPos(end);
            flownDistance += stepLength;
            if (flownDistance >= REACH - 1.0E-7) attach(end, -1);
        }
    }

    private void attach(Vec3 point, int face) {
        setPos(point);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(FACE, face);
        entityData.set(ATTACHED_AT, (int) level().getGameTime());
        previousCenter = getOwner().getBoundingBox().getCenter();
        playSound(SoundEvents.CHAIN_PLACE, 1, 1.2F);
    }

    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && getOwner() instanceof ServerPlayer player) {
            var data = player.getPersistentData();
            if (data.hasUUID(ACTIVE_TAG) && data.getUUID(ACTIVE_TAG).equals(getUUID())) data.remove(ACTIVE_TAG);
        }
        // Retraction removes the force without replacing the player's velocity.
        super.remove(reason);
    }
}
