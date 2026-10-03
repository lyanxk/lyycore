package org.lyy.lyycore.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.FrameProduction;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.recipes.CrystalCondensingRecipe;
import org.lyy.lyycore.content.recipes.ResourceGatheringRecipe;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;

import java.util.EnumMap;
import java.util.Map;

public class LyyJeiTypes {
    public static final RecipeType<ImaginaryCraftingRecipe> IMAGINARY_CRAFTING =
            RecipeType.create(LyyCore.MODID, "imaginary_crafting", ImaginaryCraftingRecipe.class);
    public static final Map<ResourceFrameKind, RecipeType<ResourceGatheringRecipe>> RESOURCE_GATHERING = new EnumMap<>(ResourceFrameKind.class);
    static {
        for (var kind : ResourceFrameKind.values()) RESOURCE_GATHERING.put(kind,
                RecipeType.create(LyyCore.MODID, kind.recipeId(), ResourceGatheringRecipe.class));
    }
    public static final RecipeType<CrystalCondensingRecipe> CRYSTAL_CONDENSING =
            RecipeType.create(LyyCore.MODID, "crystal_condensing", CrystalCondensingRecipe.class);
    public static final RecipeType<CrystalCondensingRecipe> MINIATURE_CRYSTAL =
            RecipeType.create(LyyCore.MODID, "miniature_crystal_factory", CrystalCondensingRecipe.class);
    public static final Map<ResourceFrameKind, RecipeType<ResourceGatheringRecipe>> RESOURCE_FACTORIES = new EnumMap<>(ResourceFrameKind.class);
    static {
        for (var kind : ResourceFrameKind.FACTORY_KINDS) RESOURCE_FACTORIES.put(kind,
                RecipeType.create(LyyCore.MODID, kind.factoryId(), ResourceGatheringRecipe.class));
    }
    public static RecipeType<CrystalCondensingRecipe> crystal(FrameProduction production) {
        return production == FrameProduction.MINIATURE_FACTORY ? MINIATURE_CRYSTAL : CRYSTAL_CONDENSING;
    }
    public static RecipeType<ResourceGatheringRecipe> gathering(ResourceFrameKind kind, FrameProduction production) {
        return (production == FrameProduction.MINIATURE_FACTORY ? RESOURCE_FACTORIES : RESOURCE_GATHERING).get(kind);
    }
    public static final RecipeType<ImaginaryAlloyingRecipe> IMAGINARY_ALLOYING =
            RecipeType.create(LyyCore.MODID, "imaginary_alloying", ImaginaryAlloyingRecipe.class);
}
