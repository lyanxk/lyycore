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
import org.lyy.lyycore.content.FrameProduction;
import net.minecraft.world.level.block.Block;
import org.lyy.lyycore.content.recipes.CrystalCondensingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;

public class CrystalCondensingCategory implements IRecipeCategory<CrystalCondensingRecipe> {
    private final IDrawable icon;
    private final FrameProduction production;
    private final Block block;
    private boolean showsWaterCost() { return production == FrameProduction.STANDARD; }
    public CrystalCondensingCategory(IGuiHelper helper, FrameProduction production) {
        this.production = production;
        block = production == FrameProduction.MINIATURE_FACTORY ? LyyBlocks.MINIATURE_CRYSTAL_FACTORY.get() : LyyBlocks.CRYSTAL_CONDENSING_FRAME.get();
        icon = helper.createDrawableItemStack(new ItemStack(block));
    }
    @Override public RecipeType<CrystalCondensingRecipe> getRecipeType() { return LyyJeiTypes.crystal(production); }
    @Override public Component getTitle() { return block.getName(); }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return showsWaterCost() ? 94 : 80; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, CrystalCondensingRecipe recipe, IFocusGroup focuses) {
        if (showsWaterCost()) builder.addSlot(RecipeIngredientRole.CATALYST, 18, 26).addItemStack(new ItemStack(Items.WATER_BUCKET))
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.lyycore.condensing.water")));
        else builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 18, 26).addItemStack(new ItemStack(block));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 26).addItemStack(recipe.result().copyWithCount(production.batchSize()));
    }
    @Override public void draw(CrystalCondensingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        ForgeGui.frame(g, 104, 13, true);
        ForgeGui.slot(g, 18, 26, false);
        ForgeGui.slot(g, 120, 26, true);
        ForgeGui.arrow(g, 64, 28, ForgeGui.ACCENT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable(showsWaterCost() ? "jei.lyycore.condensing.optional_water" : "jei.lyycore.gathering.production"), 8, 8, ForgeGui.TEXT, false);
        g.fill(8, 55, 168, 56, ForgeGui.TRACK);
        g.drawString(font, Component.translatable(showsWaterCost() ? "jei.lyycore.condensing.dry" : "jei.lyycore.factory.cost", production.energyCost(recipe.dryCost()), production.batchSize()), 8, 63, ForgeGui.TEXT, false);
        if (showsWaterCost()) g.drawString(font, Component.translatable("jei.lyycore.condensing.wet", recipe.wetCost()), 8, 77, ForgeGui.TEXT, false);
    }
}
