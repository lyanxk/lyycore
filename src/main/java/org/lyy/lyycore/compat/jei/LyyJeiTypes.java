package org.lyy.lyycore.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;

public class LyyJeiTypes {
    public static final java.util.Map<org.lyy.lyycore.content.ResourceFrameKind, RecipeType<org.lyy.lyycore.content.recipes.ResourceGatheringRecipe>> RESOURCE_GATHERING = new java.util.EnumMap<>(org.lyy.lyycore.content.ResourceFrameKind.class);
    static {
        for (var kind : org.lyy.lyycore.content.ResourceFrameKind.values()) RESOURCE_GATHERING.put(kind,
                RecipeType.create(LyyCore.MODID, kind.recipeId(), org.lyy.lyycore.content.recipes.ResourceGatheringRecipe.class));
    }
    public static final RecipeType<org.lyy.lyycore.content.recipes.CrystalCondensingRecipe> CRYSTAL_CONDENSING =
            RecipeType.create(LyyCore.MODID, "crystal_condensing", org.lyy.lyycore.content.recipes.CrystalCondensingRecipe.class);
    public static final RecipeType<ImaginaryAlloyingRecipe> IMAGINARY_ALLOYING =
            RecipeType.create(LyyCore.MODID, "imaginary_alloying", ImaginaryAlloyingRecipe.class);
}
