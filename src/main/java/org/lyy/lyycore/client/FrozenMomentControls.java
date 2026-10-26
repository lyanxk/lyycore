package org.lyy.lyycore.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lyy.lyycore.content.item.FrozenMomentItem;
import org.lyy.lyycore.network.FrozenMomentShot;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class FrozenMomentControls {
    private static CameraType previousCamera;
    private static boolean scoped() {
        var player = Minecraft.getInstance().player;
        return player != null && FrozenMomentItem.scoping(player);
    }
    private static void fire() {
        var mc = Minecraft.getInstance();
        if (mc.screen == null && mc.isWindowActive() && mc.player != null && !mc.player.isSpectator()
                && mc.player.getMainHandItem().getItem() instanceof FrozenMomentItem)
            PacketDistributor.sendToServer(new FrozenMomentShot());
    }
    // Raw press events also arrive while holding use; vanilla suppresses ordinary attacks during aiming.
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) fire();
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().options.keyAttack.matches(event.getKey(), event.getScanCode())) fire();
    }
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && player.getMainHandItem().getItem() instanceof FrozenMomentItem) {
            event.setCanceled(true); event.setSwingHand(false);
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (scoped()) {
            if (previousCamera == null) previousCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (previousCamera != null) {
            mc.options.setCameraType(previousCamera); previousCamera = null;
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fov(ViewportEvent.ComputeFov event) {
        if (!scoped()) return;
        // Magnify relative to the configured normal view, independently of movement effects.
        // Use the same projection for both FOV passes; hands are hidden while aiming.
        double normalFov = Minecraft.getInstance().options.fov().get();
        event.setFOV(Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(normalFov) / 2) / 4)));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hud(RenderGuiEvent.Pre event) {
        if (!scoped()) return;
        event.setCanceled(true);
        FrozenMomentScope.render(event.getGuiGraphics());
    }
    @SubscribeEvent public static void hand(RenderHandEvent event) { if (scoped()) event.setCanceled(true); }
}
