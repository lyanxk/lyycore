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
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.lyy.lyycore.client.screen.ForgeGui;
import org.lyy.lyycore.registry.LyyBlocks;
public final class FissionCategory implements IRecipeCategory<SmeltingRecipe> {
    public static final RecipeType<SmeltingRecipe> TYPE = RecipeType.create("lyycore", "fission", SmeltingRecipe.class);
    private final IDrawable icon;
    public FissionCategory(IGuiHelper helper) { icon = helper.createDrawableItemStack(new ItemStack(LyyBlocks.FISSION_FURNACE.get())); }
    @Override public RecipeType<SmeltingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return LyyBlocks.FISSION_FURNACE.get().getName(); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 64; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, SmeltingRecipe recipe, IFocusGroup focus) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 12).addIngredients(recipe.getIngredients().getFirst());
        var result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        int total = result.getCount()*2, limit = Math.min(64, result.getMaxStackSize());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 12).addItemStack(result.copyWithCount(Math.min(limit, total)));
        if (total > limit) builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 12).addItemStack(result.copyWithCount(total-limit));
    }
    @Override public void draw(SmeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
        ForgeGui.panel(g, 0, 0, getWidth(), getHeight()); ForgeGui.slot(g, 12, 12, false);
        ForgeGui.slot(g, 102, 12, true); ForgeGui.slot(g, 126, 12, true); ForgeGui.arrow(g, 62, 15, ForgeGui.ACCENT);
        g.drawString(Minecraft.getInstance().font, Component.translatable("jei.lyycore.fission.cost"), 10, 44, ForgeGui.TEXT, false);
    }
}
