package org.lyy.lyycore.content.finality;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "lyycore")
public final class FinalityNetwork {
    public record Status(int progress, int layers) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(FinalityContent.id("finality_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC =
                StreamCodec.of(
                        (b, p) -> {
                            b.writeVarInt(p.progress);
                            b.writeVarInt(p.layers);
                        },
                        b -> new Status(b.readVarInt(), b.readVarInt()));

        @Override
        public Type<Status> type() {
            return TYPE;
        }
    }

    public record Ghost(int menu, ItemStack item) implements CustomPacketPayload {
        public static final Type<Ghost> TYPE = new Type<>(FinalityContent.id("genesis_ghost"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ghost> CODEC =
                StreamCodec.of(
                        (b, p) -> {
                            b.writeVarInt(p.menu);
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(b, p.item);
                        },
                        b -> new Ghost(b.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(b)));

        @Override
        public Type<Ghost> type() {
            return TYPE;
        }
    }

    public static void status(ServerPlayer player, int progress, int layers) {
        PacketDistributor.sendToPlayer(player, new Status(progress, layers));
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                Status.TYPE,
                Status.CODEC,
                (p, c) -> org.lyy.lyycore.client.finality.FinalityClient.status(p));
        registrar.playToServer(
                Ghost.TYPE,
                Ghost.CODEC,
                (p, c) -> {
                    if (c.player().containerMenu instanceof GenesisMenu menu
                            && menu.containerId == p.menu()) menu.ghost(p.item(), c.player());
                });
    }
}
