package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.FrostChainHook;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class FrostChainControls {
    private static FrostChainHook active;
    private static Vec3 previousCenter;
    private static boolean arrived;

    private static FrostChainHook activeHook(LivingEntity owner) {
        var level = Minecraft.getInstance().level;
        if (owner != null && level != null) {
            for (var entity : level.entitiesForRendering()) {
                if (entity instanceof FrostChainHook hook && hook.getOwner() == owner && !hook.isRemoved()) return hook;
            }
        }
        return null;
    }

    public static boolean hasActiveHook(LivingEntity owner) { return activeHook(owner) != null; }

    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        FrostChainHook hook = activeHook(player);
        if (hook != active) {
            active = hook;
            previousCenter = null;
            arrived = false;
        }
        if (hook == null || !hook.attached() || arrived || player.isPassenger()
                || player.isSpectator()) return;
        arrived = hook.reachedGoal(player, previousCenter);
        previousCenter = player.getBoundingBox().getCenter();
        if (arrived) return;
        Vec3 direction = hook.pullGoal().subtract(previousCenter).normalize();
        // 128 m/s² = 0.32 blocks/tick². Vanilla travel still handles gravity,
        // air/water drag, movement input and collisions after this additive force.
        player.setDeltaMovement(player.getDeltaMovement().add(direction.scale(FrostChainHook.ACCELERATION)));
        player.fallDistance = 0;
    }
}
