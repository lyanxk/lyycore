package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.menu.ForgeGui;
import org.lyy.lyycore.content.menu.ImaginaryCraftingGui;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;

public final class ImaginaryCraftingCategory implements IRecipeCategory<ImaginaryCraftingRecipe> {
    private static final int DIAGRAM_X = -30, DIAGRAM_Y = -12;
    private static final int OUTPUT_X = 150, OUTPUT_Y = 50;
    private final IDrawable icon;

    public ImaginaryCraftingCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(LyyBlocks.IMAGINARY_CRAFTING_TABLE.get()));
    }

    @Override public RecipeType<ImaginaryCraftingRecipe> getRecipeType() { return LyyJeiTypes.IMAGINARY_CRAFTING; }
    @Override public Component getTitle() { return Component.translatable("block.lyycore.imaginary_crafting_table"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 178; }
    @Override public int getHeight() { return 150; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ImaginaryCraftingRecipe recipe, IFocusGroup focuses) {
        // Preserve menu order (center, then clockwise from the top) for JEI ingredient transfer.
        for (int slot = 0; slot < ImaginaryCraftingMenu.POSITIONS.length; slot++) {
            int[] position = ImaginaryCraftingMenu.POSITIONS[slot];
            builder.addSlot(RecipeIngredientRole.INPUT, position[0] + DIAGRAM_X, position[1] + DIAGRAM_Y)
                    .addIngredients(recipe.ingredients().get(slot));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.result())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                        Component.translatable("screen.lyycore.imaginary_crafting.returns_to_center")));
    }

    @Override
    public void draw(ImaginaryCraftingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        long durationMillis = ImaginaryCraftingRecipe.DURATION * 50L;
        float progress = (Util.getMillis() % durationMillis) / (float) durationMillis;
        ImaginaryCraftingGui.diagram(g, DIAGRAM_X, DIAGRAM_Y, progress);
        ForgeGui.arrow(g, 120, OUTPUT_Y + 2, ForgeGui.ACCENT);
        ForgeGui.slot(g, OUTPUT_X, OUTPUT_Y, true);
        var font = Minecraft.getInstance().font;
        Component outputLabel = Component.translatable("tooltip.lyycore.forge.output");
        g.drawString(font, outputLabel, OUTPUT_X + 8 - font.width(outputLabel) / 2, 34, ForgeGui.TEXT, false);
        g.fill(8, 116, getWidth() - 8, 117, ForgeGui.TRACK);
        g.drawString(font, Component.translatable("screen.lyycore.imaginary_crafting.time",
                ImaginaryCraftingRecipe.DURATION / 20), 8, 121, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.imaginary_crafting.returns_to_center"),
                8, 133, ForgeGui.TEXT, false);
    }
}
