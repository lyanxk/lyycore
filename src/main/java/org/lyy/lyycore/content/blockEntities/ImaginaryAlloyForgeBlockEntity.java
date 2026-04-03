package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.content.menu.IAFMenu;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImaginaryAlloyForgeBlockEntity extends BlockEntity implements MenuProvider {
    private int progress = 0;
    private int maxProgress = 0;
    private int energyPerTick = 0;
    private boolean hasActiveRecipe = false;

    private final EnergyStorage energyStorage = new EnergyStorage(1_000_000, 1_000_000, 1_000_000);

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> maxProgress = v;
            }
        }
        @Override public int getCount() { return 4; }
    };

    public ImaginaryAlloyForgeBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IAF.get(), pos, state);
    }

    @Override public Component getDisplayName() { return Component.empty(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new IAFMenu(id, inv, this, this.data);
    }

    public ContainerData getData() { return data; }
    public EnergyStorage getEnergyStorage() { return energyStorage; }
    public ItemStackHandler getItemHandler() { return items; }

    // --- RecipeInput adapter ---
    private RecipeInput asRecipeInput() {
        return new RecipeInput() {
            @Override public ItemStack getItem(int index) { return items.getStackInSlot(index); }
            @Override public int size() { return items.getSlots(); }
        };
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("EnergyPerTick", energyPerTick);
        tag.putBoolean("HasActiveRecipe", hasActiveRecipe);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        energyPerTick = tag.getInt("EnergyPerTick");
        hasActiveRecipe = tag.getBoolean("HasActiveRecipe");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("Items", items.serializeNBT(registries));
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryAlloyForgeBlockEntity be) {
        if (level.isClientSide) return;

        // Passive energy gain
        if (level.getGameTime() % 20 == 0)
            be.energyStorage.receiveEnergy(1, false);

        if (!be.hasActiveRecipe) {
            be.tryStartRecipe();
        } else {
            be.tickProcessing();
        }
        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private void tryStartRecipe() {
        RecipeInput input = asRecipeInput();
        Optional<RecipeHolder<ImaginaryAlloyingRecipe>> match =
                level.getRecipeManager().getRecipeFor(LyyRecipes.IMAGINARY_ALLOYING.get(), input, level);
        match.ifPresent(holder -> {
            ImaginaryAlloyingRecipe rec = holder.value();
            if (canOutput(rec)) start(rec);
        });
    }

    private void start(ImaginaryAlloyingRecipe rec) {
        this.maxProgress = Math.max(1, rec.getProcessTime());
        this.energyPerTick = (rec.getEnergyCost() + this.maxProgress - 1) / this.maxProgress;
        this.progress = 0;
        this.hasActiveRecipe = true;
        setChanged();
    }

    private void tickProcessing() {
        RecipeInput input = asRecipeInput();
        Optional<RecipeHolder<ImaginaryAlloyingRecipe>> match =
                level.getRecipeManager().getRecipeFor(LyyRecipes.IMAGINARY_ALLOYING.get(), input, level);

        if (match.isEmpty() || !canOutput(match.get().value())) {
            resetProcessing();
            return;
        }
        ImaginaryAlloyingRecipe rec = match.get().value();

        // Energy check
        if (energyPerTick > 0) {
            if (energyStorage.extractEnergy(energyPerTick, true) < energyPerTick) return;
            energyStorage.extractEnergy(energyPerTick, false);
        }

        progress++;
        if (progress >= maxProgress) {
            finish(rec);
            resetProcessing();
        }
        setChanged();
    }

    private void resetProcessing() {
        progress = 0;
        maxProgress = 0;
        energyPerTick = 0;
        hasActiveRecipe = false;
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
        consumeInputsFor(rec);
        ItemStack result = rec.getResultItem(level.registryAccess());
        ItemStack out = items.getStackInSlot(3);
        if (out.isEmpty()) {
            items.setStackInSlot(3, result.copy());
        } else {
            out.grow(result.getCount());
        }
        setChanged();
    }

    private void consumeInputsFor(ImaginaryAlloyingRecipe rec) {
        List<ItemStack> offered = new ArrayList<>();
        offered.add(items.getStackInSlot(0));
        offered.add(items.getStackInSlot(1));

        boolean[] used = new boolean[offered.size()];
        for (Ingredient need : rec.getIngredients()) {
            for (int i = 0; i < offered.size(); i++) {
                if (!used[i] && need.test(offered.get(i))) { used[i] = true; break; }
            }
        }
        for (int i = 0; i < used.length; i++) {
            if (used[i]) items.extractItem(i, 1, false);
        }

        if (rec.getCatalyst() != null && !rec.getCatalyst().isEmpty() && rec.isCatalystConsumed()) {
            items.extractItem(2, 1, false);
        }
    }
}
