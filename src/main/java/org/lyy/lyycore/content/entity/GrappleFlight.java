package org.lyy.lyycore.content.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Local player traction, applied immediately before vanilla travel. */
public final class GrappleFlight {
    private static final double CRUISE_SPEED = 0.95;
    // Must exceed normal gravity (0.08) and air drag to sustain an upward swing.
    private static final double MOTOR_ACCELERATION = 0.16;
    private static final double RADIAL_ACCELERATION = 0.2;

    private final GrappleHook hook;
    private Vec3 previousPosition;
    private Vec3 previousCenter;
    private Vec3 swingDirection;
    private double traveled;
    private int blockedTicks;
    private boolean airborne, released, jumpApplied;

    public GrappleFlight(GrappleHook hook) { this.hook = hook; }
    public GrappleHook hook() { return hook; }
    public boolean isReleased() { return released; }

    public void tick(Player player, float strafe) {
        if (released || !hook.isPullingPlayer()) return;
        // Check before even the one-time jump impulse, so detachment changes no
        // velocity component. The server independently checks the release conditions.
        if (hook.shouldBreakChain()) {
            hook.breakChain();
            released = true;
            return;
        }
        if (player.isSpectator() || player.isPassenger() || player.getAbilities().flying
                || player.isFallFlying() || player.isInWaterOrBubble() || player.isInLava()) {
            released = true;
            return;
        }
        if (airborne && player.onGround()) { released = true; return; }
        airborne |= !player.onGround();
        if (previousPosition != null) {
            double moved = player.position().distanceTo(previousPosition);
            if (moved > 8) { released = true; return; }
            traveled += moved;
        }
        previousPosition = player.position();
        Vec3 lastCenter = previousCenter;
        previousCenter = player.getBoundingBox().getCenter();
        if (traveled >= GrappleHook.FLIGHT_DISTANCE) { released = true; return; }

        // This is the actual velocity after last tick's gravity, friction and
        // collisions, not a separate ideal velocity maintained by the server.
        Vec3 velocity = player.getDeltaMovement();
        if (!jumpApplied && hook.jumpImpulse() > 0) {
            velocity = new Vec3(velocity.x, Math.max(velocity.y, hook.jumpImpulse()), velocity.z);
            jumpApplied = true;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double input = hook.acceptsContactInput() ? Mth.clamp(strafe, -1, 1) * 0.12 / GrappleHook.CONTACT_INPUT_TICKS : 0;
        Vec3 steering = new Vec3(Mth.cos(yaw) * input, 0, Mth.sin(yaw) * input);
        Vec3 acceleration;
        if (hook.swingRadius() > 0) {
            acceleration = swingAcceleration(player, velocity, steering);
        } else {
            Vec3 toward = hook.pullGoal().subtract(player.getBoundingBox().getCenter());
            double distance = toward.length();
            if (!hook.acceptsContactInput()) {
                if (hook.reachedPullGoal(player, lastCenter)) {
                    released = true;
                    return;
                }
                blockedTicks = hook.blockedDirectPull(player, lastCenter) ? blockedTicks + 1 : 0;
                if (blockedTicks >= GrappleHook.BLOCKED_RELEASE_TICKS) {
                    released = true;
                    return;
                }
            } else blockedTicks = 0;
            double targetSpeed = Math.min(CRUISE_SPEED,
                    Math.sqrt(2 * MOTOR_ACCELERATION * Math.max(0, distance - GrappleHook.arrivalRadius(player))));
            acceleration = limit(toward.normalize().scale(targetSpeed).subtract(velocity).add(steering), MOTOR_ACCELERATION);
        }
        player.setDeltaMovement(velocity.add(acceleration));
        player.fallDistance = 0;
        // Vanilla travel moves/collides, then applies gravity and drag once. No
        // extra gravity, inverse-friction compensation or motion packets here.
    }

    private Vec3 swingAcceleration(Player player, Vec3 velocity, Vec3 steering) {
        Vec3 offset = player.getBoundingBox().getCenter().subtract(hook.position());
        double distance = Math.max(offset.length(), 0.001);
        Vec3 radial = offset.scale(1 / distance);
        double radialSpeed = velocity.dot(radial);
        Vec3 tangentVelocity = velocity.subtract(radial.scale(radialSpeed));
        if (swingDirection == null) {
            swingDirection = tangentVelocity;
            if (swingDirection.lengthSqr() < 1.0E-5) {
                float yaw = player.getYRot() * Mth.DEG_TO_RAD;
                swingDirection = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            }
        }
        // Carry the intended direction around the pivot, so a momentary loss of
        // upward speed cannot reverse the motor and bounce back to the start.
        swingDirection = swingDirection.subtract(radial.scale(swingDirection.dot(radial)));
        if (swingDirection.lengthSqr() < 1.0E-5)
            swingDirection = new Vec3(0, 1, 0).subtract(radial.scale(radial.y));
        swingDirection = swingDirection.normalize();
        double targetSpeed = Math.min(CRUISE_SPEED, Math.sqrt(RADIAL_ACCELERATION * distance));
        Vec3 motor = limit(swingDirection.scale(targetSpeed).subtract(tangentVelocity).add(steering), MOTOR_ACCELERATION);
        motor = motor.subtract(radial.scale(motor.dot(radial)));

        // Centripetal force plus a damped radius correction. Radius error never
        // becomes a replacement velocity or feeds back into the motor's speed.
        double tension = tangentVelocity.lengthSqr() / distance
                + (distance - hook.swingRadius()) * 0.08 + radialSpeed * 0.5;
        tension = Mth.clamp(tension, -RADIAL_ACCELERATION, RADIAL_ACCELERATION);
        return motor.subtract(radial.scale(tension));
    }

    private static Vec3 limit(Vec3 vector, double maximum) {
        return vector.lengthSqr() > maximum * maximum ? vector.normalize().scale(maximum) : vector;
    }
}
