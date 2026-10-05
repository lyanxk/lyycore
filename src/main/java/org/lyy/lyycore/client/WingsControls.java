package org.lyy.lyycore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.registry.LyyEffects;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class WingsControls {
    public static final KeyMapping BOOST = new KeyMapping("key.lyycore.wings_boost", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.lyycore");
    private static boolean sent;
    private WingsControls() { }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(BOOST); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { sent = false; }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (BOOST.consumeClick()) { }
        boolean held = mc.screen == null && mc.isWindowActive() && BOOST.isDown() && mc.player.isFallFlying() && AegisWings.level(mc.player) >= 2;
        if (held != sent || held && mc.player.tickCount % 10 == 0) {
            PacketDistributor.sendToServer(new WingsNetwork.Boost(held)); sent = held;
        }
        mc.player.getPersistentData().putBoolean("lyycore:wings_boosting", held);
        if (held) AegisWings.boostFlight(mc.player);
        if (mc.player.hasEffect(LyyEffects.NETHER_VISION)) mc.player.spinningEffectIntensity = 1;
    }
}
