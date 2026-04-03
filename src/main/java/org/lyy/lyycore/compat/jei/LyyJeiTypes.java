package org.lyy.lyycore.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;

public class LyyJeiTypes {
    public static final RecipeType<ImaginaryAlloyingRecipe> IMAGINARY_ALLOYING =
            RecipeType.create(LyyCore.MODID, "imaginary_alloying", ImaginaryAlloyingRecipe.class);
}
