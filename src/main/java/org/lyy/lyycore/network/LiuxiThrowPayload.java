package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.item.LiuxiItem;

@EventBusSubscriber(modid = "lyycore")
public record LiuxiThrowPayload() implements CustomPacketPayload {
    public static final Type<LiuxiThrowPayload> TYPE = new Type<>(ResourceLocation.parse("lyycore:liuxi_throw"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LiuxiThrowPayload> CODEC = StreamCodec.unit(new LiuxiThrowPayload());
    @Override public Type<LiuxiThrowPayload> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) LiuxiItem.throwKnife(player);
        });
    }
}
