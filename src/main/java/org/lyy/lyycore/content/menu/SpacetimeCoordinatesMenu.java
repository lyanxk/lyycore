package org.lyy.lyycore.content.menu;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.energy.PowerChannels;
import org.lyy.lyycore.content.item.SpacetimeCoordinateDeviceItem;
import org.lyy.lyycore.network.PowerChannelNetwork;
import org.lyy.lyycore.registry.LyyMenus;

public final class SpacetimeCoordinatesMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack tool;
    private long revision = -1;
    private UUID lastBinding;
    private List<GlobalPos> lastPending = List.of();
    private PowerChannelNetwork.Snapshot snapshot;
    private final Map<GlobalPos, PowerChannelNetwork.Preview> previews = new HashMap<>();

    public SpacetimeCoordinatesMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readEnum(InteractionHand.class)); }
    public SpacetimeCoordinatesMenu(int id, Inventory inventory, InteractionHand hand) {
        super(LyyMenus.SPACETIME_COORDINATES.get(), id);
        owner = inventory.player; this.hand = hand; tool = owner.getItemInHand(hand);
        snapshot = new PowerChannelNetwork.Snapshot(id, PowerChannelNetwork.NONE, List.of(), List.of(), 0, 0);
    }
    public PowerChannelNetwork.Snapshot snapshot() { return snapshot; }
    public void update(PowerChannelNetwork.Snapshot value) {
        if (!value.bound().equals(snapshot.bound())) previews.clear();
        snapshot = value;
        previews.keySet().removeIf(address -> value.targets().stream().noneMatch(t -> t.address().equals(address)));
    }
    public PowerChannelNetwork.Preview preview(GlobalPos address) { return previews.get(address); }
    public void updatePreview(PowerChannelNetwork.Preview preview) {
        if (preview.channel().equals(snapshot.bound()) && snapshot.targets().stream().anyMatch(t -> t.address().equals(preview.target())))
            previews.put(preview.target(), preview);
    }
    public void handle(ServerPlayer player, PowerChannelNetwork.Request request) {
        if (!stillValid(player)) return;
        var directory = PowerChannels.get(player.serverLevel());
        var selected = directory.channel(SpacetimeCoordinateDeviceItem.channel(tool));
        if (selected != null && !selected.visibleTo(player.getUUID())) selected = null;
        var addresses = selected == null ? SpacetimeCoordinateDeviceItem.pending(tool) : selected.targets();
        switch (request.action()) {
            case BIND -> {
                previews.clear();
                if (request.channel().equals(PowerChannelNetwork.NONE)) SpacetimeCoordinateDeviceItem.channel(tool, null);
                else SpacetimeCoordinateDeviceItem.bind(player, tool, request.channel());
            }
            case REMOVE -> {
                if (!Objects.equals(request.channel(), selected == null ? PowerChannelNetwork.NONE : selected.id)
                        || request.target() == null || !addresses.contains(request.target())) return;
                if (selected != null) directory.removeTarget(selected.id, player.getUUID(), request.target());
                else { var pending = new ArrayList<>(addresses); pending.remove(request.target()); SpacetimeCoordinateDeviceItem.pending(tool, pending); }
                previews.remove(request.target());
            }
            case COPY -> {
                if (selected != null && !selected.id.equals(request.channel())) {
                    var destination = directory.channel(request.channel());
                    if (destination == null || !destination.visibleTo(player.getUUID())) return;
                    directory.copyTargets(selected.id, destination.id, player.getUUID());
                    var destinationReceivers = destination.targets().stream().map(address -> PowerChannels.receiver(player.serverLevel(), address)).toList();
                    boolean full = selected.targets().stream().map(address -> PowerChannels.receiver(player.serverLevel(), address)).anyMatch(address -> !destinationReceivers.contains(address));
                    player.displayClientMessage(Component.translatable("message.lyycore.spacetime_coordinates." + (full ? "full" : "copied")), true);
                }
            }
            case PREVIEW -> {
                if (!Objects.equals(request.channel(), selected == null ? PowerChannelNetwork.NONE : selected.id)
                        || request.target() == null || !addresses.contains(request.target())) return;
                var address = request.target();
                var preview = previews.computeIfAbsent(address, pos -> PowerChannelNetwork.preview(player, containerId, request.channel(), pos));
                PacketDistributor.sendToPlayer(player, preview);
                return;
            }
            default -> { return; }
        }
        player.getInventory().setChanged(); revision = -1; broadcastChanges();
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(owner instanceof ServerPlayer player) || !stillValid(player)) return;
        var directory = PowerChannels.get(player.serverLevel());
        var binding = SpacetimeCoordinateDeviceItem.channel(tool);
        var channel = directory.channel(binding);
        if (channel == null || !channel.visibleTo(player.getUUID())) {
            channel = null;
            if (binding != null) { SpacetimeCoordinateDeviceItem.channel(tool, null); player.getInventory().setChanged(); binding = null; }
        }
        var pending = SpacetimeCoordinateDeviceItem.pending(tool);
        if (revision == directory.revision() && Objects.equals(lastBinding, binding) && lastPending.equals(pending)) return;
        var channels = directory.visibleTo(player.getUUID()).stream().map(c -> PowerChannelNetwork.entry(c, player.getUUID())).toList();
        PacketDistributor.sendToPlayer(player, new PowerChannelNetwork.Snapshot(containerId, binding == null ? PowerChannelNetwork.NONE : binding,
                channels, PowerChannelNetwork.targets(player, channel == null ? pending : channel.targets()), 0, 0));
        revision = directory.revision(); lastBinding = binding; lastPending = pending;
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator() && player.getItemInHand(hand) == tool
                && tool.getItem() instanceof SpacetimeCoordinateDeviceItem;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
