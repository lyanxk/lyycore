package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.recipes.PureSmeltingRecipe;
import org.lyy.lyycore.content.recipes.CrystalCondensingRecipe;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;
import org.lyy.lyycore.content.recipes.ImaginaryCondensingRecipe;
import org.lyy.lyycore.content.recipes.ResourceGatheringRecipe;
import java.util.EnumMap;
import java.util.Map;

public class LyyRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, LyyCore.MODID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<PureSmeltingRecipe>> PURE_SMELTING =
            RECIPE_TYPES.register("pure_smelting", () -> new RecipeType<>() { @Override public String toString() { return "lyycore:pure_smelting"; } });
    public static final DeferredHolder<RecipeType<?>, RecipeType<ImaginaryCondensingRecipe>> IMAGINARY_CONDENSING =
            RECIPE_TYPES.register("imaginary_condensing", () -> new RecipeType<>() {
                @Override public String toString() { return "lyycore:imaginary_condensing"; }
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<ImaginaryCraftingRecipe>> IMAGINARY_CRAFTING =
            RECIPE_TYPES.register("imaginary_crafting", () -> new RecipeType<>() {
                @Override public String toString() { return "lyycore:imaginary_crafting"; }
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<CrystalCondensingRecipe>> CRYSTAL_CONDENSING =
            RECIPE_TYPES.register("crystal_condensing", () -> new RecipeType<>() {
                @Override public String toString() { return LyyCore.MODID + ":crystal_condensing"; }
            });

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
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PureSmeltingRecipe>> PURE_SMELTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pure_smelting", PureSmeltingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ImaginaryCondensingRecipe>> IMAGINARY_CONDENSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("imaginary_condensing", ImaginaryCondensingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ImaginaryCraftingRecipe>> IMAGINARY_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("imaginary_crafting", ImaginaryCraftingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrystalCondensingRecipe>> CRYSTAL_CONDENSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crystal_condensing", CrystalCondensingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ImaginaryAlloyingRecipe>> IMAGINARY_ALLOYING_SERIALIZER =
            RECIPE_SERIALIZERS.register("imaginary_alloying", ImaginaryAlloyingRecipe.Serializer::new);

    public static final Map<ResourceFrameKind, DeferredHolder<RecipeType<?>, RecipeType<ResourceGatheringRecipe>>> RESOURCE_GATHERING = new EnumMap<>(ResourceFrameKind.class);
    public static final Map<ResourceFrameKind, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResourceGatheringRecipe>>> RESOURCE_GATHERING_SERIALIZERS = new EnumMap<>(ResourceFrameKind.class);
    static {
        for (var kind : ResourceFrameKind.values()) {
            RESOURCE_GATHERING.put(kind, RECIPE_TYPES.register(kind.recipeId(), () -> new RecipeType<>() {
                @Override public String toString() { return LyyCore.MODID + ":" + kind.recipeId(); }
            }));
            RESOURCE_GATHERING_SERIALIZERS.put(kind, RECIPE_SERIALIZERS.register(kind.recipeId(), () -> new ResourceGatheringRecipe.Serializer(kind)));
        }
    }
}
