package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.item.RiftbreakerItem;

@EventBusSubscriber(modid = "lyycore")
public record ToolModePayload() implements CustomPacketPayload {
    public static final Type<ToolModePayload> TYPE = new Type<>(ResourceLocation.parse("lyycore:tool_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolModePayload> CODEC = StreamCodec.unit(new ToolModePayload());
    @Override public Type<ToolModePayload> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) RiftbreakerItem.openSettings(player);
        });
    }
}
