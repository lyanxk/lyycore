package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.Codec;
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

/** Outer ring 0–7, then inner ring 8–15; both start at the top and run clockwise. */
public record SpacetimeRecipe(List<Input> inputs, ItemStack result, int resultCount, int duration, int energy, boolean energyPerTick)
        implements Recipe<RecipeInput> {
    public record Input(Ingredient ingredient, int count) {
        public static final Codec<Input> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Input::ingredient),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Input::count)).apply(i, Input::new));
    }
    public SpacetimeRecipe { inputs = List.copyOf(inputs); result = result.copyWithCount(1); }
    @Override public boolean matches(RecipeInput input, Level level) {
        if (input.size() < 16) return false;
        for (int slot = 0; slot < 16; slot++) {
            var required = inputs.get(slot);
            var stack = input.getItem(slot);
            if (!required.ingredient.test(stack) || !required.ingredient.isEmpty() && stack.getCount() < required.count) return false;
        }
        return true;
    }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result.copyWithCount(resultCount); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copyWithCount(resultCount); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 16; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return LyyRecipes.SPACETIME.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return LyyRecipes.SPACETIME_SERIALIZER.get(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        var ingredients = NonNullList.<Ingredient>create();
        inputs.forEach(input -> ingredients.add(input.ingredient));
        return ingredients;
    }
    public static final class Serializer implements RecipeSerializer<SpacetimeRecipe> {
        private static final MapCodec<SpacetimeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Input.CODEC.listOf(16, 16).fieldOf("inputs").forGetter(SpacetimeRecipe::inputs),
                ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(SpacetimeRecipe::result),
                Codec.intRange(1, 256).fieldOf("result_count").forGetter(SpacetimeRecipe::resultCount),
                Codec.intRange(1, 72000).fieldOf("duration").forGetter(SpacetimeRecipe::duration),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("energy").forGetter(SpacetimeRecipe::energy),
                Codec.BOOL.optionalFieldOf("energy_per_tick", false).forGetter(SpacetimeRecipe::energyPerTick)
        ).apply(i, SpacetimeRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, SpacetimeRecipe> STREAM = StreamCodec.of((b, r) -> {
            for (var input : r.inputs) { Ingredient.CONTENTS_STREAM_CODEC.encode(b, input.ingredient); b.writeVarInt(input.count); }
            ItemStack.STREAM_CODEC.encode(b, r.result);
            b.writeVarInt(r.resultCount); b.writeVarInt(r.duration); b.writeVarInt(r.energy); b.writeBoolean(r.energyPerTick);
        }, b -> {
            var inputs = new java.util.ArrayList<Input>(16);
            for (int slot = 0; slot < 16; slot++) inputs.add(new Input(Ingredient.CONTENTS_STREAM_CODEC.decode(b), b.readVarInt()));
            return new SpacetimeRecipe(inputs, ItemStack.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean());
        });
        @Override public MapCodec<SpacetimeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SpacetimeRecipe> streamCodec() { return STREAM; }
    }
}
