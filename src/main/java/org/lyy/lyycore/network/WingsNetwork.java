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

/** Send only unlock/preference changes and hits, never per-tick animation packets. */
@EventBusSubscriber(modid = "lyycore")
public final class WingsNetwork {
    public record Attack(int playerId, long started, Vec3 origin, float yaw, Vec3 target, float width) implements CustomPacketPayload {
        public static final Type<Attack> TYPE = new Type<>(ResourceLocation.parse("lyycore:feather_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Attack> CODEC = StreamCodec.of((buf, attack) -> {
            buf.writeVarInt(attack.playerId); buf.writeLong(attack.started); buf.writeVec3(attack.origin);
            buf.writeFloat(attack.yaw); buf.writeVec3(attack.target); buf.writeFloat(attack.width);
        }, buf -> new Attack(buf.readVarInt(), buf.readLong(), buf.readVec3(), buf.readFloat(), buf.readVec3(), buf.readFloat()));
        @Override public Type<Attack> type() { return TYPE; }
    }
    public static void attack(ServerPlayer player, LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Attack(player.getId(), player.level().getGameTime(),
                player.position(), player.yBodyRot, target.getBoundingBox().getCenter(), target.getBbWidth()));
    }
    public record State(int entityId, boolean unlocked, boolean visible, long shieldStarted) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.parse("lyycore:wings_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((buf, state) -> {
            buf.writeVarInt(state.entityId); buf.writeBoolean(state.unlocked); buf.writeBoolean(state.visible); buf.writeLong(state.shieldStarted);
        }, buf -> new State(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readLong()));
        @Override public Type<State> type() { return TYPE; }
    }
    private static State state(Player player) {
        return new State(player.getId(), AegisWings.unlocked(player), AegisWings.visible(player), AegisWings.shieldStarted(player));
    }
    public static void sync(ServerPlayer player) { PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state(player)); }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Attack.TYPE, Attack.CODEC, (payload, context) -> org.lyy.lyycore.client.FeatherAttackRenderer.add(payload));
        event.registrar("1").playToClient(State.TYPE, State.CODEC, (payload, context) -> {
            if (context.player().level().getEntity(payload.entityId) instanceof Player player)
                AegisWings.receive(player, payload.unlocked, payload.visible, payload.shieldStarted);
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
