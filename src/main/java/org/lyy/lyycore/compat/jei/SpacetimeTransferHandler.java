package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lyy.lyycore.content.menu.SpacetimeFactoryMenu;
import org.lyy.lyycore.network.SpacetimeTransferNetwork;
import org.lyy.lyycore.registry.LyyMenus;

import java.util.Locale;
import java.util.Optional;

/** Each category shares one transaction instead of sending a page change ahead of its items. */
public final class SpacetimeTransferHandler<R extends Recipe<?>> implements IRecipeTransferHandler<SpacetimeFactoryMenu, R> {
    private final IRecipeTransferHandlerHelper helper;
    private final RecipeType<R> type;
    public SpacetimeTransferHandler(IRecipeTransferHandlerHelper helper, RecipeType<R> type) { this.helper = helper; this.type = type; }
    @Override public Class<SpacetimeFactoryMenu> getContainerClass() { return SpacetimeFactoryMenu.class; }
    @Override public Optional<MenuType<SpacetimeFactoryMenu>> getMenuType() { return Optional.of(LyyMenus.SPACETIME_FACTORY.get()); }
    @Override public RecipeType<R> getRecipeType() { return type; }
    @Override public @Nullable IRecipeTransferError transferRecipe(SpacetimeFactoryMenu menu, R recipe, IRecipeSlotsView slots,
                                                                  Player player, boolean maximum, boolean doTransfer) {
        if (!menu.stillValid(player) || player.isSpectator()) return error(SpacetimeTransferNetwork.Error.INVALID);
        var result = SpacetimeTransferNetwork.plan(menu, recipe, maximum);
        if (result.error() != null) return error(result.error());
        if (doTransfer) {
            var holder = player.level().getRecipeManager().getRecipes().stream().filter(entry -> entry.value() == recipe).findFirst().orElse(null);
            if (holder == null) return error(SpacetimeTransferNetwork.Error.INVALID);
            PacketDistributor.sendToServer(new SpacetimeTransferNetwork.Request(menu.containerId, holder.id(), maximum));
        }
        return null;
    }
    private IRecipeTransferError error(SpacetimeTransferNetwork.Error error) {
        return helper.createUserErrorWithTooltip(Component.translatable("jei.lyycore.spacetime.transfer." + error.name().toLowerCase(Locale.ROOT)));
    }
}
