package org.lyy.lyycore.content.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.content.recipes.ResearchRecipe;
import org.lyy.lyycore.registry.*;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import org.lyy.lyycore.content.item.ResearchNotesItem;

public final class ResearchMenu extends AbstractContainerMenu {
    public static final int READY = 0, COMPLETED = 1, MISSING_COST = 2, UNCHECKED = 3;
    public static final int CLOSE_PREVIEW = -1;
    private final Player owner;
    private final ContainerLevelAccess access;
    private final List<RecipeHolder<ResearchRecipe>> entries;
    private final int[] statuses;
    private final boolean memory;
    private int preview = -1, previewExperience;
    private float previewExperienceProgress;
    private List<ItemStack> previewInventory;

    public ResearchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), readEntries(inventory.player, buffer), false);
    }

    private ResearchMenu(int id, Inventory inventory, BlockPos pos, List<RecipeHolder<ResearchRecipe>> entries, boolean memory) {
        super(memory ? LyyMenus.MEMORY.get() : LyyMenus.RESEARCH.get(), id);
        this.owner = inventory.player;
        this.memory = memory;
        this.access = memory ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(inventory.player.level(), pos);
        this.entries = List.copyOf(entries);
        statuses = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            statuses[i] = memory || ResearchProgress.completed(owner, entries.get(i).id()) ? COMPLETED : UNCHECKED;
            addDataSlot(DataSlot.shared(statuses, i));
        }
    }
    private static List<RecipeHolder<ResearchRecipe>> available(Player player, boolean completedOnly) {
        return player.level().getRecipeManager().getAllRecipesFor(LyyRecipes.RESEARCH.get()).stream()
                .filter(entry -> ResearchProgress.completed(player, entry.id()) == completedOnly)
                .sorted(Comparator.comparing(entry -> entry.id().toString())).toList();
    }
    private static List<RecipeHolder<ResearchRecipe>> readEntries(Player player, RegistryFriendlyByteBuf buffer) {
        // Resolve only the server-selected IDs, preserving their order for button/status indices.
        return buffer.readList(buf -> {
            var id = buf.readResourceLocation();
            var entry = player.level().getRecipeManager().byKey(id).orElseThrow();
            if (!(entry.value() instanceof ResearchRecipe recipe))
                throw new IllegalStateException("Not a research recipe: " + id);
            return new RecipeHolder<>(id, recipe);
        });
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
    public List<RecipeHolder<ResearchRecipe>> entries() { return entries; }
    public int status(int index) { return statuses[index]; }

    /** Nonnegative buttons submit research; negative buttons only manage the cost preview. */
    public static int previewButton(int index) { return -index - 2; }

    private void closePreview() {
        if (preview >= 0 && statuses[preview] != COMPLETED) statuses[preview] = UNCHECKED;
        preview = -1;
        previewInventory = null;
    }

    private boolean previewInputsChanged() {
        if (previewInventory == null || previewExperience != owner.experienceLevel
                || previewExperienceProgress != owner.experienceProgress) return true;
        var items = owner.getInventory().items;
        // Inventory.setChanged() is not called by every in-place stack mutation. Compare a
        // single 36-slot snapshot only while confirming, so commands and pickups stay correct.
        for (int i = 0; i < items.size(); i++) {
            if (!ItemStack.matches(items.get(i), previewInventory.get(i))) return true;
        }
        return false;
    }

    private void refreshPreview() {
        if (preview < 0 || owner.level().isClientSide) return;
        var entry = entries.get(preview);
        if (ResearchProgress.completed(owner, entry.id())) {
            statuses[preview] = COMPLETED;
        } else if (previewInputsChanged()) {
            previewExperience = owner.experienceLevel;
            previewExperienceProgress = owner.experienceProgress;
            previewInventory = owner.getInventory().items.stream().map(ItemStack::copy).toList();
            statuses[preview] = ResearchProgress.canAfford(owner, entry.value()) ? READY : MISSING_COST;
        }
    }

    @Override public void broadcastChanges() {
        refreshPreview();
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || player != owner || !stillValid(player)) return false;
        if (id < 0) {
            if (memory) return false;
            if (id == CLOSE_PREVIEW) {
                closePreview();
            } else {
                int index = -id - 2;
                if (index < 0 || index >= entries.size()) return false;
                closePreview();
                preview = index;
            }
            broadcastChanges();
            return true;
        }
        if (id >= entries.size()) return false;
        var entry = entries.get(id);
        // Reject stale menus after /reload rather than charging for an obsolete definition.
        if (player.level().getRecipeManager().byKey(entry.id()).orElse(null) != entry) return false;
        if (memory) {
            if (!ResearchProgress.completed(player, entry.id()) || entry.value().production().isEmpty()) return false;
            ItemStack notes = ResearchNotesItem.create(entry.id(), player.getUUID());
            if (!player.getInventory().add(notes)) player.drop(notes, false);
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.lyycore.research_copied"), true);
            return true;
        }
        boolean completed = ResearchProgress.complete(server, entry.id(), entry.value());
        closePreview();
        statuses[id] = ResearchProgress.completed(player, entry.id()) ? COMPLETED : UNCHECKED;
        broadcastChanges();
        return completed;
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && (memory || stillValid(access, player, LyyBlocks.IMAGINARY_RESEARCH_TABLE.get())); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
