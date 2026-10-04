package org.lyy.lyycore.content.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.registry.LyyRecipes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImaginaryAlloyingRecipe implements Recipe<RecipeInput> {
    private final List<Ingredient> inputs;
    @Nullable private final Ingredient catalyst;
    private final boolean catalystConsumed;
    private final ItemStack result;
    private final int processTime;
    private final int energyCost;

    public ImaginaryAlloyingRecipe(List<Ingredient> inputs, @Nullable Ingredient catalyst,
                                   boolean catalystConsumed, ItemStack result,
                                   int processTime, int energyCost) {
        this.inputs = List.copyOf(inputs);
        this.catalyst = catalyst;
        this.catalystConsumed = catalystConsumed;
        this.result = result;
        this.processTime = processTime;
        this.energyCost = energyCost;
    }

    // Factory for codec (handles Optional catalyst)
    public static ImaginaryAlloyingRecipe create(List<Ingredient> inputs, Optional<Ingredient> catalyst,
                                                  boolean catalystConsumed, ItemStack result,
                                                  int processTime, int energyCost) {
        return new ImaginaryAlloyingRecipe(inputs, catalyst.orElse(null), catalystConsumed, result, processTime, energyCost);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return matchedInputSlots(input) != null;
    }

    /** The same assignment is used for matching and consumption, including overlapping ingredients. */
    @Nullable public int[] matchedInputSlots(RecipeInput input) {
        if (input.size() < 3) return null;
        // Slot 0,1 = normal inputs (unordered), slot 2 = catalyst
        if (catalyst != null && !catalyst.isEmpty()) {
            if (!catalyst.test(input.getItem(2))) return null;
        }

        List<Integer> offered = new ArrayList<>();
        if (!input.getItem(0).isEmpty()) offered.add(0);
        if (!input.getItem(1).isEmpty()) offered.add(1);

        List<Ingredient> required = new ArrayList<>();
        for (Ingredient ing : inputs) { if (!ing.isEmpty()) required.add(ing); }

        if (offered.size() != required.size()) return null;
        if (offered.isEmpty()) return new int[0];
        int first = offered.getFirst();
        if (offered.size() == 1)
            return required.getFirst().test(input.getItem(first)) ? new int[]{first} : null;
        int second = offered.get(1);
        if (required.get(0).test(input.getItem(first)) && required.get(1).test(input.getItem(second))) {
            return new int[]{first, second};
        }
        if (required.get(0).test(input.getItem(second)) && required.get(1).test(input.getItem(first))) {
            return new int[]{second, first};
        }
        return null;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return LyyRecipes.IMAGINARY_ALLOYING_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return LyyRecipes.IMAGINARY_ALLOYING.get(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(inputs);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    // --- Getters ---
    public List<Ingredient> getInputs() { return inputs; }
    @Nullable public Ingredient getCatalyst() { return catalyst; }
    public Optional<Ingredient> getCatalystOptional() { return Optional.ofNullable(catalyst); }
    public boolean isCatalystConsumed() { return catalystConsumed; }
    public ItemStack getResult() { return result; }
    public int getProcessTime() { return processTime; }
    public int getEnergyCost() { return energyCost; }

    /** A no-op reload can preserve work; changed definitions must start a new job. */
    public boolean sameDefinitionAs(ImaginaryAlloyingRecipe other) {
        return inputs.equals(other.inputs) && java.util.Objects.equals(catalyst, other.catalyst)
                && catalystConsumed == other.catalystConsumed && ItemStack.matches(result, other.result)
                && processTime == other.processTime && energyCost == other.energyCost;
    }

    // --- Serializer ---
    public static class Serializer implements RecipeSerializer<ImaginaryAlloyingRecipe> {
        private static final MapCodec<ImaginaryAlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(ImaginaryAlloyingRecipe::getInputs),
                Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(ImaginaryAlloyingRecipe::getCatalystOptional),
                Codec.BOOL.optionalFieldOf("catalyst_consumed", false).forGetter(ImaginaryAlloyingRecipe::isCatalystConsumed),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ImaginaryAlloyingRecipe::getResult),
                Codec.INT.optionalFieldOf("process_time", 200).forGetter(ImaginaryAlloyingRecipe::getProcessTime),
                Codec.INT.optionalFieldOf("energy_cost", 0).forGetter(ImaginaryAlloyingRecipe::getEnergyCost)
        ).apply(inst, ImaginaryAlloyingRecipe::create));

        private static final StreamCodec<RegistryFriendlyByteBuf, ImaginaryAlloyingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        private static void toNetwork(RegistryFriendlyByteBuf buf, ImaginaryAlloyingRecipe r) {
            buf.writeVarInt(r.inputs.size());
            for (Ingredient ing : r.inputs) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
            buf.writeBoolean(r.catalyst != null);
            if (r.catalyst != null) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.catalyst);
            buf.writeBoolean(r.catalystConsumed);
            ItemStack.STREAM_CODEC.encode(buf, r.result);
            buf.writeVarInt(r.processTime);
            buf.writeVarInt(r.energyCost);
        }

        private static ImaginaryAlloyingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<Ingredient> inputs = new ArrayList<>();
            for (int i = 0; i < size; i++) inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            Ingredient catalyst = buf.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode(buf) : null;
            boolean consumed = buf.readBoolean();
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int time = buf.readVarInt();
            int energy = buf.readVarInt();
            return new ImaginaryAlloyingRecipe(inputs, catalyst, consumed, result, time, energy);
        }

        @Override public MapCodec<ImaginaryAlloyingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ImaginaryAlloyingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
