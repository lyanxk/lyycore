package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.item.FrozenMomentItem;

@EventBusSubscriber(modid = "lyycore")
public record FrozenMomentShot() implements CustomPacketPayload {
    public static final Type<FrozenMomentShot> TYPE = new Type<>(ResourceLocation.parse("lyycore:frozen_moment_shot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrozenMomentShot> CODEC = StreamCodec.unit(new FrozenMomentShot());
    @Override public Type<FrozenMomentShot> type() { return TYPE; }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) FrozenMomentItem.fire(player);
        });
    }
}
