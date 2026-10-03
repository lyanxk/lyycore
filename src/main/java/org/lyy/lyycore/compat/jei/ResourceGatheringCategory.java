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
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.menu.ForgeGui;
import org.lyy.lyycore.content.recipes.ResourceGatheringRecipe;

public final class ResourceGatheringCategory implements IRecipeCategory<ResourceGatheringRecipe> {
    private final ResourceFrameKind kind;
    private final IDrawable icon;
    public ResourceGatheringCategory(IGuiHelper helper, ResourceFrameKind kind) {
        this.kind = kind;
        icon = helper.createDrawableItemStack(new ItemStack(kind.block()));
    }
    @Override public RecipeType<ResourceGatheringRecipe> getRecipeType() { return LyyJeiTypes.RESOURCE_GATHERING.get(kind); }
    @Override public Component getTitle() { return Component.translatable("jei.lyycore." + kind.recipeId()); }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return kind.supportsWater() ? 94 : 80; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ResourceGatheringRecipe recipe, IFocusGroup focuses) {
        if (kind.supportsWater()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 18, 26).addItemStack(new ItemStack(Items.WATER_BUCKET))
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.lyycore.condensing.water")));
        } else builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 18, 26).addItemStack(new ItemStack(kind.block()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 26).addItemStack(recipe.result());
    }
    @Override public void draw(ResourceGatheringRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight());
        ForgeGui.frame(g, 104, 13, kind.supportsWater());
        ForgeGui.slot(g, 18, 26, false);
        ForgeGui.slot(g, 120, 26, true);
        ForgeGui.arrow(g, 64, 28, kind.color());
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable(kind.supportsWater() ? "jei.lyycore.condensing.optional_water" : "jei.lyycore.gathering.production"), 8, 8, ForgeGui.TEXT, false);
        g.fill(8, 55, 168, 56, ForgeGui.TRACK);
        g.drawString(font, Component.translatable(kind.supportsWater() ? "jei.lyycore.condensing.dry" : "jei.lyycore.gathering.cost", recipe.dryCost()), 8, 63, ForgeGui.TEXT, false);
        if (kind.supportsWater()) g.drawString(font, Component.translatable("jei.lyycore.condensing.wet", recipe.wetCost()), 8, 77, ForgeGui.TEXT, false);
    }
}
