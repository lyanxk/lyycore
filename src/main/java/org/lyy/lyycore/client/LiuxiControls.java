package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.item.LiuxiItem;
import org.lyy.lyycore.network.LiuxiThrowPayload;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class LiuxiControls {
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (!event.isAttack() || player == null || !(player.getMainHandItem().getItem() instanceof LiuxiItem)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!LiuxiItem.isThrown(player.getMainHandItem())) PacketDistributor.sendToServer(new LiuxiThrowPayload());
    }
}
