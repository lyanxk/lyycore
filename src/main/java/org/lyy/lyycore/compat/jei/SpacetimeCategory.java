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
import org.lyy.lyycore.client.screen.SpacetimeGui;
import org.lyy.lyycore.content.recipes.SpacetimeRecipe;
import org.lyy.lyycore.registry.LyyBlocks;

import java.util.Arrays;

public final class SpacetimeCategory implements IRecipeCategory<SpacetimeRecipe> {
    public static final RecipeType<SpacetimeRecipe> TYPE = RecipeType.create("lyycore", "spacetime", SpacetimeRecipe.class);
    private static final int CENTER_X = 105, CENTER_Y = 86;
    private final IDrawable icon;
    public SpacetimeCategory(IGuiHelper helper) { icon = helper.createDrawableItemStack(new ItemStack(LyyBlocks.SPACETIME_FACTORY.get())); }
    @Override public RecipeType<SpacetimeRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.lyycore.spacetime"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 226; }
    @Override public int getHeight() { return 207; }
    private static int x(int slot) { return CENTER_X + (int) Math.round(Math.sin(slot % 8 * Math.PI / 4) * (slot < 8 ? 73 : 39)); }
    private static int y(int slot) { return CENTER_Y - (int) Math.round(Math.cos(slot % 8 * Math.PI / 4) * (slot < 8 ? 73 : 39)); }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, SpacetimeRecipe recipe, IFocusGroup focuses) {
        for (int slot = 0; slot < 16; slot++) {
            var input = recipe.inputs().get(slot);
            builder.addSlot(RecipeIngredientRole.INPUT, x(slot), y(slot)).addItemStacks(Arrays.stream(input.ingredient().getItems())
                    .map(stack -> stack.copyWithCount(input.count())).toList());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, CENTER_X, CENTER_Y).addItemStack(recipe.result().copyWithCount(recipe.resultCount()));
    }
    @Override public void draw(SpacetimeRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        SpacetimeGui.panel(g, 0, 0, getWidth(), getHeight());
        for (int slot = 0; slot < 16; slot++) {
            int next = slot / 8 * 8 + (slot + 1) % 8;
            int color = slot < 8 ? SpacetimeGui.PINK : SpacetimeGui.BLUE;
            SpacetimeGui.line(g, x(slot) + 8, y(slot) + 8, x(next) + 8, y(next) + 8, color, 1);
        }
        for (int slot = 0; slot < 16; slot++) SpacetimeGui.slot(g, x(slot), y(slot), slot < 8 ? SpacetimeGui.PINK : SpacetimeGui.BLUE);
        SpacetimeGui.slot(g, CENTER_X, CENTER_Y, SpacetimeGui.PINK);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("jei.lyycore.spacetime.time", recipe.duration() / 20), 10, 177, SpacetimeGui.TEXT, false);
        g.drawString(font, Component.translatable("jei.lyycore.spacetime.energy", String.format(java.util.Locale.ROOT, "%,d", recipe.energy())),
                10, 191, SpacetimeGui.TEXT, false);
    }
}
