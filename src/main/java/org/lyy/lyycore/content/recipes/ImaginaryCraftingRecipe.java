package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;

/** Slot 0 is the center; slots 1–8 run clockwise from the top. */
public record ImaginaryCraftingRecipe(List<Ingredient> ingredients, ItemStack result) implements Recipe<RecipeInput> {
    public static final int DURATION = 100;

    public ImaginaryCraftingRecipe {
        if (ingredients.size() != 9 || result.isEmpty() || result.getCount() != 1)
            throw new IllegalArgumentException("Imaginary crafting requires nine positions and one output");
        ingredients = List.copyOf(ingredients);
        result = result.copy();
    }

    @Override public ItemStack result() { return result.copy(); }
    @Override public boolean matches(RecipeInput input, Level level) {
        if (input.size() != 9) return false;
        for (int slot = 0; slot < 9; slot++) {
            if (!ingredients.get(slot).test(input.getItem(slot))) return false;
        }
        return true;
    }
    @Override public NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int i = 0; i < 9; i++) list.set(i, ingredients.get(i));
        return list;
    }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return LyyRecipes.IMAGINARY_CRAFTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return LyyRecipes.IMAGINARY_CRAFTING.get(); }

    public static final class Serializer implements RecipeSerializer<ImaginaryCraftingRecipe> {
        private static final MapCodec<ImaginaryCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(ImaginaryCraftingRecipe::ingredients),
                ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(ImaginaryCraftingRecipe::result)
        ).apply(instance, ImaginaryCraftingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ImaginaryCraftingRecipe> STREAM = StreamCodec.of(
                (buf, recipe) -> {
                    for (Ingredient ingredient : recipe.ingredients) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                    ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                }, buf -> {
                    var ingredients = new java.util.ArrayList<Ingredient>(9);
                    for (int i = 0; i < 9; i++) ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    return new ImaginaryCraftingRecipe(ingredients, ItemStack.STREAM_CODEC.decode(buf));
                });
        @Override public MapCodec<ImaginaryCraftingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ImaginaryCraftingRecipe> streamCodec() { return STREAM; }
    }
}
