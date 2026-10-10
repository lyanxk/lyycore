package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.wings.WingsFlight;

/** Sync wing state changes, guard starts and attacks, never per-tick animation packets. */
@EventBusSubscriber(modid = "lyycore")
public final class WingsNetwork {
    public record Pursuit(int playerId, int targetId, long started, Vec3 target) implements CustomPacketPayload {
        public static final Type<Pursuit> TYPE = new Type<>(ResourceLocation.parse("lyycore:wings_pursuit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pursuit> CODEC = StreamCodec.of((buf, value) -> {
            buf.writeVarInt(value.playerId); buf.writeVarInt(value.targetId); buf.writeLong(value.started); buf.writeVec3(value.target);
        }, buf -> new Pursuit(buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readVec3()));
        @Override public Type<Pursuit> type() { return TYPE; }
    }
    public static void pursuit(ServerPlayer player, LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new Pursuit(player.getId(), target.getId(), player.level().getGameTime(), target.getBoundingBox().getCenter()));
    }
    public record Scoop(int playerId, long started, Vec3 origin, float yaw) implements CustomPacketPayload {
        public static final Type<Scoop> TYPE = new Type<>(ResourceLocation.parse("lyycore:wings_scoop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scoop> CODEC = StreamCodec.of((buf, value) -> {
            buf.writeVarInt(value.playerId); buf.writeLong(value.started); buf.writeVec3(value.origin); buf.writeFloat(value.yaw);
        }, buf -> new Scoop(buf.readVarInt(), buf.readLong(), buf.readVec3(), buf.readFloat()));
        @Override public Type<Scoop> type() { return TYPE; }
    }
    public static void scoop(ServerPlayer player, float yaw) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new Scoop(player.getId(), player.level().getGameTime(), player.position(), yaw));
    }
    public record Boost(boolean held) implements CustomPacketPayload {
        public static final Type<Boost> TYPE = new Type<>(ResourceLocation.parse("lyycore:wings_boost"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Boost> CODEC = StreamCodec.of((buf, value) -> buf.writeBoolean(value.held), buf -> new Boost(buf.readBoolean()));
        @Override public Type<Boost> type() { return TYPE; }
    }
    public record Attack(int playerId, long started, Vec3 origin, float yaw, Vec3 target, float width, int featherCount) implements CustomPacketPayload {
        public static final Type<Attack> TYPE = new Type<>(ResourceLocation.parse("lyycore:feather_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Attack> CODEC = StreamCodec.of((buf, attack) -> {
            buf.writeVarInt(attack.playerId); buf.writeLong(attack.started); buf.writeVec3(attack.origin);
            buf.writeFloat(attack.yaw); buf.writeVec3(attack.target); buf.writeFloat(attack.width); buf.writeVarInt(attack.featherCount);
        }, buf -> new Attack(buf.readVarInt(), buf.readLong(), buf.readVec3(), buf.readFloat(), buf.readVec3(), buf.readFloat(), buf.readVarInt()));
        @Override public Type<Attack> type() { return TYPE; }
    }
    public static void attack(ServerPlayer player, LivingEntity target, int featherCount) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Attack(player.getId(), player.level().getGameTime(),
                player.position(), player.yBodyRot, target.getBoundingBox().getCenter(), target.getBbWidth(), featherCount));
    }
    public record State(int entityId, int level, boolean visible, long shieldStarted, boolean guarding, boolean boosting, boolean flight, int acceleration) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.parse("lyycore:wings_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((buf, state) -> {
            buf.writeVarInt(state.entityId); buf.writeVarInt(state.level); buf.writeBoolean(state.visible); buf.writeLong(state.shieldStarted); buf.writeBoolean(state.guarding); buf.writeBoolean(state.boosting); buf.writeBoolean(state.flight); buf.writeVarInt(state.acceleration);
        }, buf -> new State(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readLong(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        @Override public Type<State> type() { return TYPE; }
    }
    private static State state(Player player) {
        return new State(player.getId(), AegisWings.level(player), AegisWings.visible(player), AegisWings.shieldStarted(player),
                player instanceof ServerPlayer server && org.lyy.lyycore.content.skills.GuardSkill.guarding(server), WingsFlight.boosting(player), org.lyy.lyycore.content.wings.WingsSettings.flight(player), org.lyy.lyycore.content.wings.WingsSettings.acceleration(player));
    }
    public static void sync(ServerPlayer player) {
        WingsFlight.update(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state(player));
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("5").playToClient(Pursuit.TYPE, Pursuit.CODEC, (payload, context) -> org.lyy.lyycore.client.WingsPursuitRenderer.add(payload));
        event.registrar("5").playToClient(Scoop.TYPE, Scoop.CODEC, (payload, context) -> org.lyy.lyycore.client.WingsScoopRenderer.add(payload));
        event.registrar("5").playToServer(Boost.TYPE, Boost.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WingsFlight.input(player, payload.held);
        });
        event.registrar("5").playToClient(Attack.TYPE, Attack.CODEC, (payload, context) -> org.lyy.lyycore.client.FeatherAttackRenderer.add(payload));
        event.registrar("5").playToClient(State.TYPE, State.CODEC, (payload, context) -> {
            if (context.player().level().getEntity(payload.entityId) instanceof Player player) {
                AegisWings.receive(player, payload.level, payload.visible, payload.shieldStarted);
                org.lyy.lyycore.content.wings.WingsSettings.set(player, 0, payload.flight, payload.acceleration);
                player.getPersistentData().putBoolean("lyycore:wings_guarding", payload.guarding);
                player.getPersistentData().putBoolean("lyycore:wings_boosting", payload.boosting);
            }
        });
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof Player player)
            PacketDistributor.sendToPlayer(observer, state(player));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync((ServerPlayer) event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync((ServerPlayer) event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync((ServerPlayer) event.getEntity()); }
}
