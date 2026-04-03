package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;

public class ImaginaryAlloyingCategory implements IRecipeCategory<ImaginaryAlloyingRecipe> {
    private static final ResourceLocation BG_TEX =
            ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "textures/gui/pure_white.png");

    private final IDrawable icon;
    private final IDrawableStatic background;

    public ImaginaryAlloyingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LyyBlocks.IAF.get()));
        this.background = guiHelper.createDrawable(BG_TEX, 0, 0, 176, 86);
    }

    @Override public RecipeType<ImaginaryAlloyingRecipe> getRecipeType() { return LyyJeiTypes.IMAGINARY_ALLOYING; }
    @Override public Component getTitle() { return Component.translatable("jei.lyycore.imaginary_alloying"); }
    @SuppressWarnings("removal")
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, ImaginaryAlloyingRecipe r, IFocusGroup focuses) {
        if (r.getIngredients().size() >= 1)
            b.addSlot(RecipeIngredientRole.INPUT, 34, 22).addIngredients(r.getIngredients().get(0));
        if (r.getIngredients().size() >= 2)
            b.addSlot(RecipeIngredientRole.INPUT, 50, 52).addIngredients(r.getIngredients().get(1));
        if (r.getCatalyst() != null && !r.getCatalyst().isEmpty())
            b.addSlot(RecipeIngredientRole.CATALYST, 66, 22).addIngredients(r.getCatalyst());
        b.addSlot(RecipeIngredientRole.OUTPUT, 122, 50).addItemStack(r.getResult());
    }
}
