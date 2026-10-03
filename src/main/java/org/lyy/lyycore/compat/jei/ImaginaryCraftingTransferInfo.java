package org.lyy.lyycore.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;
import org.lyy.lyycore.registry.LyyMenus;

import java.util.List;
import java.util.Optional;

/** Let JEI handle item movement, but report the temporary crafting lock as a user-facing error. */
public final class ImaginaryCraftingTransferInfo
        implements IRecipeTransferInfo<ImaginaryCraftingMenu, ImaginaryCraftingRecipe> {
    private final IRecipeTransferHandlerHelper helper;

    public ImaginaryCraftingTransferInfo(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override public Class<ImaginaryCraftingMenu> getContainerClass() { return ImaginaryCraftingMenu.class; }
    @Override public Optional<MenuType<ImaginaryCraftingMenu>> getMenuType() { return Optional.of(LyyMenus.IMAGINARY_CRAFTING.get()); }
    @Override public RecipeType<ImaginaryCraftingRecipe> getRecipeType() { return LyyJeiTypes.IMAGINARY_CRAFTING; }

    @Override
    public boolean canHandle(ImaginaryCraftingMenu menu, ImaginaryCraftingRecipe recipe) {
        return menu.progress() == 0;
    }

    @Override
    public IRecipeTransferError getHandlingError(ImaginaryCraftingMenu menu, ImaginaryCraftingRecipe recipe) {
        return helper.createUserErrorWithTooltip(Component.translatable("jei.lyycore.imaginary_crafting.busy"));
    }

    @Override
    public List<Slot> getRecipeSlots(ImaginaryCraftingMenu menu, ImaginaryCraftingRecipe recipe) {
        return menu.slots.subList(0, 9);
    }

    @Override
    public List<Slot> getInventorySlots(ImaginaryCraftingMenu menu, ImaginaryCraftingRecipe recipe) {
        return menu.slots.subList(9, 45);
    }
}
