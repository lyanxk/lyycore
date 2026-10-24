package org.lyy.lyycore.network;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.logistics.LogisticsChannels;
import org.lyy.lyycore.content.menu.LogisticsMenu;

@EventBusSubscriber(modid = "lyycore")
public final class LogisticsNetwork {
    public static final UUID NONE = new UUID(0, 0);
    public enum Action { CREATE, RENAME, DELETE, BIND, PAGE }
    public record Entry(UUID id, String name, GlobalPos source, boolean publicChannel, boolean editable) { }
    public record Request(int menu, Action action, UUID channel, String name, int page, boolean publicChannel) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:logistics_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeEnum(p.action); b.writeUUID(p.channel);
            b.writeUtf(p.name, LogisticsChannels.NAME_LIMIT); b.writeVarInt(p.page); b.writeBoolean(p.publicChannel);
        }, b -> new Request(b.readVarInt(), b.readEnum(Action.class), b.readUUID(), b.readUtf(LogisticsChannels.NAME_LIMIT), b.readVarInt(), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Snapshot(int menu, int page, int pages, UUID bound, String boundName, boolean canCreate, boolean hasBinding, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.parse("lyycore:logistics_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeVarInt(p.page); b.writeVarInt(p.pages);
            b.writeUUID(p.bound); b.writeUtf(p.boundName, LogisticsChannels.NAME_LIMIT);
            b.writeBoolean(p.canCreate); b.writeBoolean(p.hasBinding);
            b.writeCollection(p.entries, (buf, entry) -> {
                buf.writeUUID(entry.id); buf.writeUtf(entry.name, LogisticsChannels.NAME_LIMIT);
                buf.writeResourceLocation(entry.source.dimension().location()); buf.writeBlockPos(entry.source.pos());
                buf.writeBoolean(entry.publicChannel); buf.writeBoolean(entry.editable);
            });
        }, b -> {
            int menu = b.readVarInt(), page = b.readVarInt(), pages = b.readVarInt();
            var bound = b.readUUID(); var name = b.readUtf(LogisticsChannels.NAME_LIMIT);
            boolean canCreate = b.readBoolean(), hasBinding = b.readBoolean();
            var entries = b.readList(buf -> new Entry(buf.readUUID(), buf.readUtf(LogisticsChannels.NAME_LIMIT),
                    GlobalPos.of(ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation()), buf.readBlockPos()), buf.readBoolean(), buf.readBoolean()));
            return new Snapshot(menu, page, pages, bound, name, canCreate, hasBinding, entries);
        });
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            var player = context.player();
            if (player.containerMenu instanceof LogisticsMenu menu && menu.containerId == payload.menu()) menu.handle(player, payload);
        });
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof LogisticsMenu menu && menu.containerId == payload.menu()) menu.update(payload);
        });
    }
    private LogisticsNetwork() { }
}
