package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;

/** One or two unordered inputs; slot two holds an optional catalyst. Results are templates. */
public record ImaginaryAlloyingRecipe(List<Ingredient> inputs, Ingredient catalyst, boolean catalystConsumed,
                                     ItemStack result, int processTime, int energyCost) implements Recipe<RecipeInput> {
    @Override public boolean matches(RecipeInput input, Level level) {
        if (!catalyst.isEmpty() && !catalyst.test(input.getItem(2))) return false;
        var first = inputs.getFirst();
        var second = inputs.size() == 2 ? inputs.get(1) : Ingredient.EMPTY;
        return first.test(input.getItem(0)) && second.test(input.getItem(1))
                || first.test(input.getItem(1)) && second.test(input.getItem(0));
    }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return LyyRecipes.IMAGINARY_ALLOYING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return LyyRecipes.IMAGINARY_ALLOYING.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, inputs.toArray(Ingredient[]::new)); }

    /** A no-op reload preserves work; a changed definition starts a new job. */
    public boolean sameDefinitionAs(ImaginaryAlloyingRecipe other) {
        return inputs.equals(other.inputs) && catalyst.equals(other.catalyst)
                && catalystConsumed == other.catalystConsumed && ItemStack.matches(result, other.result)
                && processTime == other.processTime && energyCost == other.energyCost;
    }

    public static final class Serializer implements RecipeSerializer<ImaginaryAlloyingRecipe> {
        private static final MapCodec<ImaginaryAlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.listOf(1, 2).fieldOf("inputs").forGetter(ImaginaryAlloyingRecipe::inputs),
                Ingredient.CODEC.optionalFieldOf("catalyst", Ingredient.EMPTY).forGetter(ImaginaryAlloyingRecipe::catalyst),
                Codec.BOOL.optionalFieldOf("catalyst_consumed", false).forGetter(ImaginaryAlloyingRecipe::catalystConsumed),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ImaginaryAlloyingRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE / 20).optionalFieldOf("process_time", 200).forGetter(ImaginaryAlloyingRecipe::processTime),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy_cost", 0).forGetter(ImaginaryAlloyingRecipe::energyCost)
        ).apply(i, ImaginaryAlloyingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ImaginaryAlloyingRecipe> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), ImaginaryAlloyingRecipe::inputs,
                Ingredient.CONTENTS_STREAM_CODEC, ImaginaryAlloyingRecipe::catalyst,
                ByteBufCodecs.BOOL, ImaginaryAlloyingRecipe::catalystConsumed,
                ItemStack.STREAM_CODEC, ImaginaryAlloyingRecipe::result,
                ByteBufCodecs.VAR_INT, ImaginaryAlloyingRecipe::processTime,
                ByteBufCodecs.VAR_INT, ImaginaryAlloyingRecipe::energyCost,
                ImaginaryAlloyingRecipe::new);
        @Override public MapCodec<ImaginaryAlloyingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ImaginaryAlloyingRecipe> streamCodec() { return STREAM; }
    }
}
