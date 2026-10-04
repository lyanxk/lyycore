package org.lyy.lyycore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.network.ResearchNetwork;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class ResearchControls {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ResearchManager.clearClient();
    }
    public static final KeyMapping MEMORY = key("memory", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K);
    public static final KeyMapping STYLE = key("style", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V);
    // GLFW is zero-based: mouse button 5 is index 4.
    public static final KeyMapping SPECIAL = key("special_skill", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5);
    private static KeyMapping key(String name, InputConstants.Type type, int key) {
        return new KeyMapping("key.lyycore." + name, KeyConflictContext.IN_GAME, type, key, "key.categories.lyycore");
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        event.register(MEMORY); event.register(STYLE); event.register(SPECIAL);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        send(MEMORY, ResearchNetwork.Action.MEMORY);
        send(STYLE, ResearchNetwork.Action.NEXT_STYLE);
        send(SPECIAL, ResearchNetwork.Action.SPECIAL);
    }
    private static void send(KeyMapping key, ResearchNetwork.Action action) {
        var mc = Minecraft.getInstance();
        while (key.consumeClick()) {
            if (mc.player != null && mc.screen == null && (action == ResearchNetwork.Action.MEMORY || SkillSystem.unlocked(mc.player))) {
                PacketDistributor.sendToServer(new ResearchNetwork.Request(action));
            }
        }
    }
    @SubscribeEvent public static void hud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.parse("lyycore:style"), StyleHud::render);
    }
}
