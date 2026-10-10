package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.content.research.ResearchDefinition;
import org.lyy.lyycore.content.research.ResearchEntry;
import org.lyy.lyycore.content.research.ResearchManager;
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
    public record OpenMemory() implements CustomPacketPayload {
        public static final Type<OpenMemory> TYPE = new Type<>(ResourceLocation.parse("lyycore:open_memory"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMemory> CODEC = StreamCodec.unit(new OpenMemory());
        @Override public Type<OpenMemory> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6");
        registrar.playToClient(Catalog.TYPE, Catalog.CODEC, (payload, context) -> ResearchManager.updateClient(payload.entries));
        registrar.playToServer(OpenMemory.TYPE, OpenMemory.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ResearchMenu.openMemory(player);
        });
    }
    @SubscribeEvent public static void datapackSync(OnDatapackSyncEvent event) {
        var catalog = new Catalog(List.copyOf(ResearchManager.all(event.getPlayerList().getServer().overworld())));
        event.getRelevantPlayers().forEach(player -> {
            if (player.containerMenu instanceof ResearchMenu || player.containerMenu instanceof org.lyy.lyycore.content.menu.ExperimentTableMenu)
                player.closeContainer();
            PacketDistributor.sendToPlayer(player, catalog);
        });
    }
}
