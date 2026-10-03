package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.registry.LyyRecipes;

/** Condensation has no item inputs. Water changes the cost and is not consumed. */
public record CrystalCondensingRecipe(ItemStack result, int dryCost, int wetCost) implements Recipe<RecipeInput> {
    public CrystalCondensingRecipe {
        if (result.isEmpty() || result.getCount() != 1 || dryCost <= 0 || wetCost <= 0)
            throw new IllegalArgumentException("Condensation requires one output and positive IE costs");
        result = result.copy();
    }

    @Override public ItemStack result() { return result.copy(); }
    public int energyCost(boolean waterlogged) { return waterlogged ? wetCost : dryCost; }
    @Override public boolean matches(RecipeInput input, Level level) { return true; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return LyyRecipes.CRYSTAL_CONDENSING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return LyyRecipes.CRYSTAL_CONDENSING.get(); }

    public static class Serializer implements RecipeSerializer<CrystalCondensingRecipe> {
        private static final MapCodec<CrystalCondensingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(CrystalCondensingRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("dry_cost").forGetter(CrystalCondensingRecipe::dryCost),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("wet_cost").forGetter(CrystalCondensingRecipe::wetCost)
        ).apply(instance, CrystalCondensingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, CrystalCondensingRecipe> STREAM = StreamCodec.of(
                (buf, recipe) -> {
                    ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                    buf.writeVarInt(recipe.dryCost);
                    buf.writeVarInt(recipe.wetCost);
                }, buf -> new CrystalCondensingRecipe(ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt()));
        @Override public MapCodec<CrystalCondensingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CrystalCondensingRecipe> streamCodec() { return STREAM; }
    }
}
