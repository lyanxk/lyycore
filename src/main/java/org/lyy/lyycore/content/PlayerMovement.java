package org.lyy.lyycore.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Short dashes stop at obstacles instead of teleporting through them. */
public final class PlayerMovement {
    private PlayerMovement() { }
    public static boolean dash(ServerPlayer player, Vec3 direction, double distance) {
        if (direction.lengthSqr() < 1e-8 || distance <= 0) return false;
        direction = direction.normalize();
        Vec3 origin = player.position(), destination = origin;
        for (int step = 1; step <= Math.ceil(distance * 4); step++) {
            Vec3 offset = direction.scale(Math.min(distance, step * .25));
            var bounds = player.getBoundingBox().move(offset);
            if (!player.level().hasChunkAt(BlockPos.containing(origin.add(offset)))
                    || !player.level().getWorldBorder().isWithinBounds(bounds) || !player.level().noCollision(player, bounds)) break;
            destination = origin.add(offset);
        }
        if (destination.distanceToSqr(origin) < .01) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0;
        return true;
    }
}
