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
import org.lyy.lyycore.content.ResourceFrameKind;

/** Four independent types share their wire format, not their recipe pools. */
public record ResourceGatheringRecipe(ResourceFrameKind kind, ItemStack result, int dryCost, int wetCost) implements Recipe<RecipeInput> {
    public ResourceGatheringRecipe {
        if (result.isEmpty() || result.getCount() != 1 || dryCost <= 0 || wetCost <= 0)
            throw new IllegalArgumentException("Gathering requires one result and positive IE costs");
        result = result.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    public int energyCost(boolean wet) { return kind.supportsWater() && wet ? wetCost : dryCost; }
    @Override public boolean matches(RecipeInput input, Level level) { return true; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return kind.serializer(); }
    @Override public RecipeType<?> getType() { return kind.recipeType(); }

    public static final class Serializer implements RecipeSerializer<ResourceGatheringRecipe> {
        private final MapCodec<ResourceGatheringRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ResourceGatheringRecipe> stream;
        public Serializer(ResourceFrameKind kind) {
            codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(ResourceGatheringRecipe::result),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("dry_cost", 200).forGetter(ResourceGatheringRecipe::dryCost),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("wet_cost", kind.supportsWater() ? 100 : 200).forGetter(ResourceGatheringRecipe::wetCost)
            ).apply(instance, (result, dry, wet) -> new ResourceGatheringRecipe(kind, result, dry, wet)));
            stream = StreamCodec.of((buf, recipe) -> {
                ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                buf.writeVarInt(recipe.dryCost);
                buf.writeVarInt(recipe.wetCost);
            }, buf -> new ResourceGatheringRecipe(kind, ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt()));
        }
        @Override public MapCodec<ResourceGatheringRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ResourceGatheringRecipe> streamCodec() { return stream; }
    }
}
