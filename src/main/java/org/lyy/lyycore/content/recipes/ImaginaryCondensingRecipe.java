package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;

public record ImaginaryCondensingRecipe(List<Ingredient> ingredients, ItemStack result, int duration, int energy) implements OctagonalRecipe {
    public ImaginaryCondensingRecipe {
        OctagonalRecipe.validate(ingredients, result);
        if (duration < 1 || duration > 32767 || energy < 0 || energy > 100000)
            throw new IllegalArgumentException("Invalid condensing duration or energy");
        ingredients = List.copyOf(ingredients);
        result = result.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public RecipeSerializer<?> getSerializer() { return LyyRecipes.IMAGINARY_CONDENSING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return LyyRecipes.IMAGINARY_CONDENSING.get(); }
    public static final class Serializer implements RecipeSerializer<ImaginaryCondensingRecipe> {
        private static final MapCodec<ImaginaryCondensingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(ImaginaryCondensingRecipe::ingredients),
                ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(ImaginaryCondensingRecipe::result),
                Codec.intRange(1, 32767).optionalFieldOf("duration", 100).forGetter(ImaginaryCondensingRecipe::duration),
                Codec.intRange(0, 100000).fieldOf("energy").forGetter(ImaginaryCondensingRecipe::energy)
        ).apply(instance, ImaginaryCondensingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ImaginaryCondensingRecipe> STREAM = StreamCodec.of((buf, recipe) -> {
            for (var ingredient : recipe.ingredients) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.duration); buf.writeVarInt(recipe.energy);
        }, buf -> {
            var ingredients = new java.util.ArrayList<Ingredient>();
            for (int i = 0; i < 9; i++) ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new ImaginaryCondensingRecipe(ingredients, ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt());
        });
        @Override public MapCodec<ImaginaryCondensingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ImaginaryCondensingRecipe> streamCodec() { return STREAM; }
    }
}
