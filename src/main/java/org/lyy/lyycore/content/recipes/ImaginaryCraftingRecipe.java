package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;

/** Slot 0 is the center; slots 1–8 run clockwise from the top. */
public record ImaginaryCraftingRecipe(List<Ingredient> ingredients, ItemStack result) implements OctagonalRecipe {
    public static final int DURATION = 100;

    public ImaginaryCraftingRecipe {
        OctagonalRecipe.validate(ingredients, result);
        ingredients = List.copyOf(ingredients);
        result = result.copy();
    }

    @Override public ItemStack result() { return result.copy(); }
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
