package org.lyy.lyycore.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.recipes.OctagonalRecipe;
import org.lyy.lyycore.registry.LyyMenus;

import java.util.List;
import java.util.Optional;

/** Let JEI handle item movement, but report the temporary crafting lock as a user-facing error. */
public final class ImaginaryCraftingTransferInfo<R extends OctagonalRecipe>
        implements IRecipeTransferInfo<ImaginaryCraftingMenu, R> {
    private final IRecipeTransferHandlerHelper helper;
    private final RecipeType<R> type;
    private final boolean condensing;

    public ImaginaryCraftingTransferInfo(IRecipeTransferHandlerHelper helper, RecipeType<R> type, boolean condensing) {
        this.helper = helper; this.type = type; this.condensing = condensing;
    }

    @Override public Class<ImaginaryCraftingMenu> getContainerClass() { return ImaginaryCraftingMenu.class; }
    @Override public Optional<MenuType<ImaginaryCraftingMenu>> getMenuType() { return Optional.of(LyyMenus.IMAGINARY_CRAFTING.get()); }
    @Override public RecipeType<R> getRecipeType() { return type; }

    @Override
    public boolean canHandle(ImaginaryCraftingMenu menu, R recipe) {
        return menu.progress() == 0 && (!condensing || menu.condensing());
    }

    @Override
    public IRecipeTransferError getHandlingError(ImaginaryCraftingMenu menu, R recipe) {
        return helper.createUserErrorWithTooltip(Component.translatable(condensing && !menu.condensing() ? "jei.lyycore.condensing.required" : "jei.lyycore.imaginary_crafting.busy"));
    }

    @Override
    public List<Slot> getRecipeSlots(ImaginaryCraftingMenu menu, R recipe) {
        return menu.slots.subList(0, 9);
    }

    @Override
    public List<Slot> getInventorySlots(ImaginaryCraftingMenu menu, R recipe) {
        return menu.slots.subList(9, 45);
    }
}
