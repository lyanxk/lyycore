package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.lyy.lyycore.content.entity.guiding.GuidingBoss;

/** Apply capture movement before vanilla travel so key input cannot cancel the pull. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class GuidingCaptureControls {
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var player = event.getEntity();
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof GuidingBoss boss) || !boss.isAlive() || !boss.pulling(player)) continue;
            var input = event.getInput();
            input.forwardImpulse = input.leftImpulse = 0;
            input.up = input.down = input.left = input.right = input.jumping = false;
            player.stopFallFlying(); player.setDeltaMovement(boss.pullVelocity(player)); player.fallDistance = 0;
            return;
        }
    }
}
