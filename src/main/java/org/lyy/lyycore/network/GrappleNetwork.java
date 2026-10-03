package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.GrappleHook;

@EventBusSubscriber(modid = LyyCore.MODID)
public final class GrappleNetwork {
    public record Controls(boolean jump) implements CustomPacketPayload {
        public static final Type<Controls> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "grapple_controls"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Controls> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeBoolean(value.jump),
                buffer -> new Controls(buffer.readBoolean()));
        @Override public Type<Controls> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(Controls.TYPE, Controls.CODEC, (message, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                GrappleHook hook = GrappleHook.active(player);
                if (hook != null) hook.control(message.jump());
            }
        });
    }
}
