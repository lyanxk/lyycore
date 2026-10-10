package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.client.screen.ForgeGui;
import org.lyy.lyycore.content.recipes.PureSmeltingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import java.util.Arrays;

public final class PureSmeltingCategory implements IRecipeCategory<PureSmeltingRecipe> {
    public static final RecipeType<PureSmeltingRecipe> TYPE = RecipeType.create("lyycore", "pure_smelting", PureSmeltingRecipe.class);
    private final IDrawable icon;
    public PureSmeltingCategory(IGuiHelper helper) { icon = helper.createDrawableItemStack(new ItemStack(LyyBlocks.PURE_SMELTING_PLANT.get())); }
    @Override public RecipeType<PureSmeltingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return LyyBlocks.PURE_SMELTING_PLANT.get().getName(); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 172; }
    @Override public int getHeight() { return 110; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, PureSmeltingRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            var input = recipe.inputs().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, 10+i%3*22, 9+i/3*22)
                    .addItemStacks(Arrays.stream(input.ingredient().getItems()).map(s -> s.copyWithCount(input.count())).toList());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 31).addItemStack(recipe.result().copyWithCount(recipe.resultCount()));
    }
    @Override public void draw(PureSmeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        for (int i = 0; i < 9; i++) ForgeGui.slot(g, 10+i%3*22, 9+i/3*22, false);
        ForgeGui.slot(g, 138, 31, true); ForgeGui.arrow(g, 98, 34, ForgeGui.ACCENT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("jei.lyycore.pure_smelting.time", recipe.duration()/20), 10, 82, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("jei.lyycore.pure_smelting.power"), 10, 96, ForgeGui.TEXT, false);
    }
}
