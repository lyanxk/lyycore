package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.client.SpacetimeChainRenderer;

import java.util.List;

@EventBusSubscriber(modid = "lyycore")
public record SpacetimeChainPayload(ResourceLocation dimension, List<Link> links) implements CustomPacketPayload {
    public record Link(int player, int target, int color) {
        public Link(int player, int target) { this(player, target, 0xEB4CB4FF); }
    }
    public static final Type<SpacetimeChainPayload> TYPE = new Type<>(ResourceLocation.parse("lyycore:spacetime_chains"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpacetimeChainPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeCollection(payload.links, (buf, link) -> { buf.writeVarInt(link.player); buf.writeVarInt(link.target); buf.writeInt(link.color); });
            }, buffer -> new SpacetimeChainPayload(buffer.readResourceLocation(),
                    buffer.readList(buf -> new Link(buf.readVarInt(), buf.readVarInt(), buf.readInt()))));
    @Override public Type<SpacetimeChainPayload> type() { return TYPE; }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> SpacetimeChainRenderer.update(payload));
    }
}
