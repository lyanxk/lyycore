package org.lyy.lyycore.content.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.research.ResearchEntry;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.content.research.ResearchProgress;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.registry.*;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import org.lyy.lyycore.content.item.ResearchNotesItem;

public final class ResearchMenu extends AbstractContainerMenu {
    public static final int READY = 0, COMPLETED = 1, MISSING_COST = 2, UNCHECKED = 3, LOCKED = 4;
    public static final int CLOSE_PREVIEW = -1;
    public static final int TOGGLE_WINGS = Integer.MIN_VALUE;
    private final Player owner;
    private final ContainerLevelAccess access;
    private final List<ResearchEntry> entries;
    private final int[] statuses;
    private final boolean memory;
    public final SelfMenuData self;
    private final DataSlot wingsVisible = DataSlot.standalone();
    private int preview = -1;

    public ResearchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), readEntries(inventory.player, buffer), false);
    }

    private ResearchMenu(int id, Inventory inventory, BlockPos pos, List<ResearchEntry> entries, boolean memory) {
        super(memory ? LyyMenus.MEMORY.get() : LyyMenus.RESEARCH.get(), id);
        this.owner = inventory.player;
        this.memory = memory;
        this.self = new SelfMenuData(owner);
        if (memory) addDataSlots(self);
        this.access = memory ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(inventory.player.level(), pos);
        this.entries = List.copyOf(entries);
        wingsVisible.set(AegisWings.visible(owner) ? 1 : 0);
        addDataSlot(wingsVisible);
        statuses = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            statuses[i] = memory ? COMPLETED : initialStatus(i);
            addDataSlot(DataSlot.shared(statuses, i));
        }
    }
    private static List<ResearchEntry> available(Player player, boolean completedOnly) {
        return ResearchManager.all(player.level()).stream()
                .filter(entry -> ResearchProgress.completed(player, entry.id()) == completedOnly)
                .sorted(Comparator.comparingInt((ResearchEntry entry) -> entry.value().display().stage())
                        .thenComparing(entry -> entry.value().display().purpose())
                        .thenComparingInt(entry -> entry.value().display().order())
                        .thenComparing(entry -> entry.id().toString())).toList();
    }
    private static List<ResearchEntry> readEntries(Player player, RegistryFriendlyByteBuf buffer) {
        // Resolve only the server-selected IDs, preserving their order for button/status indices.
        return buffer.readList(buf -> ResearchManager.get(player.level(), buf.readResourceLocation()));
    }
    public static ResearchMenu memory(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new ResearchMenu(id, inventory, BlockPos.ZERO, readEntries(inventory.player, buffer), true);
    }
    public static void openTable(ServerPlayer player, BlockPos pos) {
        var entries = available(player, false);
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                new ResearchMenu(id, inventory, pos, entries, false), Component.translatable("block.lyycore.imaginary_research_table")),
                buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeCollection(entries, (buf, entry) -> buf.writeResourceLocation(entry.id()));
                });
    }
    public static void openMemory(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu) return;
        var entries = available(player, true);
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                new ResearchMenu(id, inventory, BlockPos.ZERO, entries, true), Component.translatable("gui.lyycore.memory.title")),
                buffer -> buffer.writeCollection(entries, (buf, entry) -> buf.writeResourceLocation(entry.id())));
    }
    public boolean isMemory() { return memory; }
    public List<ResearchEntry> entries() { return entries; }
    public int status(int index) { return statuses[index]; }
    public boolean wingsVisible() { return wingsVisible.get() != 0; }

    /** Nonnegative buttons submit research; negative buttons only manage the cost preview. */
    public static int previewButton(int index) { return -index - 2; }

    private int initialStatus(int index) {
        var entry = entries.get(index);
        if (ResearchProgress.completed(owner, entry.id())) return COMPLETED;
        return ResearchProgress.available(owner, entry.value()) ? UNCHECKED : LOCKED;
    }

    private void closePreview() {
        if (preview >= 0) statuses[preview] = initialStatus(preview);
        preview = -1;
    }

    private void refreshPreview() {
        if (preview < 0) return;
        int status = initialStatus(preview);
        statuses[preview] = status == UNCHECKED
                ? (ResearchProgress.canAfford(owner, entries.get(preview).value()) ? READY : MISSING_COST) : status;
    }

    @Override public void broadcastChanges() {
        // Unlock changes need no inventory scan; refresh the overview once per second.
        if (!memory && !owner.level().isClientSide && owner.tickCount % 20 == 0)
            for (int i = 0; i < statuses.length; i++) if (i != preview) statuses[i] = initialStatus(i);
        if (!memory && !owner.level().isClientSide && owner.tickCount % 5 == 0) refreshPreview();
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || player != owner || !stillValid(player)) return false;
        if (memory && id <= -10000 && id >= -10300) {
            boolean changed = self.click(server, id); broadcastChanges(); return changed;
        }
        if (id == TOGGLE_WINGS) {
            if (!memory || !AegisWings.unlocked(player) || entries.stream().noneMatch(entry -> entry.id().equals(AegisWings.RESEARCH))) return false;
            AegisWings.setVisible(player, !AegisWings.visible(player));
            wingsVisible.set(AegisWings.visible(player) ? 1 : 0);
            WingsNetwork.sync(server);
            broadcastChanges();
            return true;
        }
        if (id < 0) {
            if (memory) return false;
            if (id == CLOSE_PREVIEW) {
                closePreview();
            } else {
                int index = -id - 2;
                if (index < 0 || index >= entries.size()) return false;
                closePreview();
                preview = index;
                refreshPreview();
            }
            broadcastChanges();
            return true;
        }
        if (id >= entries.size()) return false;
        var entry = entries.get(id);
        if (memory) {
            if (!ResearchProgress.completed(player, entry.id()) || entry.value().production().isEmpty()) return false;
            ItemStack notes = ResearchNotesItem.create(entry.id(), player.getUUID());
            if (!player.getInventory().add(notes)) player.drop(notes, false);
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.lyycore.research_copied"), true);
            server.closeContainer();
            return true;
        }
        boolean completed = ResearchProgress.complete(server, entry.id(), entry.value());
        closePreview();
        statuses[id] = initialStatus(id);
        broadcastChanges();
        if (completed) access.execute((level, pos) -> openTable(server, pos));
        return completed;
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && (memory || stillValid(access, player, LyyBlocks.IMAGINARY_RESEARCH_TABLE.get())); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
