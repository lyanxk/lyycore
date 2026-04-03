package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;

public class LyyRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, LyyCore.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ImaginaryAlloyingRecipe>> IMAGINARY_ALLOYING =
            RECIPE_TYPES.register("imaginary_alloying",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return LyyCore.MODID + ":imaginary_alloying";
                        }
                    });

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, LyyCore.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ImaginaryAlloyingRecipe>> IMAGINARY_ALLOYING_SERIALIZER =
            RECIPE_SERIALIZERS.register("imaginary_alloying", ImaginaryAlloyingRecipe.Serializer::new);
}
