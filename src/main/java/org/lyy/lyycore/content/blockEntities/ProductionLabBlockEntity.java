package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.ProductionOwnership;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import org.lyy.lyycore.content.menu.ProductionLabMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyItems;

/** One slot changes from the research record into its result, just as the design describes. */
public final class ProductionLabBlockEntity extends BlockEntity implements MenuProvider {
    private int progress, duration;
    private boolean output;
    private long lastUpdate;
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return stack.is(LyyItems.RESEARCH_NOTES.get()); }
        @Override protected void onContentsChanged(int slot) { progress = duration = 0; output = false; changed(); }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return index == 0 ? duration == 0 ? 0 : progress * 1000 / duration
                    : output ? 2 : duration > 0 ? 1 : items.getStackInSlot(0).isEmpty() ? 0 : 3;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 2; }
    };
    public ProductionLabBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.PRODUCTION_LAB.get(), pos, state); }
    public ItemStackHandler items() { return items; }
    public ContainerData data() { return data; }
    public boolean working() { return !output && duration > 0; }
    public float animationProgress(float partial) {
        if (!working() || level == null) return 0;
        return Math.min(1, (progress + level.getGameTime() - lastUpdate + partial) / duration);
    }
    private void changed() {
        setChanged();
        if (level instanceof ServerLevel server) {
            // Only the inventory and animation change; keep the static chunk mesh intact.
            var packet = getUpdatePacket();
            for (var player : server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false))
                player.connection.send(packet);
        }
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, ProductionLabBlockEntity lab) {
        if (lab.output) return;
        var notes = lab.items.getStackInSlot(0);
        var owner = ResearchNotesItem.owner(notes);
        var research = ResearchNotesItem.research(notes, level);
        var production = research == null ? null : research.production().orElse(null);
        if (production == null || owner == null) {
            if (lab.progress != 0 || lab.duration != 0) { lab.progress = lab.duration = 0; lab.changed(); }
            return;
        }
        lab.duration = production.duration();
        lab.progress++;
        if (lab.progress >= lab.duration) {
            var result = production.result();
            ProductionOwnership.bind(result, owner);
            lab.items.setStackInSlot(0, result);
            lab.output = true;
            lab.changed();
        } else if (lab.progress == 1 || lab.progress % 10 == 0) lab.changed();
        else lab.setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.production_lab"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ProductionLabMenu(id, inventory, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress); tag.putInt("Duration", duration); tag.putBoolean("Output", output);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        duration = Math.clamp(tag.getInt("Duration"), 0, 72000);
        progress = Math.clamp(tag.getInt("Progress"), 0, duration);
        output = tag.getBoolean("Output");
        lastUpdate = level == null ? 0 : level.getGameTime();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
