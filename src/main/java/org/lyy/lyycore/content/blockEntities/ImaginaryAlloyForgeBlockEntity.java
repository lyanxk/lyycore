package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.energy.ImaginaryEnergyFeAdapter;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.Config;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.content.menu.IAFMenu;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;
import org.lyy.lyycore.content.blocks.ImaginaryAlloyForge;
import net.minecraft.world.level.block.Block;


public class ImaginaryAlloyForgeBlockEntity extends BlockEntity implements MenuProvider {
    private int progress = 0;
    private int maxProgress = 0;
    private int totalEnergyCost = 0;
    private int energySpent = 0;
    private boolean hasActiveRecipe = false;
    private boolean recipeDirty = true;
    @Nullable private RecipeHolder<ImaginaryAlloyingRecipe> cachedRecipe;
    @Nullable private RecipeManager cachedRecipeManager;
    @Nullable private ResourceLocation activeRecipeId;

    private final ImaginaryEnergyStorage energyStorage = new ImaginaryEnergyStorage(1_000_000, Integer.MAX_VALUE, Integer.MAX_VALUE, this::setChanged);
    private final ImaginaryEnergyFeAdapter feAdapter = new ImaginaryEnergyFeAdapter(energyStorage, this::setChanged);

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            inventoryChanged();
        }
    };

    private final RecipeInput recipeInput = new RecipeInput() {
        @Override public ItemStack getItem(int index) { return items.getStackInSlot(index); }
        @Override public int size() { return items.getSlots(); }
    };

    private final IItemHandler automationHandler = new FilteredItemHandler(new int[]{0, 1, 2, 3});
    private final IItemHandler inputHandler = new FilteredItemHandler(new int[]{0, 1});
    private final IItemHandler catalystHandler = new FilteredItemHandler(new int[]{2, 3});
    private final IItemHandler outputHandler = new FilteredItemHandler(new int[]{3});

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int value = switch (i / 2) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getImaginaryEnergyStored();
                case 3 -> energyStorage.getMaxImaginaryEnergyStored();
                default -> 0;
            };
            return i % 2 == 0 ? value & 0xFFFF : value >>> 16 & 0xFFFF;
        }
        @Override public void set(int i, int v) { }
        @Override public int getCount() { return 8; }
    };

    public ImaginaryAlloyForgeBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IAF.get(), pos, state);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.imaginary_alloy_forge"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new IAFMenu(id, inv, this, this.data);
    }

    public ContainerData getData() { return data; }
    public IEnergyStorage getEnergyStorage() { return feAdapter; }
    public ImaginaryEnergyStorage getImaginaryEnergyStorage() { return energyStorage; }
    public ItemStackHandler getItemHandler() { return items; }
    public void inventoryChanged() {
        recipeDirty = true;
        setChanged();
    }
    public IItemHandler getAutomationItemHandler(@Nullable Direction side) {
        if (side == null) return automationHandler;
        if (side == Direction.UP) return inputHandler;
        if (side == Direction.DOWN) return outputHandler;
        return catalystHandler;
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IE", energyStorage.getImaginaryEnergyStored());
        tag.putByte("FERemainder", feAdapter.getRemainder());
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("TotalEnergyCost", totalEnergyCost);
        tag.putInt("EnergySpent", energySpent);
        tag.putBoolean("HasActiveRecipe", hasActiveRecipe);
        if (activeRecipeId != null) tag.putString("ActiveRecipe", activeRecipeId.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // The old natural-generation store and recipe costs were labelled FE;
        // keep their numerical values when correcting the internal unit to IE.
        energyStorage.setImaginaryEnergy(tag.getInt(tag.contains("IE") ? "IE" : "Energy"));
        feAdapter.setRemainder(tag.getInt("FERemainder"));
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        totalEnergyCost = tag.getInt("TotalEnergyCost");
        energySpent = tag.getInt("EnergySpent");
        hasActiveRecipe = tag.getBoolean("HasActiveRecipe");
        activeRecipeId = tag.contains("ActiveRecipe") ? ResourceLocation.tryParse(tag.getString("ActiveRecipe")) : null;
        // Saves made before process_time became seconds stored an incompatible
        // progress scale. Restarting that recipe preserves its inputs and avoids
        // finishing it twenty times too early after an upgrade.
        if (hasActiveRecipe && !tag.contains("TotalEnergyCost")) {
            progress = 0;
            maxProgress = 0;
            totalEnergyCost = 0;
            energySpent = 0;
            hasActiveRecipe = false;
            activeRecipeId = null;
        }
        recipeDirty = true;
        cachedRecipe = null;
    }

    // --- Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryAlloyForgeBlockEntity be) {
        if (level.isClientSide) return;

        int passiveInterval = Config.FORGE_PASSIVE_INTERVAL.get();
        if (level.getGameTime() % passiveInterval == 0) {
            be.energyStorage.receiveImaginaryEnergy(Config.FORGE_PASSIVE_IE.get(), false);
        }

        var manager = level.getRecipeManager();
        // A resource reload replaces the manager, including cached misses. Also
        // reject an active holder replaced in-place by another recipe provider.
        if (be.cachedRecipeManager != manager || be.cachedRecipe != null
                && manager.byKey(be.cachedRecipe.id()).orElse(null) != be.cachedRecipe) {
            be.recipeDirty = true;
            be.cachedRecipeManager = manager;
        }
        if (be.recipeDirty) be.refreshRecipe();
        boolean burning = be.hasActiveRecipe && be.cachedRecipe != null
                && be.tickProcessing(be.cachedRecipe.value());
        // Sync only on ignition/extinction, so animation does not rebuild the chunk every tick.
        if (state.getValue(ImaginaryAlloyForge.LIT) != burning) {
            level.setBlock(pos, state.setValue(ImaginaryAlloyForge.LIT, burning), Block.UPDATE_CLIENTS);
        }
    }

    private void start(RecipeHolder<ImaginaryAlloyingRecipe> holder) {
        ImaginaryAlloyingRecipe rec = holder.value();
        // Recipe process_time is authored in seconds; the machine advances once per tick.
        int seconds = Math.max(1, Math.min(rec.getProcessTime(), Integer.MAX_VALUE / 20));
        this.maxProgress = seconds * 20;
        this.totalEnergyCost = Math.max(0, rec.getEnergyCost());
        this.energySpent = 0;
        this.progress = 0;
        this.hasActiveRecipe = true;
        this.activeRecipeId = holder.id();
        setChanged();
    }

    private void refreshRecipe() {
        recipeDirty = false;
        var previous = cachedRecipe;
        cachedRecipe = level.getRecipeManager()
                .getRecipeFor(LyyRecipes.IMAGINARY_ALLOYING.get(), recipeInput, level)
                .orElse(null);

        if (cachedRecipe == null || !canOutput(cachedRecipe.value())) {
            if (hasActiveRecipe) resetProcessing();
            return;
        }

        if (!hasActiveRecipe) {
            start(cachedRecipe);
        } else if (activeRecipeId != null && !activeRecipeId.equals(cachedRecipe.id())
                || previous != null && !previous.value().sameDefinitionAs(cachedRecipe.value())) {
            var next = cachedRecipe;
            resetProcessing();
            cachedRecipe = next;
            start(next);
        } else if (maxProgress <= 0) {
            start(cachedRecipe);
        } else {
            if (activeRecipeId == null) activeRecipeId = cachedRecipe.id();
            totalEnergyCost = Math.max(0, cachedRecipe.value().getEnergyCost());
            energySpent = Math.max(0, Math.min(energySpent, totalEnergyCost));
            progress = Math.max(0, Math.min(progress, maxProgress));
        }
    }

    private boolean tickProcessing(ImaginaryAlloyingRecipe rec) {
        int nextProgress = Math.min(progress + 1, maxProgress);
        int targetSpent = calculateEnergySpentAtProgress(nextProgress, maxProgress, totalEnergyCost);
        int due = Math.max(0, targetSpent - energySpent);
        if (due > 0 && energyStorage.extractImaginaryEnergy(due, true) < due) return false;
        if (due > 0) energyStorage.extractImaginaryEnergy(due, false);

        energySpent = targetSpent;
        progress++;
        if (progress >= maxProgress) {
            finish(rec);
            resetProcessing();
        } else {
            setChanged();
        }
        return hasActiveRecipe;
    }

    public static int calculateEnergySpentAtProgress(int progress, int maxProgress, int totalEnergyCost) {
        if (maxProgress <= 0 || totalEnergyCost <= 0 || progress <= 0) return 0;
        int clampedProgress = Math.min(progress, maxProgress);
        return (int) ((long) clampedProgress * totalEnergyCost / maxProgress);
    }

    private void resetProcessing() {
        progress = 0;
        maxProgress = 0;
        totalEnergyCost = 0;
        energySpent = 0;
        hasActiveRecipe = false;
        activeRecipeId = null;
        cachedRecipe = null;
        setChanged();
    }

    private boolean canOutput(ImaginaryAlloyingRecipe rec) {
        ItemStack outSlot = items.getStackInSlot(3);
        ItemStack result = rec.getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;
        if (outSlot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(outSlot, result)) return false;
        return outSlot.getCount() + result.getCount() <= outSlot.getMaxStackSize();
    }

    private void finish(ImaginaryAlloyingRecipe rec) {
        int[] matchedSlots = rec.matchedInputSlots(recipeInput);
        if (matchedSlots == null || !canOutput(rec)) {
            recipeDirty = true;
            return;
        }
        consumeInputsFor(rec, matchedSlots);
        ItemStack result = rec.getResultItem(level.registryAccess());
        ItemStack out = items.getStackInSlot(3);
        if (out.isEmpty()) {
            items.setStackInSlot(3, result.copy());
        } else {
            out.grow(result.getCount());
        }
        setChanged();
    }

    private void consumeInputsFor(ImaginaryAlloyingRecipe rec, int[] matchedSlots) {
        for (int slot : matchedSlots) items.extractItem(slot, 1, false);

        if (rec.getCatalyst() != null && !rec.getCatalyst().isEmpty() && rec.isCatalystConsumed()) {
            items.extractItem(2, 1, false);
        }
    }

    private final class FilteredItemHandler implements IItemHandler {
        private final int[] slots;

        private FilteredItemHandler(int[] slots) {
            this.slots = slots;
        }

        private int actualSlot(int slot) {
            if (slot < 0 || slot >= slots.length) throw new RuntimeException("Slot " + slot + " not in valid range");
            return slots[slot];
        }

        @Override public int getSlots() { return slots.length; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(actualSlot(slot)); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int actual = actualSlot(slot);
            return actual == 3 ? stack : items.insertItem(actual, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(actualSlot(slot), amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(actualSlot(slot)); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int actual = actualSlot(slot);
            return actual != 3 && items.isItemValid(actual, stack);
        }
    }
}
