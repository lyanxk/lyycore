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
import net.minecraft.world.item.*;
import org.lyy.lyycore.client.screen.ForgeGui;
import org.lyy.lyycore.content.cauldron.CauldronMixes;
import org.lyy.lyycore.registry.LyyBlocks;

public final class AlloyCauldronCategory implements IRecipeCategory<CauldronMixes.Special> {
    public static final RecipeType<CauldronMixes.Special> TYPE = RecipeType.create("lyycore", "alloy_cauldron", CauldronMixes.Special.class);
    private final IDrawable icon;
    public AlloyCauldronCategory(IGuiHelper helper) { icon = helper.createDrawableItemStack(new ItemStack(LyyBlocks.ALLOY_CAULDRON.get())); }
    @Override public RecipeType<CauldronMixes.Special> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return LyyBlocks.ALLOY_CAULDRON.get().getName(); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 168; }
    @Override public int getHeight() { return 94; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, CauldronMixes.Special recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.ingredients().size(); i++) builder.addSlot(RecipeIngredientRole.INPUT, 10 + i % 4 * 22, 9 + i / 4 * 22).addItemStack(recipe.ingredients().get(i));
        builder.addSlot(RecipeIngredientRole.INPUT, 105, 39).addItemStack(new ItemStack(Items.GLASS_BOTTLE));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 139, 16).addItemStack(new ItemStack(recipe.result()));
    }
    @Override public void draw(CauldronMixes.Special recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        for (int i = 0; i < recipe.ingredients().size(); i++) ForgeGui.slot(g, 10 + i % 4 * 22, 9 + i / 4 * 22, false);
        ForgeGui.slot(g, 105, 39, false); ForgeGui.slot(g, 139, 16, true); ForgeGui.arrow(g, 105, 18, ForgeGui.ACCENT);
        g.drawWordWrap(Minecraft.getInstance().font, Component.translatable("jei.lyycore.cauldron.instructions"), 10, 65, 148, ForgeGui.TEXT);
    }
}
