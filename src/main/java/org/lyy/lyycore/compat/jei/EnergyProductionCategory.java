package org.lyy.lyycore.compat.jei;

import java.util.List;
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
import org.lyy.lyycore.registry.LyyBlocks;

/** Views of tag-driven conversion and the shared production catalog, without registering synthetic recipes. */
public final class EnergyProductionCategory implements IRecipeCategory<EnergyProductionCategory.View> {
    public record View(ItemStack output, List<ItemStack> materials) {
        public View { output = output.copy(); materials = materials.stream().map(ItemStack::copy).toList(); }
    }
    public static final RecipeType<View> EROSION = RecipeType.create("lyycore", "erosion", View.class);
    public static final RecipeType<View> OTHERWORLD = RecipeType.create("lyycore", "otherworld", View.class);
    private final boolean erosion;
    private final IDrawable icon;
    public EnergyProductionCategory(IGuiHelper helper, boolean erosion) {
        this.erosion = erosion;
        icon = helper.createDrawableItemStack(new ItemStack(erosion ? LyyBlocks.EROSION_FACTORY.get() : LyyBlocks.OTHERWORLD_CHEST.get()));
    }
    @Override public RecipeType<View> getRecipeType() { return erosion ? EROSION : OTHERWORLD; }
    @Override public Component getTitle() { return Component.translatable("block.lyycore." + (erosion ? "erosion_factory" : "otherworld_chest")); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 80; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, View view, IFocusGroup focus) {
        if (erosion) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 16, 24).addItemStack(view.output);
            builder.addSlot(RecipeIngredientRole.INPUT, 66, 24).addItemStacks(view.materials);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 137, 24).addItemStack(view.output);
    }
    @Override public void draw(View view, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        if (erosion) { ForgeGui.slot(g, 16, 24, true); ForgeGui.slot(g, 66, 24, false); }
        ForgeGui.slot(g, 137, 24, false); ForgeGui.arrow(g, 103, 26, ForgeGui.ACCENT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable(erosion ? "gui.lyycore.erosion.template" : "jei.lyycore.otherworld.production"), 10, 8, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable(erosion ? "jei.lyycore.erosion.cost" : "jei.lyycore.otherworld.cost"), 10, 56, ForgeGui.TEXT, false);
    }
}
