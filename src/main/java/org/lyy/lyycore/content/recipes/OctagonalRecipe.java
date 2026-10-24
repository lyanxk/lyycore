package org.lyy.lyycore.content.recipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Center first, then eight positions clockwise from the top. The result is a recipe template. */
public interface OctagonalRecipe extends Recipe<RecipeInput> {
    List<Ingredient> ingredients();
    ItemStack result();
    default int duration() { return 100; }
    default int energy() { return 0; }
    default boolean energyPerTick() { return false; }
    @Override default boolean matches(RecipeInput input, Level level) {
        for (int i = 0; i < 9; i++) if (!ingredients().get(i).test(input.getItem(i))) return false;
        return true;
    }
    @Override default NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredients().toArray(Ingredient[]::new));
    }
    @Override default ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result().copy(); }
    @Override default ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override default boolean canCraftInDimensions(int width, int height) { return true; }
    @Override default boolean isSpecial() { return true; }
}
