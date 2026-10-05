package org.lyy.lyycore.content.skills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.network.SkillNetwork;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

final class MovementSkill {
    static final ResourceLocation BLINK_RESEARCH = ResourceLocation.parse("lyycore:research/behind_you");
    private static final Map<ServerPlayer, Integer> HOVER_UNTIL = new WeakHashMap<>();
    private MovementSkill() { }

    static boolean available(ServerPlayer player) {
        return ResearchProgress.completed(player, BasicSkills.RESEARCH) || ResearchProgress.completed(player, BLINK_RESEARCH);
    }

    static boolean cast(ServerPlayer player) {
        if (SkillInput.forwardSpecial(player) && ResearchProgress.completed(player, BLINK_RESEARCH)) return blink(player);
        return ResearchProgress.completed(player, BasicSkills.RESEARCH) && dash(player);
    }

    private static boolean blink(ServerPlayer player) {
        if (player.tickCount < HOVER_UNTIL.getOrDefault(player, 0)) return false;
        var target = player.level().getEntitiesOfClass(Mob.class, AABB.ofSize(player.position(), 10, 10, 10),
                mob -> mob instanceof Enemy && !(mob instanceof net.minecraft.world.entity.NeutralMob)
                        && mob.isAlive() && !mob.isAlliedTo(player)
                        && org.lyy.lyycore.content.control.MindControl.binding(mob) == null)
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target == null) return false;
        var behind = Vec3.directionFromRotation(0, target.getYRot()).scale(-1);
        var destination = target.position().add(behind);
        var bounds = player.getBoundingBox().move(destination.subtract(player.position()));
        if (!player.level().hasChunkAt(BlockPos.containing(destination))
                || !player.level().getWorldBorder().isWithinBounds(bounds)
                || !player.level().noCollision(player, bounds)
                || bounds.intersects(target.getBoundingBox())) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
        player.fallDistance = 0;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 254));
        HOVER_UNTIL.put(player, player.tickCount + 20);
        PacketDistributor.sendToPlayer(player, new SkillNetwork.Hover(player.level().getGameTime() + 20));
        player.serverLevel().sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1, destination.z, 12, .3, .5, .3, .1);
        return true;
    }

    static void tick(ServerPlayer player) {
        var until = HOVER_UNTIL.get(player);
        if (until == null) return;
        if (player.tickCount >= until || !player.isAlive() || player.isSpectator()) {
            HOVER_UNTIL.remove(player);
            return;
        }
        player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
        player.fallDistance = 0;
    }
    static void forget(ServerPlayer player) { HOVER_UNTIL.remove(player); }
    static void clear() { HOVER_UNTIL.clear(); }
    static boolean dash(ServerPlayer player) {
        if (!SkillInput.fresh(player)) return false;
        int forward = SkillInput.forward(player), strafe = SkillInput.strafe(player);
        Vec3 direction = player.getLookAngle();
        if (forward != 0 || strafe != 0) {
            double yaw = Math.toRadians(player.getYRot());
            direction = new Vec3(-Math.sin(yaw) * forward + Math.cos(yaw) * strafe, 0,
                    Math.cos(yaw) * forward + Math.sin(yaw) * strafe).normalize();
        }
        Vec3 destination = player.position();
        for (int step = 1; step <= 16; step++) {
            Vec3 offset = direction.scale(step * .25);
            var bounds = player.getBoundingBox().move(offset);
            if (!player.level().hasChunkAt(BlockPos.containing(player.position().add(offset)))
                    || !player.level().getWorldBorder().isWithinBounds(bounds) || !player.level().noCollision(player, bounds)) break;
            destination = player.position().add(offset);
        }
        if (destination.distanceToSqr(player.position()) < .01) return false;
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 8, .2, .2, .2, .02);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0;
        return true;
    }
}
