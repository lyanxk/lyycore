package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.content.research.ResearchDefinition;
import org.lyy.lyycore.content.research.ResearchEntry;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.content.skills.StyleSystem;
import java.util.List;

@EventBusSubscriber(modid = "lyycore")
public final class ResearchNetwork {
    public record Catalog(List<ResearchEntry> entries) implements CustomPacketPayload {
        public Catalog { entries = List.copyOf(entries); }
        public static final Type<Catalog> TYPE = new Type<>(ResourceLocation.parse("lyycore:research_catalog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Catalog> CODEC = StreamCodec.of(
                (buffer, catalog) -> buffer.writeCollection(catalog.entries, (buf, entry) -> {
                    buf.writeResourceLocation(entry.id());
                    ResearchDefinition.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, entry.value());
                }), buffer -> new Catalog(buffer.readList(buf -> new ResearchEntry(buf.readResourceLocation(),
                        ResearchDefinition.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf)))));
        @Override public Type<Catalog> type() { return TYPE; }
    }
    public enum Action { MEMORY, NEXT_STYLE, SPECIAL }
    public record Request(Action action) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:research_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buffer, request) -> buffer.writeEnum(request.action), buffer -> new Request(buffer.readEnum(Action.class)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record State(boolean unlocked, StyleSystem.Style style) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.parse("lyycore:skill_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buffer, state) -> { buffer.writeBoolean(state.unlocked); buffer.writeEnum(state.style); },
                buffer -> new State(buffer.readBoolean(), buffer.readEnum(StyleSystem.Style.class)));
        @Override public Type<State> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(Catalog.TYPE, Catalog.CODEC, (payload, context) -> ResearchManager.updateClient(payload.entries));
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case MEMORY -> ResearchMenu.openMemory(player);
                case SPECIAL -> SkillSystem.cast(player, SkillSystem.SPECIAL);
                case NEXT_STYLE -> {
                    if (SkillSystem.unlocked(player) && player.isAlive() && !player.isSpectator()
                            && player.containerMenu == player.inventoryMenu) {
                        StyleSystem.select(player, StyleSystem.current(player).next());
                        sync(player);
                    }
                }
            }
        });
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) -> {
            SkillSystem.setUnlocked(context.player(), payload.unlocked);
            StyleSystem.select(context.player(), payload.style);
        });
    }
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new State(SkillSystem.unlocked(player), StyleSystem.current(player)));
    }
    @SubscribeEvent public static void datapackSync(OnDatapackSyncEvent event) {
        var catalog = new Catalog(List.copyOf(ResearchManager.all(event.getPlayerList().getServer().overworld())));
        event.getRelevantPlayers().forEach(player -> {
            if (player.containerMenu instanceof ResearchMenu) player.closeContainer();
            PacketDistributor.sendToPlayer(player, catalog);
        });
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync((ServerPlayer) event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync((ServerPlayer) event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync((ServerPlayer) event.getEntity()); }
}
