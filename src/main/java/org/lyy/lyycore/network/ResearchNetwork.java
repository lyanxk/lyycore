package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.content.skills.StyleSystem;

@EventBusSubscriber(modid = "lyycore")
public final class ResearchNetwork {
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
        var registrar = event.registrar("1");
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
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync((ServerPlayer) event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync((ServerPlayer) event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync((ServerPlayer) event.getEntity()); }
}
