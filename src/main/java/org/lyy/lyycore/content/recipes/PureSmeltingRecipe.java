package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;

/** Counted, unordered input stacks; matching and payment use the same assignment. */
public record PureSmeltingRecipe(List<Input> inputs, ItemStack result, int resultCount, int duration) implements Recipe<RecipeInput> {
    public record Input(Ingredient ingredient, int count) {
        public static final Codec<Input> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Input::ingredient),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Input::count)).apply(i, Input::new));
    }
    public PureSmeltingRecipe { inputs = List.copyOf(inputs); result = result.copy(); }
    public int[] assignment(RecipeInput offered) {
        int[] slots = new int[inputs.size()];
        return assign(offered, slots, 0, 0) ? slots : null;
    }
    private boolean assign(RecipeInput offered, int[] slots, int index, int used) {
        if (index == inputs.size()) return true;
        var required = inputs.get(index);
        for (int slot = 0; slot < Math.min(9, offered.size()); slot++) {
            var stack = offered.getItem(slot);
            if ((used & 1 << slot) != 0 || stack.getCount() < required.count || !required.ingredient.test(stack)) continue;
            slots[index] = slot;
            if (assign(offered, slots, index + 1, used | 1 << slot)) return true;
        }
        return false;
    }
    @Override public boolean matches(RecipeInput input, Level level) { return assignment(input) != null; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result.copyWithCount(resultCount); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copyWithCount(resultCount); }
    @Override public boolean canCraftInDimensions(int w, int h) { return w*h >= inputs.size(); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return LyyRecipes.PURE_SMELTING.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return LyyRecipes.PURE_SMELTING_SERIALIZER.get(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.<Ingredient>create(); inputs.forEach(i -> list.add(i.ingredient)); return list;
    }
    public static final class Serializer implements RecipeSerializer<PureSmeltingRecipe> {
        private static final MapCodec<PureSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Input.CODEC.listOf(1, 9).fieldOf("inputs").forGetter(PureSmeltingRecipe::inputs),
                ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(PureSmeltingRecipe::result),
                Codec.intRange(1, 256).fieldOf("result_count").forGetter(PureSmeltingRecipe::resultCount),
                Codec.intRange(1, 72000).fieldOf("duration").forGetter(PureSmeltingRecipe::duration)).apply(i, PureSmeltingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, PureSmeltingRecipe> STREAM = StreamCodec.of((b, r) -> {
            b.writeCollection(r.inputs, (buf, input) -> { Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, input.ingredient); buf.writeVarInt(input.count); });
            ItemStack.STREAM_CODEC.encode(b, r.result); b.writeVarInt(r.resultCount); b.writeVarInt(r.duration);
        }, b -> new PureSmeltingRecipe(b.readList(buf -> new Input(Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf), buf.readVarInt())),
                ItemStack.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt()));
        @Override public MapCodec<PureSmeltingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PureSmeltingRecipe> streamCodec() { return STREAM; }
    }
}
