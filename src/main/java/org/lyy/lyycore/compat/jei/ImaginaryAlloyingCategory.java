package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.menu.ForgeGui;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;

public class ImaginaryAlloyingCategory implements IRecipeCategory<ImaginaryAlloyingRecipe> {
    private static final int INPUT_X = 16, INPUT_A_Y = 26, INPUT_B_Y = 50;
    private static final int CATALYST_X = 62, OUTPUT_X = 136, SLOT_Y = 38;
    private final IDrawable icon;

    public ImaginaryAlloyingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(LyyBlocks.IAF.get()));
    }

    @Override public RecipeType<ImaginaryAlloyingRecipe> getRecipeType() { return LyyJeiTypes.IMAGINARY_ALLOYING; }
    @Override public Component getTitle() { return Component.translatable("jei.lyycore.imaginary_alloying"); }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 104; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ImaginaryAlloyingRecipe recipe, IFocusGroup focuses) {
        // Keep all three input views in menu order, even for recipes with an empty slot.
        IRecipeSlotBuilder inputA = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_A_Y);
        IRecipeSlotBuilder inputB = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_B_Y);
        if (!recipe.getInputs().isEmpty()) inputA.addIngredients(recipe.getInputs().get(0));
        if (recipe.getInputs().size() >= 2) inputB.addIngredients(recipe.getInputs().get(1));

        // JEI's standard transfer handler only transfers INPUT roles, including reusable ingredients.
        IRecipeSlotBuilder catalyst = builder.addSlot(RecipeIngredientRole.INPUT, CATALYST_X, SLOT_Y);
        if (recipe.getCatalyst() != null && !recipe.getCatalyst().isEmpty()) {
            catalyst.addIngredients(recipe.getCatalyst());
            catalyst.addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                    recipe.isCatalystConsumed() ? "jei.lyycore.catalyst.consumed" : "jei.lyycore.catalyst.reusable")));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y).addItemStack(recipe.getResult());
    }

    @Override
    public void draw(ImaginaryAlloyingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        ForgeGui.slot(g, INPUT_X, INPUT_A_Y, false);
        ForgeGui.slot(g, INPUT_X, INPUT_B_Y, false);
        ForgeGui.slot(g, CATALYST_X, SLOT_Y, true);
        ForgeGui.slot(g, OUTPUT_X, SLOT_Y, false);
        ForgeGui.arrow(g, 98, 40, ForgeGui.ACCENT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("tooltip.lyycore.forge.input"), 8, 10, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("tooltip.lyycore.forge.catalyst"), 52, 10, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("tooltip.lyycore.forge.output"), 126, 10, ForgeGui.TEXT, false);
        g.fill(8, 72, getWidth() - 8, 73, ForgeGui.TRACK);
        // process_time is in seconds; mirror the machine's bounds, not vanilla furnace ticks.
        int seconds = Math.clamp(recipe.getProcessTime(), 1, Integer.MAX_VALUE / 20);
        g.drawString(font, Component.translatable("jei.lyycore.time", seconds), 8, 78, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("jei.lyycore.energy", Math.max(0, recipe.getEnergyCost())),
                8, 90, ForgeGui.TEXT, false);
    }
}
