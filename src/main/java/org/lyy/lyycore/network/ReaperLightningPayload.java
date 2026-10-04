package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.client.ReaperLightningRenderer;
import org.lyy.lyycore.content.ReaperLightning;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "lyycore")
public record ReaperLightningPayload(List<Vec3> points) implements CustomPacketPayload {
    public static final Type<ReaperLightningPayload> TYPE = new Type<>(ResourceLocation.parse("lyycore:reaper_lightning"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReaperLightningPayload> CODEC = StreamCodec.of((buffer, payload) -> {
        buffer.writeVarInt(payload.points.size());
        payload.points.forEach(buffer::writeVec3);
    }, buffer -> {
        int size = buffer.readVarInt();
        if (size < 2 || size > ReaperLightning.MAX_TARGETS + 1) throw new IllegalArgumentException("Invalid lightning chain length");
        var points = new ArrayList<Vec3>(size);
        for (int i = 0; i < size; i++) points.add(buffer.readVec3());
        return new ReaperLightningPayload(points);
    });

    public ReaperLightningPayload { points = List.copyOf(points); }
    @Override public Type<ReaperLightningPayload> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> ReaperLightningRenderer.add(payload.points));
    }
}
