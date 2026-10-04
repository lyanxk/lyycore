package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.recipes.OctagonalRecipe;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.energy.ImaginaryEnergyFeAdapter;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

public final class ImaginaryCondensingBeaconBlockEntity extends AbstractImaginaryCraftingBlockEntity {
    public static final int CAPACITY = 100000;
    private final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(0, 0, 0) {
        @Override public int getMaxImaginaryEnergyStored() { return CAPACITY; }
        @Override public boolean canExtractImaginaryEnergy() { return false; }
        @Override public int extractImaginaryEnergy(int amount, boolean simulate) { return 0; }
        @Override public int receiveImaginaryEnergy(int amount, boolean simulate) {
            int received = super.receiveImaginaryEnergy(amount, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
    };
    private final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);
    public ImaginaryCondensingBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IMAGINARY_CONDENSING_BEACON.get(), pos, state);
    }
    public ImaginaryEnergyStorage energy() { return energy; }
    public ImaginaryEnergyFeAdapter fe() { return fe; }
    @Override protected int storedEnergy() { return energy.getImaginaryEnergyStored(); }
    @Override protected boolean consumeEnergy(int amount, boolean simulate) {
        if (storedEnergy() < amount) return false;
        if (!simulate && amount > 0) { energy.setImaginaryEnergy(storedEnergy() - amount); setChanged(); }
        return true;
    }
    @Override protected RecipeHolder<? extends OctagonalRecipe> findRecipe(RecipeInput input) {
        var condensed = level.getRecipeManager().getRecipeFor(LyyRecipes.IMAGINARY_CONDENSING.get(), input, level);
        return condensed.isPresent() ? condensed.get()
                : level.getRecipeManager().getRecipeFor(LyyRecipes.IMAGINARY_CRAFTING.get(), input, level).orElse(null);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IE", storedEnergy()); tag.putByte("FE", fe.getRemainder());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setImaginaryEnergy(Math.clamp(tag.getInt("IE"), 0, CAPACITY)); fe.setRemainder(tag.getByte("FE"));
    }
}
