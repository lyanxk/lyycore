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
import java.util.List;

@EventBusSubscriber(modid = "lyycore")
public record ReaperLightningPayload(List<Vec3> points) implements CustomPacketPayload {
    public static final Type<ReaperLightningPayload> TYPE = new Type<>(ResourceLocation.parse("lyycore:reaper_lightning"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReaperLightningPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeCollection(payload.points, (buf, point) -> buf.writeVec3(point)),
            buffer -> new ReaperLightningPayload(buffer.readList(buf -> buf.readVec3())));
    @Override public Type<ReaperLightningPayload> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> ReaperLightningRenderer.add(payload.points));
    }
}
