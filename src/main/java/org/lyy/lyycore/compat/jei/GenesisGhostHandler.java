package org.lyy.lyycore.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import org.lyy.lyycore.client.finality.GenesisScreen;

import java.util.List;

public final class GenesisGhostHandler implements IGhostIngredientHandler<GenesisScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(
            GenesisScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        if (!(ingredient.getIngredient() instanceof ItemStack)) return List.of();
        return List.of(
                new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return screen.ghostArea();
                    }

                    @Override
                    public void accept(I item) {
                        if (item instanceof ItemStack stack) screen.acceptGhost(stack);
                    }
                });
    }

    @Override
    public void onComplete() {}
}
