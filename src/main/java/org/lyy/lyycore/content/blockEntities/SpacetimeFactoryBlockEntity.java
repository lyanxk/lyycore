package org.lyy.lyycore.content.blockEntities;

import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.content.equipment.AccessoryData;
import org.lyy.lyycore.content.menu.SpacetimeFactoryMenu;
import org.lyy.lyycore.content.recipes.*;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.*;

/** Four independent jobs share energy, allocated from the leftmost page each tick. */
public final class SpacetimeFactoryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 2_000_000_000, OUTPUT_LIMIT = 256;
    public enum Page {
        SPACETIME(16), CONDENSING(9), CRAFTING(9), ALLOYING(3);
        public final int inputCount;
        Page(int inputCount) { this.inputCount = inputCount; }
    }
    public static final int TOTAL_SLOTS = java.util.Arrays.stream(Page.values()).mapToInt(page -> page.inputCount + 1).sum();
    private final PageState[] pages = new PageState[Page.values().length];
    private RecipeManager recipeManager;
    public final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(CAPACITY, Integer.MAX_VALUE, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);

    /** Every value is split into two unsigned shorts for the vanilla menu data protocol. */
    public static final int DATA_VALUES = 2 + Page.values().length * 6, DATA_COUNT = DATA_VALUES * 2;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            int valueIndex = index / 2;
            int value;
            if (valueIndex == 0) value = energy.getImaginaryEnergyStored();
            else if (valueIndex == 1) value = CAPACITY;
            else {
                var page = pages[(valueIndex - 2) / 6];
                value = switch ((valueIndex - 2) % 6) {
                    case 0 -> page.progress; case 1 -> page.duration; case 2 -> page.cost;
                    case 3 -> page.outputCount; case 4 -> page.energyPerTick ? 1 : 0;
                    default -> page.running ? 1 : 0;
                };
            }
            return index % 2 == 0 ? value & 65535 : value >>> 16;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public SpacetimeFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.SPACETIME_FACTORY.get(), pos, state);
        for (var page : Page.values()) pages[page.ordinal()] = new PageState(page);
    }
    public ItemStackHandler inputs(Page page) { return pages[page.ordinal()].inputs; }
    public void inventoryChanged(Page page) { pages[page.ordinal()].dirty = true; setChanged(); }
    public IItemHandlerModifiable outputHandler(Page page) { return pages[page.ordinal()].outputHandler; }
    public int outputCount(Page page) { return pages[page.ordinal()].outputCount; }
    public ContainerData data() { return data; }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.spacetime_factory"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new SpacetimeFactoryMenu(id, inv, this, data); }
    @Override public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) { buffer.writeBlockPos(worldPosition); }

    private final class PageState {
        final Page page;
        final ItemStackHandler inputs;
        final RecipeInput input;
        ItemStack output = ItemStack.EMPTY;
        int outputCount, progress, duration = 1, cost;
        boolean dirty = true, energyPerTick, running;
        ResourceLocation activeRecipe, completedRecipe;
        String definition = "";
        RecipeHolder<? extends Recipe<RecipeInput>> recipe;
        final IItemHandlerModifiable outputHandler = new IItemHandlerModifiable() {
            @Override public int getSlots() { return 1; }
            @Override public ItemStack getStackInSlot(int slot) { return output.copyWithCount(Math.min(outputCount, output.getMaxStackSize())); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                int taken = Math.min(Math.min(Math.max(0, amount), outputCount), output.getMaxStackSize());
                var stack = output.copyWithCount(taken);
                if (!simulate && taken > 0) { outputCount -= taken; if (outputCount == 0) output = ItemStack.EMPTY; setChanged(); }
                return stack;
            }
            @Override public void setStackInSlot(int slot, ItemStack stack) {
                // Menus synchronize ordinary stacks. The full buffer count travels separately as data.
                if (level != null && level.isClientSide) {
                    output = stack.copyWithCount(1); outputCount = stack.getCount(); return;
                }
                int visible = Math.min(outputCount, output.getMaxStackSize());
                if (stack.isEmpty() || ItemStack.isSameItemSameComponents(output, stack)) {
                    outputCount -= Math.max(0, visible - stack.getCount());
                    if (outputCount == 0) output = ItemStack.EMPTY;
                    setChanged();
                }
            }
            @Override public int getSlotLimit(int slot) { return OUTPUT_LIMIT; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        };
        PageState(Page page) {
            this.page = page;
            inputs = new ItemStackHandler(page.inputCount) {
                @Override protected void onContentsChanged(int slot) { dirty = true; setChanged(); }
            };
            input = new RecipeInput() {
                @Override public ItemStack getItem(int slot) { return inputs.getStackInSlot(slot); }
                @Override public int size() { return inputs.getSlots(); }
            };
        }
        void reset() { progress = 0; duration = 1; cost = 0; activeRecipe = null; definition = ""; }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpacetimeFactoryBlockEntity factory) {
        var manager = level.getRecipeManager();
        boolean reload = factory.recipeManager != manager;
        factory.recipeManager = manager;
        for (var page : factory.pages) {
            page.running = false;
            if (reload || page.recipe != null && manager.byKey(page.recipe.id()).orElse(null) != page.recipe) page.dirty = true;
            if (page.dirty) factory.refresh(page);
            factory.process(page);
        }
        if (level.getGameTime() % 8 == 0) factory.pushOutputs();
    }
    private void refresh(PageState state) {
        state.dirty = false;
        state.recipe = switch (state.page) {
            case SPACETIME -> recipeManager.getRecipeFor(LyyRecipes.SPACETIME.get(), state.input, level).orElse(null);
            case CONDENSING -> recipeManager.getRecipeFor(LyyRecipes.IMAGINARY_CONDENSING.get(), state.input, level).orElse(null);
            case CRAFTING -> recipeManager.getRecipeFor(LyyRecipes.IMAGINARY_CRAFTING.get(), state.input, level).orElse(null);
            case ALLOYING -> recipeManager.getRecipeFor(LyyRecipes.IMAGINARY_ALLOYING.get(), state.input, level).orElse(null);
        };
        if (state.recipe == null) { if (state.activeRecipe != null) { state.reset(); setChanged(); } return; }
        var recipe = state.recipe.value();
        String definition = definition(recipe);
        if (!state.recipe.id().equals(state.activeRecipe) || !state.definition.isEmpty() && !state.definition.equals(definition)) {
            state.progress = 0; setChanged();
        }
        state.activeRecipe = state.recipe.id(); state.definition = definition;
        if (recipe instanceof SpacetimeRecipe r) { state.duration = r.duration(); state.cost = r.energy(); state.energyPerTick = false; }
        else if (recipe instanceof OctagonalRecipe r) { state.duration = r.duration(); state.cost = r.energy(); state.energyPerTick = r.energyPerTick(); }
        else if (recipe instanceof ImaginaryAlloyingRecipe r) { state.duration = r.processTime() * 20; state.cost = r.energyCost(); state.energyPerTick = false; }
        if (state.progress >= state.duration) state.progress = 0;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String definition(Recipe<RecipeInput> recipe) {
        // Compare definitions only when inventory changes or recipes reload, including same-id replacements.
        var codec = ((com.mojang.serialization.MapCodec) recipe.getSerializer().codec()).codec();
        return codec.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), recipe).getOrThrow().toString();
    }
    private ItemStack result(PageState state) {
        var recipe = state.recipe.value();
        if (recipe instanceof SpacetimeRecipe r) return r.result().copyWithCount(r.resultCount());
        return recipe.assemble(state.input, level.registryAccess());
    }
    private void process(PageState state) {
        if (state.recipe == null) return;
        var result = result(state);
        if (state.outputCount + result.getCount() > OUTPUT_LIMIT || state.outputCount > 0
                && !ItemStack.isSameItemSameComponents(state.output, result)) return;
        int due;
        if (state.recipe.value() instanceof OctagonalRecipe) {
            // Existing tables require the full fee available while progressing and pay at completion.
            if (energy.getImaginaryEnergyStored() < state.cost) return;
            due = state.energyPerTick || state.progress + 1 >= state.duration ? state.cost : 0;
        } else {
            // Exact total payment, entirely in int arithmetic, with the remainder paid over early ticks.
            due = state.cost / state.duration + (state.progress < state.cost % state.duration ? 1 : 0);
        }
        if (energy.getImaginaryEnergyStored() < due) return;
        energy.setImaginaryEnergy(energy.getImaginaryEnergyStored() - due);
        state.running = true;
        state.progress++;
        if (state.progress >= state.duration) finish(state, result);
        setChanged();
    }
    private void finish(PageState state, ItemStack result) {
        var recipe = state.recipe.value();
        if (recipe instanceof SpacetimeRecipe r) {
            for (int slot = 0; slot < 16; slot++) if (!r.inputs().get(slot).ingredient().isEmpty()) state.inputs.extractItem(slot, r.inputs().get(slot).count(), false);
        } else if (recipe instanceof ImaginaryAlloyingRecipe r) {
            state.inputs.extractItem(0, 1, false); state.inputs.extractItem(1, 1, false);
            if (!r.catalyst().isEmpty() && r.catalystConsumed()) state.inputs.extractItem(2, 1, false);
        } else {
            AccessoryData.initialize(result, level.random);
            for (int slot = 0; slot < state.inputs.getSlots(); slot++) state.inputs.extractItem(slot, 1, false);
        }
        state.output = result.copyWithCount(1); state.outputCount += result.getCount();
        state.completedRecipe = state.recipe.id(); state.progress = 0; state.dirty = true;
    }
    public void awardCraftedResult(Page page, Player player, ItemStack taken) {
        var state = pages[page.ordinal()];
        if (player instanceof ServerPlayer server && state.completedRecipe != null && !taken.isEmpty()) {
            CriteriaTriggers.RECIPE_CRAFTED.trigger(server, state.completedRecipe, java.util.List.of());
            state.completedRecipe = null; setChanged();
        }
    }

    /** Top and horizontal faces accept inputs; the bottom exposes the four outputs. */
    public IItemHandler getAutomationItemHandler(@Nullable Direction side) {
        return new IItemHandler() {
            final boolean onlyOutput = side == Direction.DOWN;
            @Override public int getSlots() { return onlyOutput ? pages.length : TOTAL_SLOTS; }
            private PageState state(int slot) {
                if (onlyOutput) return pages[slot];
                for (var page : pages) { if (slot <= page.page.inputCount) return page; slot -= page.page.inputCount + 1; }
                throw new IndexOutOfBoundsException(slot);
            }
            private int local(int slot) {
                if (onlyOutput) return state(slot).page.inputCount;
                for (var page : pages) { if (slot <= page.page.inputCount) return slot; slot -= page.page.inputCount + 1; }
                throw new IndexOutOfBoundsException(slot);
            }
            @Override public ItemStack getStackInSlot(int slot) { var p = state(slot); int i = local(slot); return i == p.page.inputCount ? p.outputHandler.getStackInSlot(0) : p.inputs.getStackInSlot(i); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { var p = state(slot); int i = local(slot); return i == p.page.inputCount ? stack : p.inputs.insertItem(i, stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { var p = state(slot); return local(slot) == p.page.inputCount ? p.outputHandler.extractItem(0, amount, simulate) : ItemStack.EMPTY; }
            @Override public int getSlotLimit(int slot) { return local(slot) == state(slot).page.inputCount ? OUTPUT_LIMIT : 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return local(slot) < state(slot).page.inputCount; }
        };
    }
    private void pushOutputs() {
        for (var direction : new Direction[]{Direction.UP, Direction.DOWN}) {
            var target = worldPosition.relative(direction);
            if (!level.hasChunkAt(target)) continue;
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, direction.getOpposite());
            if (handler == null) continue;
            for (var page : pages) while (page.outputCount > 0) {
                var offered = page.outputHandler.getStackInSlot(0);
                var remainder = ItemHandlerHelper.insertItemStacked(handler, offered, false);
                int moved = offered.getCount() - remainder.getCount();
                if (moved == 0) break;
                page.outputHandler.extractItem(0, moved, false);
            }
        }
    }
    public void dropContents() {
        for (var page : pages) {
            for (int slot = 0; slot < page.inputs.getSlots(); slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), page.inputs.getStackInSlot(slot));
            while (page.outputCount > 0) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), page.outputHandler.extractItem(0, 64, false));
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IE", energy.getImaginaryEnergyStored()); tag.putByte("FE", fe.getRemainder());
        for (var page : pages) {
            var saved = new CompoundTag(); saved.put("Inputs", page.inputs.serializeNBT(registries));
            if (!page.output.isEmpty()) saved.put("Output", page.output.save(registries));
            saved.putInt("OutputCount", page.outputCount); saved.putInt("Progress", page.progress);
            if (page.activeRecipe != null) saved.putString("Recipe", page.activeRecipe.toString());
            if (page.completedRecipe != null) saved.putString("CompletedRecipe", page.completedRecipe.toString());
            saved.putString("Definition", page.definition); tag.put(page.page.name(), saved);
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setImaginaryEnergy(tag.getInt("IE")); fe.setRemainder(tag.getByte("FE"));
        for (var page : pages) {
            var saved = tag.getCompound(page.page.name()); page.inputs.deserializeNBT(registries, saved.getCompound("Inputs"));
            page.output = ItemStack.parseOptional(registries, saved.getCompound("Output"));
            page.outputCount = page.output.isEmpty() ? 0 : Math.clamp(saved.getInt("OutputCount"), 0, OUTPUT_LIMIT);
            page.progress = Math.max(0, saved.getInt("Progress")); page.activeRecipe = ResourceLocation.tryParse(saved.getString("Recipe"));
            page.completedRecipe = ResourceLocation.tryParse(saved.getString("CompletedRecipe")); page.definition = saved.getString("Definition");
            page.recipe = null; page.dirty = true;
        }
        recipeManager = null;
    }
}
