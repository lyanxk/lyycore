package org.lyy.lyycore.content.menu;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.item.SpacetimeCollectionDeviceItem;
import org.lyy.lyycore.content.logistics.LogisticsChannels;
import org.lyy.lyycore.network.PowerChannelNetwork;
import org.lyy.lyycore.network.SpacetimeLocatorNetwork;
import org.lyy.lyycore.network.SpacetimeLocatorNetwork.*;
import org.lyy.lyycore.registry.LyyMenus;

public final class SpacetimeLocatorMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack tool;
    private long revision = -1;
    private Snapshot snapshot;
    private final Map<GlobalPos, PowerChannelNetwork.Preview> previews = new HashMap<>();
    public SpacetimeLocatorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readEnum(InteractionHand.class)); }
    public SpacetimeLocatorMenu(int id, Inventory inventory, InteractionHand hand) {
        super(LyyMenus.SPACETIME_LOCATOR.get(), id);
        owner = inventory.player; this.hand = hand; tool = owner.getItemInHand(hand);
        snapshot = new Snapshot(id, SpacetimeLocatorNetwork.NONE, false, false, -1, List.of(), List.of());
    }
    public Snapshot snapshot() { return snapshot; }
    public void update(Snapshot value) {
        if (!snapshot.bound().equals(value.bound())) previews.clear();
        snapshot = value;
        previews.keySet().removeIf(pos -> value.targets().stream().noneMatch(t -> t.previewPositions().contains(pos)));
    }
    public PowerChannelNetwork.Preview preview(GlobalPos address) { return previews.get(address); }
    public void updatePreview(PowerChannelNetwork.Preview value) {
        if (snapshot.bound().equals(value.channel()) && snapshot.targets().stream().anyMatch(t -> t.previewPositions().contains(value.target())))
            previews.put(value.target(), value);
    }
    public void handle(ServerPlayer player, Request request) {
        if (!stillValid(player)) return;
        var directory = LogisticsChannels.get(player.serverLevel());
        var id = SpacetimeCollectionDeviceItem.channel(tool);
        var selected = directory.channel(id);
        boolean output = SpacetimeCollectionDeviceItem.output(tool), area = SpacetimeCollectionDeviceItem.area(tool);
        if (request.action() == Action.BIND) {
            var destination = directory.channel(request.channel());
            if (destination == null || !destination.visibleTo(player.getUUID())) return;
            SpacetimeCollectionDeviceItem.configure(tool, destination.id, output, area);
        } else {
            if (!request.channel().equals(id == null ? SpacetimeLocatorNetwork.NONE : id)) return;
            switch (request.action()) {
                case INPUT, OUTPUT, AREA, BLOCK -> {
                    switch (request.action()) {
                        case INPUT -> output = false;
                        case OUTPUT -> { output = true; area = false; }
                        case AREA -> { output = false; area = true; }
                        case BLOCK -> area = false;
                        default -> { }
                    }
                    SpacetimeCollectionDeviceItem.configure(tool, id, output, area);
                }
                case REMOVE, PREVIEW -> {
                    if (selected == null || !selected.visibleTo(player.getUUID()) || request.revision() != directory.revision()
                            || request.output() != output || request.target() < 0 || request.target() >= snapshot.targets().size()) return;
                    var target = snapshot.targets().get(request.target());
                    if (request.action() == Action.PREVIEW) {
                        for (var address : target.previewPositions())
                            PacketDistributor.sendToPlayer(player, previews.computeIfAbsent(address,
                                    pos -> PowerChannelNetwork.preview(player, containerId, selected.id, pos)));
                        return;
                    }
                    if (target.area()) {
                        var areas = new ArrayList<>(selected.collectionAreas());
                        areas.remove(new LogisticsChannels.CollectionArea(target.first(), target.second()));
                        directory.setCollectionAreas(id, areas);
                    } else {
                        var blocks = new ArrayList<>(output ? selected.outputTargets() : selected.inputTargets());
                        blocks.remove(new LogisticsChannels.BlockTarget(target.first(), target.side()));
                        directory.setBlockTargets(id, output, blocks);
                    }
                }
                default -> { return; }
            }
        }
        player.getInventory().setChanged(); revision = -1; broadcastChanges();
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(owner instanceof ServerPlayer player) || !stillValid(player)) return;
        var directory = LogisticsChannels.get(player.serverLevel());
        if (revision == directory.revision()) return;
        var id = SpacetimeCollectionDeviceItem.channel(tool);
        var channel = directory.channel(id);
        boolean output = SpacetimeCollectionDeviceItem.output(tool), area = SpacetimeCollectionDeviceItem.area(tool);
        if (channel == null || !channel.visibleTo(player.getUUID())) {
            channel = null; id = null;
            if (SpacetimeCollectionDeviceItem.channel(tool) != null) {
                SpacetimeCollectionDeviceItem.configure(tool, null, output, area); player.getInventory().setChanged();
            }
        }
        var targets = new ArrayList<Target>();
        if (channel != null) {
            if (!output) for (var region : channel.collectionAreas()) targets.add(new Target(region.first(), region.second(), null, -1));
            for (var block : output ? channel.outputTargets() : channel.inputTargets()) {
                var address = block.position(); var level = player.server.getLevel(address.dimension());
                int state = level != null && level.hasChunkAt(address.pos()) ? Block.getId(level.getBlockState(address.pos())) : -1;
                targets.add(new Target(address, null, block.side(), state));
            }
        }
        var channels = directory.visibleTo(player.getUUID()).stream().map(c -> new Channel(c.id, c.name(), c.isPublic())).toList();
        update(new Snapshot(containerId, id == null ? SpacetimeLocatorNetwork.NONE : id, output, area, directory.revision(), channels, targets));
        PacketDistributor.sendToPlayer(player, snapshot); revision = directory.revision();
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator() && player.getItemInHand(hand) == tool
                && tool.getItem() instanceof SpacetimeCollectionDeviceItem;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
