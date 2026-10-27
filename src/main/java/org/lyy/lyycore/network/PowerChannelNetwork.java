package org.lyy.lyycore.network;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.energy.PowerChannels;
import org.lyy.lyycore.content.menu.SpacetimeCoordinatesMenu;
import org.lyy.lyycore.content.menu.SpacetimeMatrixMenu;

/** Server-owned channel edits and deliberately requested, static block previews. */
@EventBusSubscriber(modid = "lyycore")
public final class PowerChannelNetwork {
    public static final UUID NONE = new UUID(0, 0);
    public enum Action { UPDATE, BIND, REMOVE, COPY, PREVIEW }
    public record Channel(UUID id, String name, boolean publicChannel, boolean editable) { }
    public record Target(GlobalPos address, int state) { }

    public record Request(int menu, Action action, UUID channel, String name, boolean publicChannel, GlobalPos target)
            implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:power_channel_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeEnum(p.action); b.writeUUID(p.channel);
            b.writeUtf(p.name, PowerChannels.NAME_LIMIT); b.writeBoolean(p.publicChannel);
            b.writeBoolean(p.target != null); if (p.target != null) writePos(b, p.target);
        }, b -> new Request(b.readVarInt(), b.readEnum(Action.class), b.readUUID(), b.readUtf(PowerChannels.NAME_LIMIT), b.readBoolean(), b.readBoolean() ? readPos(b) : null));
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Snapshot(int menu, UUID bound, List<Channel> channels, List<Target> targets, int power, int stage)
            implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.parse("lyycore:power_channel_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeUUID(p.bound);
            b.writeCollection(p.channels, (buf, c) -> {
                buf.writeUUID(c.id); buf.writeUtf(c.name, PowerChannels.NAME_LIMIT); buf.writeBoolean(c.publicChannel); buf.writeBoolean(c.editable);
            });
            b.writeCollection(p.targets, (buf, t) -> { writePos(buf, t.address); buf.writeVarInt(t.state); });
            b.writeVarInt(p.power); b.writeVarInt(p.stage);
        }, b -> new Snapshot(b.readVarInt(), b.readUUID(),
                b.readList(buf -> new Channel(buf.readUUID(), buf.readUtf(PowerChannels.NAME_LIMIT), buf.readBoolean(), buf.readBoolean())),
                b.readList(buf -> new Target(readPos(buf), buf.readVarInt())), b.readVarInt(), b.readVarInt()));
        @Override public Type<Snapshot> type() { return TYPE; }
        public Channel selected() { return channels.stream().filter(c -> c.id.equals(bound)).findFirst().orElse(null); }
    }

    public record Preview(int menu, UUID channel, GlobalPos target, boolean loaded, List<Integer> states)
            implements CustomPacketPayload {
        public static final Type<Preview> TYPE = new Type<>(ResourceLocation.parse("lyycore:power_channel_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Preview> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeUUID(p.channel); writePos(b, p.target); b.writeBoolean(p.loaded);
            b.writeCollection(p.states, (buf, state) -> buf.writeVarInt(state));
        }, b -> new Preview(b.readVarInt(), b.readUUID(), readPos(b), b.readBoolean(), b.readList(buf -> buf.readVarInt())));
        @Override public Type<Preview> type() { return TYPE; }
    }

    public static Channel entry(PowerChannels.Channel channel, UUID viewer) {
        return new Channel(channel.id, channel.name(), channel.isPublic(), channel.editableBy(viewer));
    }
    public static List<Target> targets(ServerPlayer player, List<GlobalPos> addresses) {
        return addresses.stream().map(address -> {
            var level = player.server.getLevel(address.dimension());
            int state = level != null && level.hasChunkAt(address.pos()) ? Block.getId(level.getBlockState(address.pos())) : -1;
            return new Target(address, state);
        }).toList();
    }
    public static Preview preview(ServerPlayer player, int menu, UUID channel, GlobalPos address) {
        var level = player.server.getLevel(address.dimension());
        var states = new ArrayList<Integer>(125);
        if (level == null) return new Preview(menu, channel, address, false, List.of());
        // Check the entire cube before reading any state; this must never load a chunk.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (!level.hasChunkAt(address.pos().offset(x, 0, z))) return new Preview(menu, channel, address, false, List.of());
        for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) for (int x = -2; x <= 2; x++)
            states.add(Block.getId(level.getBlockState(address.pos().offset(x, y, z))));
        return new Preview(menu, channel, address, true, states);
    }
    private static void writePos(FriendlyByteBuf buffer, GlobalPos pos) {
        buffer.writeResourceLocation(pos.dimension().location()); buffer.writeBlockPos(pos.pos());
    }
    private static GlobalPos readPos(FriendlyByteBuf buffer) {
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()), buffer.readBlockPos());
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
            if (player.containerMenu.containerId != request.menu || !player.containerMenu.stillValid(player)) return;
            if (player.containerMenu instanceof SpacetimeMatrixMenu menu) menu.handle(player, request);
            else if (player.containerMenu instanceof SpacetimeCoordinatesMenu menu) menu.handle(player, request);
        });
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (snapshot, context) -> {
            if (context.player().containerMenu.containerId != snapshot.menu) return;
            if (context.player().containerMenu instanceof SpacetimeMatrixMenu menu) menu.update(snapshot);
            else if (context.player().containerMenu instanceof SpacetimeCoordinatesMenu menu) menu.update(snapshot);
        });
        registrar.playToClient(Preview.TYPE, Preview.CODEC, (preview, context) -> {
            if (context.player().containerMenu instanceof SpacetimeCoordinatesMenu menu && menu.containerId == preview.menu)
                menu.updatePreview(preview);
            else if (context.player().containerMenu instanceof org.lyy.lyycore.content.menu.SpacetimeLocatorMenu menu && menu.containerId == preview.menu)
                menu.updatePreview(preview);
        });
    }
    private PowerChannelNetwork() { }
}
