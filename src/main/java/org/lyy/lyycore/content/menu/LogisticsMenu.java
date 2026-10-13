package org.lyy.lyycore.content.menu;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.blockEntities.*;
import org.lyy.lyycore.content.logistics.LogisticsChannels;
import org.lyy.lyycore.network.LogisticsNetwork;
import org.lyy.lyycore.registry.LyyMenus;

public final class LogisticsMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 5;
    private final BlockEntity machine;
    private final Player viewer;
    private final int bufferSlots;
    private int page;
    private long revision = -1;
    private UUID lastBinding;
    private LogisticsNetwork.Snapshot snapshot;

    public LogisticsMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }
    public LogisticsMenu(int id, Inventory inventory, BlockEntity machine) {
        super(LyyMenus.LOGISTICS.get(), id);
        this.machine = machine; this.viewer = inventory.player;
        snapshot = new LogisticsNetwork.Snapshot(id, 0, 1, LogisticsNetwork.NONE, "", false, false, List.of());
        bufferSlots = machine instanceof MatrixPortBlockEntity ? 4 : 0;
        if (machine instanceof MatrixPortBlockEntity port) for (int i = 0; i < bufferSlots; i++) {
            addSlot(new BlockEntityItemSlot(port, port.items, i, 89 + i * 18, 160) {
                @Override public boolean mayPlace(ItemStack stack) { return !port.isOutput() && super.mayPlace(stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 44 + col * 18, 194 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 44 + col * 18, 252));
    }
    public boolean matrix() { return machine instanceof LogisticsMatrixBlockEntity; }
    public LogisticsNetwork.Snapshot snapshot() { return snapshot; }
    public void update(LogisticsNetwork.Snapshot value) { snapshot = value; }
    private GlobalPos address() { return GlobalPos.of(machine.getLevel().dimension(), machine.getBlockPos()); }
    public void handle(Player player, LogisticsNetwork.Request request) {
        if (!(player instanceof ServerPlayer server) || !stillValid(player) || player.isSpectator()) return;
        var directory = LogisticsChannels.get(server.serverLevel());
        if (matrix()) {
            switch (request.action()) {
                case CREATE -> directory.create(address(), player.getUUID(), request.name(), request.publicChannel());
                case RENAME -> directory.update(address(), player.getUUID(), request.channel(), request.name(), request.publicChannel());
                case DELETE -> directory.delete(address(), player.getUUID(), request.channel());
                default -> { }
            }
        } else if (machine instanceof MatrixPortBlockEntity port) {
            switch (request.action()) {
                case BIND -> {
                    var target = directory.channel(request.channel());
                    if (request.channel().equals(LogisticsNetwork.NONE)) port.bind(null);
                    else if (target != null && target.visibleTo(player.getUUID())) port.bind(target.id);
                }
                case PAGE -> page = Math.clamp(request.page(), 0, Math.max(0, (directory.visibleTo(player.getUUID()).size() - 1) / PAGE_SIZE));
                default -> { }
            }
        }
        revision = -1; broadcastChanges();
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof ServerPlayer server) || !stillValid(viewer)) return;
        var directory = LogisticsChannels.get(server.serverLevel());
        var binding = machine instanceof MatrixPortBlockEntity port ? port.channel() : null;
        if (revision == directory.revision() && Objects.equals(lastBinding, binding)) return;
        var channels = matrix() ? directory.atSource(address()).stream().filter(c -> c.visibleTo(viewer.getUUID())).toList()
                : directory.visibleTo(viewer.getUUID());
        int pages = Math.max(1, (channels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.clamp(page, 0, pages - 1);
        var bound = directory.channel(binding);
        if (bound != null && !bound.visibleTo(viewer.getUUID())) bound = null;
        var entries = channels.subList(page * PAGE_SIZE, Math.min(channels.size(), (page + 1) * PAGE_SIZE)).stream()
                .map(c -> new LogisticsNetwork.Entry(c.id, c.name(), c.source, c.isPublic(), c.editableBy(viewer.getUUID()))).toList();
        PacketDistributor.sendToPlayer(server, new LogisticsNetwork.Snapshot(containerId, page, pages,
                bound == null ? LogisticsNetwork.NONE : bound.id, bound == null ? "" : bound.name(),
                matrix() && directory.atSource(address()).size() < LogisticsChannels.CHANNEL_LIMIT, binding != null, entries));
        revision = directory.revision(); lastBinding = binding;
    }
    @Override public boolean stillValid(Player player) {
        return machine != null && !machine.isRemoved() && player.isAlive() && player.level() == machine.getLevel()
                && player.distanceToSqr(machine.getBlockPos().getCenter()) < (matrix() ? 144 : 64);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        boolean moved;
        if (index < bufferSlots) moved = moveItemStackTo(stack, bufferSlots, slots.size(), true);
        else if (bufferSlots > 0 && machine instanceof MatrixPortBlockEntity port && !port.isOutput())
            moved = moveItemStackTo(stack, 0, bufferSlots, false);
        else if (index < bufferSlots + 27) moved = moveItemStackTo(stack, bufferSlots + 27, slots.size(), false);
        else moved = moveItemStackTo(stack, bufferSlots, bufferSlots + 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }
}
