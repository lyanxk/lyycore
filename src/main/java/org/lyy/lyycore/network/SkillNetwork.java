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
import org.lyy.lyycore.content.skills.*;
import java.util.Map;
import java.util.WeakHashMap;

/** Skill input, style selection and complete client-facing skill snapshots. */
@EventBusSubscriber(modid = "lyycore")
public final class SkillNetwork {
    private static final Map<ServerPlayer, State> SENT = new WeakHashMap<>();
    public record Hover(long until) implements CustomPacketPayload {
        public static final Type<Hover> TYPE = new Type<>(ResourceLocation.parse("lyycore:skill_hover"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hover> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeLong(value.until), buf -> new Hover(buf.readLong()));
        @Override public Type<Hover> type() { return TYPE; }
    }
    public record State(boolean unlocked, StyleSystem.Style style, boolean explored, int guard, boolean guarding, boolean doubleJump, boolean speedUp) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.parse("lyycore:skill_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buffer, state) -> { buffer.writeBoolean(state.unlocked); buffer.writeEnum(state.style); buffer.writeBoolean(state.explored); buffer.writeVarInt(state.guard); buffer.writeBoolean(state.guarding); buffer.writeBoolean(state.doubleJump); buffer.writeBoolean(state.speedUp); },
                buffer -> new State(buffer.readBoolean(), buffer.readEnum(StyleSystem.Style.class), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));
        @Override public Type<State> type() { return TYPE; }
    }
    public record DoubleJumpRequest() implements CustomPacketPayload {
        public static final Type<DoubleJumpRequest> TYPE = new Type<>(ResourceLocation.parse("lyycore:double_jump"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpRequest> CODEC = StreamCodec.unit(new DoubleJumpRequest());
        @Override public Type<DoubleJumpRequest> type() { return TYPE; }
    }
    public record Input(boolean held, int forward, int strafe) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.parse("lyycore:skill_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of((buf, input) -> {
            buf.writeBoolean(input.held); buf.writeByte(input.forward); buf.writeByte(input.strafe);
        }, buf -> new Input(buf.readBoolean(), buf.readByte(), buf.readByte()));
        @Override public Type<Input> type() { return TYPE; }
    }
    public record NextStyle() implements CustomPacketPayload {
        public static final Type<NextStyle> TYPE = new Type<>(ResourceLocation.parse("lyycore:next_style"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NextStyle> CODEC = StreamCodec.unit(new NextStyle());
        @Override public Type<NextStyle> type() { return TYPE; }
    }
    public record SelectStyle(StyleSystem.Style style) implements CustomPacketPayload {
        public static final Type<SelectStyle> TYPE = new Type<>(ResourceLocation.parse("lyycore:select_style"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectStyle> CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeEnum(payload.style),
                buffer -> new SelectStyle(buffer.readEnum(StyleSystem.Style.class)));
        @Override public Type<SelectStyle> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("8");
        registrar.playToServer(DoubleJumpRequest.TYPE, DoubleJumpRequest.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) DoubleJump.jump(player);
        });
        registrar.playToClient(Hover.TYPE, Hover.CODEC, (payload, context) ->
                context.player().getPersistentData().putLong("lyycore:hover_until", payload.until));
        registrar.playToServer(Input.TYPE, Input.CODEC, (input, context) -> {
            if (context.player() instanceof ServerPlayer player) SkillInput.accept(player, input.held, input.forward, input.strafe);
        });
        registrar.playToServer(NextStyle.TYPE, NextStyle.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) selectStyle(player, StyleSystem.current(player).next());
        });
        registrar.playToServer(SelectStyle.TYPE, SelectStyle.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) selectStyle(player, payload.style);
        });
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) -> {
            SkillSystem.setUnlocked(context.player(), payload.unlocked);
            context.player().getPersistentData().putBoolean("lyycore:double_jump", payload.doubleJump);
            context.player().getPersistentData().putBoolean("lyycore:speed_up", payload.speedUp);
            StyleSystem.select(context.player(), payload.style);
            context.player().getPersistentData().putBoolean("lyycore:exploration", payload.explored);
            GuardSkill.setValue(context.player(), payload.guard);
            context.player().getPersistentData().putBoolean("lyycore:guarding", payload.guarding);
        });
    }
    private static void selectStyle(ServerPlayer player, StyleSystem.Style style) {
        if (!SkillSystem.unlocked(player) || !player.isAlive() || player.isSpectator()
                || player.containerMenu != player.inventoryMenu || StyleSystem.current(player) == style) return;
        StyleSystem.select(player, style);
        GuardSkill.reset(player);
        BuildingSkills.updateFlight(player);
        sync(player);
    }
    public static void sync(ServerPlayer player) {
        var state = new State(SkillSystem.unlocked(player), StyleSystem.current(player), BasicSkills.available(player), GuardSkill.value(player), GuardSkill.guarding(player),
                org.lyy.lyycore.content.ResearchProgress.completed(player, DoubleJump.RESEARCH), org.lyy.lyycore.content.ResearchProgress.completed(player, org.lyy.lyycore.content.AegisWings.SPEED_UP));
        var previous = SENT.put(player, state);
        if (!state.equals(previous)) PacketDistributor.sendToPlayer(player, state);
        if (previous == null || previous.guarding != state.guarding) WingsNetwork.sync(player);
    }
    public static void forget(ServerPlayer player) { SENT.remove(player); }
    public static void clear() { SENT.clear(); }
    private static void initial(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { forget(player); sync(player); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { initial(event); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { initial(event); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { initial(event); }
}
