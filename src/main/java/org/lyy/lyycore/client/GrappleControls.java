package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.GrappleHook;
import org.lyy.lyycore.content.entity.GrappleFlight;
import org.lyy.lyycore.network.GrappleNetwork;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class GrappleControls {
    private static GrappleFlight flight;
    private static GrappleHook activeHook(net.minecraft.world.entity.LivingEntity owner) {
        var level = Minecraft.getInstance().level;
        if (owner == null || level == null) return null;
        for (var entity : level.entitiesForRendering())
            if (entity instanceof GrappleHook hook && hook.getOwner() == owner && !hook.isRemoved()) return hook;
        return null;
    }
    public static boolean hasActiveHook(net.minecraft.world.entity.LivingEntity owner) { return activeHook(owner) != null; }

    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        GrappleHook hook = activeHook(event.getEntity());
        if (hook == null || !hook.attached() || !hook.isPullingPlayer() || hook.isReelingEntity()) return;
        if (flight == null || flight.hook() != hook) flight = new GrappleFlight(hook);
        var minecraft = Minecraft.getInstance();
        float strafe = minecraft.screen == null
                ? (minecraft.options.keyLeft.isDown() ? 1 : 0) - (minecraft.options.keyRight.isDown() ? 1 : 0) : 0;
        flight.tick(event.getEntity(), strafe);
        // Preserve this tick's normal input when local arrival/collision released
        // the motor, even before the server's release state reaches the client.
        if (!flight.isReleased() && hook.isPullingPlayer()) {
            var input = event.getInput();
            input.forwardImpulse = input.leftImpulse = 0;
            input.up = input.down = input.left = input.right = input.jumping = false;
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) { flight = null; return; }
        if (minecraft.isPaused()) return;
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof GrappleHook hook) || hook.getOwner() != minecraft.player || hook.isRemoved()) continue;
            boolean inGame = minecraft.screen == null;
            if (!hook.attached() || hook.acceptsContactInput())
                PacketDistributor.sendToServer(new GrappleNetwork.Controls(inGame && minecraft.options.keyJump.isDown()));
            if (hook.attached()) minecraft.player.fallDistance = 0;
            return;
        }
        flight = null;
    }
}
