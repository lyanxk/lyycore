package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import org.lyy.lyycore.content.menu.ExperimentTableMenu;
import org.lyy.lyycore.content.research.ExperimentDefinition.Element;
import org.lyy.lyycore.registry.*;

public final class ExperimentTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 100;
    public record Fuel(Element element, int amount) { }
    public static Fuel fuel(ItemStack stack) {
        if (stack.is(Items.ICE)) return new Fuel(Element.ICE, 1);
        if (stack.is(Items.PACKED_ICE)) return new Fuel(Element.ICE, 9);
        if (stack.is(Items.BLUE_ICE)) return new Fuel(Element.ICE, 81);
        if (stack.is(Items.MAGMA_BLOCK)) return new Fuel(Element.FIRE, 1);
        if (stack.is(LyyItems.LIGHTNING_BOTTLE.get())) return new Fuel(Element.LIGHTNING, 4);
        return null;
    }
    private final int[] reserves = new int[Element.values().length];
    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? fuel(stack) != null : stack.is(LyyItems.RESEARCH_NOTES.get());
        }
        @Override public int getSlotLimit(int slot) { return slot == 1 ? 1 : 64; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return reserves[index]; }
        @Override public void set(int index, int value) { reserves[index] = Math.clamp(value, 0, CAPACITY); }
        @Override public int getCount() { return reserves.length; }
    };
    public ExperimentTableBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.EXPERIMENT_TABLE.get(), pos, state); }
    public ItemStackHandler items() { return items; }
    public ContainerData data() { return data; }
    public static void serverTick(Level level, BlockPos pos, BlockState state, ExperimentTableBlockEntity table) {
        var fuel = fuel(table.items.getStackInSlot(0));
        if (fuel == null || table.reserves[fuel.element.ordinal()] >= CAPACITY) return;
        table.items.extractItem(0, 1, false);
        table.reserves[fuel.element.ordinal()] = Math.min(CAPACITY, table.reserves[fuel.element.ordinal()] + fuel.amount);
        table.setChanged();
    }
    /** All button actions are revalidated against the server's current report and reserves. */
    public boolean act(int action) {
        if (level == null || level.isClientSide) return false;
        ItemStack notes = items.getStackInSlot(1);
        var experiment = ResearchNotesItem.experiment(notes, level);
        if (experiment == null || action != -1 && ResearchNotesItem.experimentComplete(notes, experiment)) return false;
        int[] positions = ResearchNotesItem.experimentPositions(notes, experiment);
        if (action == -1) positions = experiment.initial();
        else {
            if (action < 0 || action >= experiment.nodes().size() * 25) return false;
            int node = action / 25, destination = action % 25, origin = positions[node];
            int element = experiment.nodes().get(node).element().ordinal();
            if (origin == destination || reserves[element] == 0) return false;
            for (int other = 0; other < positions.length; other++) if (positions[other] == destination) {
                positions[other] = origin; break;
            }
            positions[node] = destination;
            reserves[element]--;
        }
        var updated = notes.copy();
        ResearchNotesItem.setExperimentPositions(updated, experiment, positions);
        items.setStackInSlot(1, updated);
        setChanged();
        return true;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.experiment_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ExperimentTableMenu(id, inventory, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries)); tag.putIntArray("Reserves", reserves);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        int[] saved = tag.getIntArray("Reserves");
        for (int i = 0; i < reserves.length; i++) reserves[i] = i < saved.length ? Math.clamp(saved[i], 0, CAPACITY) : 0;
    }
}
