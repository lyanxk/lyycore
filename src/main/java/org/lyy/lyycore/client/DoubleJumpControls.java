package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.skills.DoubleJump;
import org.lyy.lyycore.network.SkillNetwork;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class DoubleJumpControls {
    private static LocalPlayer currentPlayer;
    private static boolean held, used, consumedPress;

    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        var player = Minecraft.getInstance().player;
        if (currentPlayer != player) {
            currentPlayer = player;
            held = used = consumedPress = false;
        }
        if (player != null && (player.onGround() || !player.isAlive())) used = false;
    }

    // Run after capture/grapple controls so their movement restrictions take precedence.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) return;
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !held;
        held = down;
        if (!down) consumedPress = false;

        if (pressed && !used && event.getInput().jumping && mc.screen == null && mc.isWindowActive()
                && DoubleJump.canJump(mc.player)) {
            used = true;
            consumedPress = true;
            PacketDistributor.sendToServer(new SkillNetwork.DoubleJumpRequest());
        }
        // Vanilla and Caelus check jumping later in this same tick. Consume the whole
        // press, otherwise holding jump would start gliding on the following tick.
        if (consumedPress) event.getInput().jumping = false;
    }

    private DoubleJumpControls() { }
}
