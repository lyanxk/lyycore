package org.lyy.lyycore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lyy.lyycore.network.ToolModePayload;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class ToolModeControls {
    public static final KeyMapping MODE = new KeyMapping("key.lyycore.tool_mode", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.lyycore");

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(MODE); }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        while (MODE.consumeClick()) {
            if (mc.screen == null && mc.player != null && !mc.player.isSpectator())
                PacketDistributor.sendToServer(new ToolModePayload());
        }
    }
}
