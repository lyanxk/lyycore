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
import net.minecraft.world.item.Items;
import org.lyy.lyycore.content.menu.ForgeGui;
import org.lyy.lyycore.content.recipes.CrystalCondensingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;

public class CrystalCondensingCategory implements IRecipeCategory<CrystalCondensingRecipe> {
    private final IDrawable icon;
    public CrystalCondensingCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(LyyBlocks.CRYSTAL_CONDENSING_FRAME.get()));
    }
    @Override public RecipeType<CrystalCondensingRecipe> getRecipeType() { return LyyJeiTypes.CRYSTAL_CONDENSING; }
    @Override public Component getTitle() { return Component.translatable("jei.lyycore.crystal_condensing"); }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 94; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, CrystalCondensingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 18, 26).addItemStack(new ItemStack(Items.WATER_BUCKET))
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.lyycore.condensing.water")));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 26).addItemStack(recipe.result());
    }
    @Override public void draw(CrystalCondensingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        ForgeGui.frame(g, 104, 13, true);
        ForgeGui.slot(g, 18, 26, false);
        ForgeGui.slot(g, 120, 26, true);
        ForgeGui.arrow(g, 64, 28, ForgeGui.ACCENT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("jei.lyycore.condensing.optional_water"), 8, 8, ForgeGui.TEXT, false);
        g.fill(8, 55, 168, 56, ForgeGui.TRACK);
        g.drawString(font, Component.translatable("jei.lyycore.condensing.dry", recipe.dryCost()), 8, 63, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("jei.lyycore.condensing.wet", recipe.wetCost()), 8, 77, ForgeGui.TEXT, false);
    }
}
