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
import org.lyy.lyycore.content.skills.BasicSkills;
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
    public enum Action { MEMORY, NEXT_STYLE }
    public record Request(Action action) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:research_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buffer, request) -> buffer.writeEnum(request.action), buffer -> new Request(buffer.readEnum(Action.class)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record State(boolean unlocked, StyleSystem.Style style, boolean explored, int guard, boolean guarding) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.parse("lyycore:skill_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buffer, state) -> { buffer.writeBoolean(state.unlocked); buffer.writeEnum(state.style); buffer.writeBoolean(state.explored); buffer.writeVarInt(state.guard); buffer.writeBoolean(state.guarding); },
                buffer -> new State(buffer.readBoolean(), buffer.readEnum(StyleSystem.Style.class), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean()));
        @Override public Type<State> type() { return TYPE; }
    }
    public record Input(boolean held, int forward, int strafe) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.parse("lyycore:skill_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of((buf, input) -> {
            buf.writeBoolean(input.held); buf.writeByte(input.forward); buf.writeByte(input.strafe);
        }, buf -> new Input(buf.readBoolean(), buf.readByte(), buf.readByte()));
        @Override public Type<Input> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(Input.TYPE, Input.CODEC, (input, context) -> {
            if (context.player() instanceof ServerPlayer player) BasicSkills.input(player, input.held, input.forward, input.strafe);
        });
        registrar.playToClient(Catalog.TYPE, Catalog.CODEC, (payload, context) -> ResearchManager.updateClient(payload.entries));
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case MEMORY -> ResearchMenu.openMemory(player);
                case NEXT_STYLE -> {
                    if (SkillSystem.unlocked(player) && player.isAlive() && !player.isSpectator()
                            && player.containerMenu == player.inventoryMenu) {
                        StyleSystem.select(player, StyleSystem.current(player).next());
                        BasicSkills.resetInput(player);
                        BasicSkills.updateFlight(player);
                        sync(player);
                    }
                }
            }
        });
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) -> {
            SkillSystem.setUnlocked(context.player(), payload.unlocked);
            StyleSystem.select(context.player(), payload.style);
            context.player().getPersistentData().putBoolean("lyycore:exploration", payload.explored);
            BasicSkills.setGuard(context.player(), payload.guard);
            context.player().getPersistentData().putBoolean("lyycore:guarding", payload.guarding);
        });
    }
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new State(SkillSystem.unlocked(player), StyleSystem.current(player), BasicSkills.available(player), BasicSkills.guard(player), BasicSkills.guarding(player)));
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
