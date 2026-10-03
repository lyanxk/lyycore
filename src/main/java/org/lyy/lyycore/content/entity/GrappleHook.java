package org.lyy.lyycore.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** One hook owns the entire flight, including the coast until landing. */
public final class GrappleHook extends Projectile {
    public static final String ACTIVE_TAG = "ImaginaryGrapple";
    public static final double REACH = 20;
    public static final double FLIGHT_DISTANCE = 40;
    public static final int CONTACT_INPUT_TICKS = 4;
    private static final double MIN_SWING_RADIUS = 0.8;
    public static final int BLOCKED_RELEASE_TICKS = 10;
    private static final EntityDataAccessor<Integer> ATTACHED_AT = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACE = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REELING_ENTITY = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PULLING_PLAYER = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHAIN_BROKEN = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SWING_RADIUS = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> SWING_FORWARD = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> JUMP_IMPULSE = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.FLOAT);
    private Vec3 launchPosition;
    private Vec3 previousPlayerPosition;
    private Vec3 previousPlayerCenter;
    private BlockPos anchorBlock;
    private double flownDistance;
    private int blockedTicks;
    private boolean airborne, coasting, exhausted, jumpInputCaptured, risingAtContact;
    private boolean jump;
    private long lastInput = Long.MIN_VALUE;

    public GrappleHook(EntityType<? extends GrappleHook> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ATTACHED_AT, -1);
        builder.define(FACE, Direction.UP);
        builder.define(TARGET_ID, -1);
        builder.define(REELING_ENTITY, false);
        builder.define(PULLING_PLAYER, false);
        builder.define(CHAIN_BROKEN, false);
        builder.define(SWING_RADIUS, 0F);
        builder.define(SWING_FORWARD, new Vector3f());
        builder.define(JUMP_IMPULSE, 0F);
    }
    public boolean attached() { return entityData.get(ATTACHED_AT) >= 0; }
    public boolean isReelingEntity() { return entityData.get(REELING_ENTITY); }
    public boolean isPullingPlayer() { return entityData.get(PULLING_PLAYER); }
    public boolean isChainBroken() { return entityData.get(CHAIN_BROKEN); }
    public float swingRadius() { return entityData.get(SWING_RADIUS); }
    public float jumpImpulse() { return entityData.get(JUMP_IMPULSE); }
    public Vec3 pullGoal() {
        return anchorEntity() != null ? position()
                : position().add(Vec3.atLowerCornerOf(anchorFace().getNormal()).scale(0.8));
    }
    public static double arrivalRadius(Player player) {
        var bounds = player.getBoundingBox();
        return Math.max(bounds.getXsize(), bounds.getZsize()) / 2;
    }
    public boolean reachedPullGoal(Player player, Vec3 previousCenter) {
        Vec3 center = player.getBoundingBox().getCenter();
        Vec3 goal = pullGoal();
        double radius = arrivalRadius(player);
        if (center.distanceToSqr(goal) <= radius * radius) return true;
        if (previousCenter == null) return false;
        // A fast movement packet may cross the arrival sphere in one tick.
        Vec3 movement = center.subtract(previousCenter);
        if (movement.lengthSqr() < 1.0E-7) return false;
        double along = Math.clamp(goal.subtract(previousCenter).dot(movement) / movement.lengthSqr(), 0, 1);
        return previousCenter.add(movement.scale(along)).distanceToSqr(goal) <= radius * radius;
    }
    public boolean blockedDirectPull(Player player, Vec3 previousCenter) {
        if (previousCenter == null) return false;
        Vec3 center = player.getBoundingBox().getCenter();
        Vec3 direction = pullGoal().subtract(center).normalize();
        // Require both collision and negligible forward progress. Swinging does
        // not use this test because its distance to the pivot need not decrease.
        return center.subtract(previousCenter).dot(direction) < 0.01
                && !level().noCollision(player, player.getBoundingBox().move(direction.scale(0.05)));
    }
    public Entity anchorEntity() { return level().getEntity(entityData.get(TARGET_ID)); }
    public Direction anchorFace() { return entityData.get(FACE); }
    public float attachedAge(float partial) { return attached() ? (level().getGameTime() - entityData.get(ATTACHED_AT)) + partial : 0; }
    public boolean acceptsContactInput() {
        return attached() && isPullingPlayer() && !isChainBroken() && !isReelingEntity()
                && attachedAge(0) < CONTACT_INPUT_TICKS;
    }

    public boolean shouldBreakChain() {
        if (!attached() || isChainBroken() || getOwner() == null) return false;
        if (swingRadius() > 0) {
            Vec3 offset = getOwner().getBoundingBox().getCenter().subtract(position());
            Vector3f forward = entityData.get(SWING_FORWARD);
            double pastAnchor = offset.x * forward.x() + offset.z * forward.z();
            // Release as soon as the player crosses above the anchor in the
            // original approach direction. Looking around cannot end a swing.
            boolean pastTop = offset.y > 0 && pastAnchor >= 0;
            return pastTop || isChainObstructed();
        }
        Vec3 towardHook = position().subtract(getOwner().position());
        double horizontalDistance = towardHook.horizontalDistance();
        if (horizontalDistance > 1.0E-4) {
            double yaw = Math.toRadians(getOwner().getYRot());
            double facingDot = (-Math.sin(yaw) * towardHook.x + Math.cos(yaw) * towardHook.z) / horizontalDistance;
            // Rear 45-degree cone: at least 135 degrees from forward. Pitch does
            // not affect release, and a hook directly above/below has no rear side.
            if (facingDot <= -Math.sqrt(0.5) + 1.0E-7) return true;
        }
        return isChainObstructed();
    }

    public boolean isChainObstructed() {
        if (!attached() || getOwner() == null) return false;
        Vec3 start = position();
        // Start just outside the contact face; touching the anchor is intentional.
        // Do not ignore the entire anchor block: crossing behind it must still break.
        if (anchorEntity() == null) start = start.add(Vec3.atLowerCornerOf(anchorFace().getNormal()).scale(0.01));
        Vec3 end = getOwner().getEyePosition().add(0, -0.4, 0);
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.BLOCK;
    }

    public void breakChain() {
        if (isChainBroken()) return;
        entityData.set(CHAIN_BROKEN, true);
        // Leave both player and target velocity untouched.
        if (isReelingEntity()) discard();
        else releasePull();
    }

    public void releasePull() {
        entityData.set(PULLING_PLAYER, false);
        coasting = true;
        // Airborne players regain movement now and retain fall protection until
        // landing. A grounded player need not take off before the hook can reset.
        if (!level().isClientSide && getOwner() instanceof Player player && player.onGround()) {
            player.fallDistance = 0;
            discard();
        }
    }

    public static GrappleHook active(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.hasUUID(ACTIVE_TAG)) {
            var entity = player.serverLevel().getEntity(data.getUUID(ACTIVE_TAG));
            if (entity instanceof GrappleHook hook && !hook.isRemoved() && hook.getOwner() == player) {
                if (!hook.attached() || hook.isReelingEntity() || !player.onGround()
                        || !hook.airborne && hook.isPullingPlayer()) return hook;
                hook.discard();
            }
            data.remove(ACTIVE_TAG);
        }
        return null;
    }

    public void launch(ServerPlayer player) {
        setOwner(player);
        launchPosition = player.getEyePosition();
        setPos(launchPosition);
        Vec3 aim = player.getLookAngle();
        shoot(aim.x, aim.y, aim.z, 2, 0);
    }

    public void control(boolean jump) {
        if (attached()) {
            if (isReelingEntity()) return;
            if (!acceptsContactInput()) return;
            if (!jumpInputCaptured && jump) {
                this.jump = true;
                captureJumpInput();
            }
            return;
        }
        this.jump = jump;
        lastInput = level().getGameTime();
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!attached()) setPos(position().add(getDeltaMovement()));
            else if (anchorEntity() != null) setPos(anchorEntity().getBoundingBox().getCenter());
            if (shouldBreakChain()) breakChain();
            return;
        }
        if (!(getOwner() instanceof ServerPlayer player) || !player.isAlive() || player.isRemoved()
                || player.level() != level() || player.isSpectator() || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying()) {
            discard();
            return;
        }
        if (!attached()) { fly(player); return; }
        if (isChainBroken()) { pull(player); return; }
        if (entityData.get(TARGET_ID) >= 0) {
            Entity target = anchorEntity();
            if (target == null || !target.isAlive() || target.isRemoved() || target.level() != level()) { discard(); return; }
            setPos(target.getBoundingBox().getCenter());
            if (shouldBreakChain()) { breakChain(); return; }
            if (isReelingEntity()) pullEntity(player, target);
            else pull(player);
        } else {
            if (shouldBreakChain()) { breakChain(); return; }
            pull(player);
        }
    }

    private void fly(ServerPlayer player) {
        if (launchPosition == null) { discard(); return; }
        double remaining = REACH - position().distanceTo(launchPosition);
        if (remaining <= 0.001 || tickCount > 30) { discard(); return; }
        Vec3 step = getDeltaMovement().normalize().scale(Math.min(getDeltaMovement().length(), remaining));
        Vec3 end = position().add(step);
        if (!level().hasChunkAt(BlockPos.containing(end))) { discard(); return; }
        var hit = level().clip(new ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 collisionEnd = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
        var entityHit = ProjectileUtil.getEntityHitResult(this, position(), collisionEnd,
                getBoundingBox().expandTowards(step).inflate(0.3),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof Projectile) && !entity.isPassengerOfSameVehicle(player)
                        && (entity.isPickable() || entity instanceof ItemEntity),
                position().distanceToSqr(collisionEnd));
        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            entityData.set(TARGET_ID, target.getId());
            entityData.set(REELING_ENTITY, canPull(target));
            attach(player, target.getBoundingBox().getCenter(), Direction.UP);
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            anchorBlock = hit.getBlockPos();
            attach(player, hit.getLocation(), hit.getDirection());
        } else {
            setPos(end);
            if (remaining <= step.length() + 0.001) discard();
        }
    }

    private void attach(ServerPlayer player, Vec3 point, Direction face) {
        setPos(point);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(FACE, face);
        entityData.set(ATTACHED_AT, (int) level().getGameTime());
        previousPlayerPosition = player.position();
        previousPlayerCenter = player.getBoundingBox().getCenter();
        airborne = !player.onGround();
        // Accepted movement packets tell us whether a jump is already in progress.
        Vec3 flightVelocity = player.getKnownMovement();
        if (flightVelocity.lengthSqr() < 1.0E-7) flightVelocity = player.getDeltaMovement();
        risingAtContact = flightVelocity.y > 0;
        entityData.set(PULLING_PLAYER, !isReelingEntity());
        if (!isReelingEntity() && lastInput != Long.MIN_VALUE && level().getGameTime() - lastInput <= 5) captureJumpInput();
        playSound(SoundEvents.CHAIN_PLACE, 1, 0.8F);
    }

    private void captureJumpInput() {
        if (!jump) return; // Neutral input does not consume the four-tick jump opportunity.
        // A jump already in progress is inherited, not replaced or boosted a second time.
        double jumpImpulse = !risingAtContact ? 0.42 : 0;
        entityData.set(JUMP_IMPULSE, (float) jumpImpulse);
        if (getOwner() != null) {
            double radius = getOwner().getBoundingBox().getCenter().distanceTo(position());
            if (radius > MIN_SWING_RADIUS) {
                Vec3 approach = position().subtract(getOwner().getBoundingBox().getCenter());
                Vec3 forward = new Vec3(approach.x, 0, approach.z).normalize();
                if (forward.lengthSqr() < 1.0E-7) {
                    double yaw = Math.toRadians(getOwner().getYRot());
                    forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                }
                entityData.set(SWING_FORWARD, new Vector3f((float) forward.x, 0, (float) forward.z));
                entityData.set(SWING_RADIUS, (float) radius);
            }
        }
        jumpInputCaptured = true;
    }

    private static boolean canPull(Entity target) {
        if (target.isPassenger() || !(target.isPushable() || target instanceof ItemEntity)) return false;
        return !(target instanceof LivingEntity living) || living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1;
    }

    private static Vec3 limitSpeed(Vec3 velocity, double maximum) {
        return velocity.lengthSqr() > maximum * maximum ? velocity.normalize().scale(maximum) : velocity;
    }

    private void pullEntity(ServerPlayer player, Entity target) {
        if (!canPull(target) || attachedAge(0) > 200 || target.distanceToSqr(player) > FLIGHT_DISTANCE * FLIGHT_DISTANCE) {
            discard();
            return;
        }
        Vec3 toward = player.getBoundingBox().getCenter().subtract(target.getBoundingBox().getCenter());
        if (toward.length() < 1.5) { discard(); return; }
        Vec3 velocity = target.getDeltaMovement().scale(0.8).add(toward.normalize().scale(0.16));
        target.setDeltaMovement(limitSpeed(velocity, Math.min(0.8, toward.length() * 0.15)));
        target.fallDistance = 0;
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private void pull(ServerPlayer player) {
        if ((airborne || coasting || exhausted) && player.onGround()) { player.fallDistance = 0; discard(); return; }
        if (!player.onGround()) airborne = true;
        if (player.isInWaterOrBubble() || player.isInLava()) { discard(); return; }
        double moved = player.position().distanceTo(previousPlayerPosition);
        previousPlayerPosition = player.position();
        Vec3 lastCenter = previousPlayerCenter;
        previousPlayerCenter = player.getBoundingBox().getCenter();
        if (moved > 8) { discard(); return; } // Teleports end the flight rather than dragging the player back.
        flownDistance += moved;
        player.fallDistance = 0;
        player.connection.aboveGroundTickCount = 0;
        if (coasting || exhausted) return;
        if (anchorBlock != null && (!level().hasChunkAt(anchorBlock)
                || level().getBlockState(anchorBlock).getCollisionShape(level(), anchorBlock).isEmpty())) {
            releasePull();
            return;
        }

        // Players simulate their own movement, including vanilla gravity, drag and
        // collisions. The server owns the attachment and its lifetime, but must not
        // send a replacement velocity every tick based on delayed movement packets.
        if (swingRadius() == 0 && !acceptsContactInput()) {
            if (reachedPullGoal(player, lastCenter)) {
                releasePull();
                return;
            }
            blockedTicks = blockedDirectPull(player, lastCenter) ? blockedTicks + 1 : 0;
            if (blockedTicks >= BLOCKED_RELEASE_TICKS) {
                releasePull();
                return;
            }
        } else blockedTicks = 0;
        if (flownDistance >= FLIGHT_DISTANCE) {
            exhausted = true;
            releasePull();
        }
    }

    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && getOwner() instanceof ServerPlayer player) {
            var data = player.getPersistentData();
            if (data.hasUUID(ACTIVE_TAG) && data.getUUID(ACTIVE_TAG).equals(getUUID())) data.remove(ACTIVE_TAG);
        }
        super.remove(reason);
    }
}
