package org.lyy.lyycore.network;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.menu.SpacetimeLocatorMenu;

@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeLocatorNetwork {
    public static final UUID NONE = new UUID(0, 0);
    public enum Action { BIND, INPUT, OUTPUT, AREA, BLOCK, REMOVE, PREVIEW }
    public record Channel(UUID id, String name, boolean publicChannel) { }
    public record Target(GlobalPos first, @Nullable BlockPos second, @Nullable Direction side, int state) {
        public boolean area() { return second != null; }
        public List<GlobalPos> previewPositions() {
            return area() ? List.of(first, GlobalPos.of(first.dimension(), second)) : List.of(first);
        }
    }
    public record Request(int menu, Action action, UUID channel, boolean output, long revision, int target) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:spacetime_locator_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeEnum(p.action); b.writeUUID(p.channel); b.writeBoolean(p.output); b.writeLong(p.revision); b.writeVarInt(p.target);
        }, b -> new Request(b.readVarInt(), b.readEnum(Action.class), b.readUUID(), b.readBoolean(), b.readLong(), b.readVarInt()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Snapshot(int menu, UUID bound, boolean output, boolean area, long revision, List<Channel> channels, List<Target> targets) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.parse("lyycore:spacetime_locator_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeUUID(p.bound); b.writeBoolean(p.output); b.writeBoolean(p.area); b.writeLong(p.revision);
            b.writeCollection(p.channels, (buf, c) -> { buf.writeUUID(c.id); buf.writeUtf(c.name, 32); buf.writeBoolean(c.publicChannel); });
            b.writeCollection(p.targets, (buf, t) -> {
                buf.writeGlobalPos(t.first); buf.writeNullable(t.second, (out, pos) -> out.writeBlockPos(pos));
                buf.writeNullable(t.side, (out, side) -> out.writeEnum(side)); buf.writeVarInt(t.state);
            });
        }, b -> new Snapshot(b.readVarInt(), b.readUUID(), b.readBoolean(), b.readBoolean(), b.readLong(),
                b.readList(buf -> new Channel(buf.readUUID(), buf.readUtf(32), buf.readBoolean())),
                b.readList(buf -> new Target(buf.readGlobalPos(), buf.readNullable(in -> in.readBlockPos()),
                        buf.readNullable(in -> in.readEnum(Direction.class)), buf.readVarInt()))));
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    public record Highlight(GlobalPos position) implements CustomPacketPayload {
        public static final Type<Highlight> TYPE = new Type<>(ResourceLocation.parse("lyycore:spacetime_locator_highlight"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Highlight> CODEC = StreamCodec.of((b, p) -> b.writeGlobalPos(p.position), b -> new Highlight(b.readGlobalPos()));
        @Override public Type<Highlight> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof SpacetimeLocatorMenu menu
                    && menu.containerId == request.menu()) menu.handle(player, request);
        });
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (snapshot, context) -> {
            if (context.player().containerMenu instanceof SpacetimeLocatorMenu menu && menu.containerId == snapshot.menu()) menu.update(snapshot);
        });
        registrar.playToClient(Highlight.TYPE, Highlight.CODEC, (highlight, context) ->
                org.lyy.lyycore.client.SpacetimeLocatorOutline.show(highlight.position()));
    }
    private SpacetimeLocatorNetwork() { }
}
