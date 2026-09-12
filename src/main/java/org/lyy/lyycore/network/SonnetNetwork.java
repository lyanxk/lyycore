package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.item.SonnetBowItem;
import org.lyy.lyycore.registry.LyyEntities;

@EventBusSubscriber(modid = LyyCore.MODID)
public final class SonnetNetwork {
    public record Ultimate() implements CustomPacketPayload {
        public static final Ultimate INSTANCE = new Ultimate();
        public static final Type<Ultimate> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "sonnet_ultimate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ultimate> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<Ultimate> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Ultimate.TYPE, Ultimate.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) activate(player);
        });
    }
    private static org.lyy.lyycore.content.entity.SonnetDome findOwnedDome(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.hasUUID("SonnetActiveDome")) {
            var id = data.getUUID("SonnetActiveDome");
            for (var level : player.server.getAllLevels()) {
                if (level.getEntity(id) instanceof org.lyy.lyycore.content.entity.SonnetDome dome
                        && dome.isValid() && dome.belongsTo(player)) return dome;
            }
            data.remove("SonnetActiveDome");
        }
        // Also recognize domes created before the active-dome player tag was introduced.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var dome = SonnetBowItem.resolveDome(player.getInventory().getItem(slot), player.serverLevel());
            if (dome != null && dome.belongsTo(player)) return dome;
        }
        return null;
    }

    public static void activate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.hasEffect(org.lyy.lyycore.registry.LyyEffects.CRYSTALLIZATION)) return;
        var existing = findOwnedDome(player);
        if (existing != null) {
            existing.recall(player);
            return;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SonnetBowItem)) stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof SonnetBowItem) || SonnetBowItem.resolveDome(stack, player.serverLevel()) != null) return;
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("SonnetUltimateUntil") > now) return;
        var dome = LyyEntities.SONNET_DOME.get().create(player.level());
        var arrow = LyyEntities.CRYSTAL_ARROW.get().create(player.level());
        if (dome == null || arrow == null) return;
        dome.prepare(player);
        arrow.setOwner(player);
        arrow.setPos(player.getEyePosition());
        arrow.launchDome(dome);
        arrow.setDeltaMovement(0, SonnetBowItem.ARROW_SPEED, 0);
        player.stopUsingItem();
        player.level().addFreshEntity(dome);
        SonnetBowItem.bind(stack, dome);
        player.getPersistentData().putUUID("SonnetActiveDome", dome.getUUID());
        player.level().addFreshEntity(arrow);
        player.getPersistentData().putLong("SonnetUltimateUntil", now + 310);
    }
}
