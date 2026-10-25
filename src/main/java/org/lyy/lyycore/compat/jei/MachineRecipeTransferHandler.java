package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.network.MachineRecipeTransferNetwork;

import java.util.Optional;

/** Shared preview and server transaction for all machine recipe transfers. */
public final class MachineRecipeTransferHandler<C extends AbstractContainerMenu, R extends Recipe<?>> implements IRecipeTransferHandler<C, R> {
    private final IRecipeTransferHandlerHelper helper;
    private final Class<C> menuClass;
    private final MenuType<C> menuType;
    private final RecipeType<R> type;
    public MachineRecipeTransferHandler(IRecipeTransferHandlerHelper helper, Class<C> menuClass, MenuType<C> menuType, RecipeType<R> type) {
        this.helper = helper; this.menuClass = menuClass; this.menuType = menuType; this.type = type;
    }
    @Override public Class<C> getContainerClass() { return menuClass; }
    @Override public Optional<MenuType<C>> getMenuType() { return Optional.of(menuType); }
    @Override public RecipeType<R> getRecipeType() { return type; }
    @Override public @Nullable IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots,
                                                                  Player player, boolean maximum, boolean doTransfer) {
        if (!menu.stillValid(player) || player.isSpectator()) return error(MachineRecipeTransferNetwork.Error.INVALID);
        var result = MachineRecipeTransferNetwork.plan(menu, recipe, maximum);
        if (result.error() == MachineRecipeTransferNetwork.Error.MISSING && !result.missingSlots().isEmpty()) {
            // Categories retain empty INPUT views, so these indexes match their machine input slots.
            var inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            return helper.createUserErrorForMissingSlots(
                    result.error().message(),
                    result.missingSlots().stream().map(inputs::get).toList());
        }
        if (result.error() != null) return error(result.error());
        if (doTransfer) {
            var holder = player.level().getRecipeManager().getRecipes().stream().filter(entry -> entry.value() == recipe).findFirst().orElse(null);
            if (holder == null) return error(MachineRecipeTransferNetwork.Error.INVALID);
            PacketDistributor.sendToServer(new MachineRecipeTransferNetwork.Request(menu.containerId, holder.id(), maximum));
        }
        return null;
    }
    private IRecipeTransferError error(MachineRecipeTransferNetwork.Error error) {
        return helper.createUserErrorWithTooltip(error.message());
    }
}
