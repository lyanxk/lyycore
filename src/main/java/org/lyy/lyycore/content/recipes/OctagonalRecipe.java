package org.lyy.lyycore.content.recipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Center first, then eight positions clockwise from the top. */
public interface OctagonalRecipe extends Recipe<RecipeInput> {
    List<Ingredient> ingredients();
    ItemStack result();
    default int duration() { return 100; }
    default int energy() { return 0; }
    default boolean energyPerTick() { return false; }
    static void validate(List<Ingredient> ingredients, ItemStack result) {
        if (ingredients.size() != 9 || result.isEmpty() || result.getCount() != 1)
            throw new IllegalArgumentException("Octagonal crafting requires nine positions and one output");
    }
    @Override default boolean matches(RecipeInput input, Level level) {
        if (input.size() != 9) return false;
        for (int i = 0; i < 9; i++) if (!ingredients().get(i).test(input.getItem(i))) return false;
        return true;
    }
    @Override default NonNullList<Ingredient> getIngredients() {
        var result = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int i = 0; i < 9; i++) result.set(i, ingredients().get(i));
        return result;
    }
    @Override default ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result(); }
    @Override default ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override default boolean canCraftInDimensions(int width, int height) { return true; }
    @Override default boolean isSpecial() { return true; }
}
