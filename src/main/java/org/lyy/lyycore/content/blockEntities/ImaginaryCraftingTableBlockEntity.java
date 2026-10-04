package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

public final class ImaginaryCraftingTableBlockEntity extends AbstractImaginaryCraftingBlockEntity {
    public ImaginaryCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IMAGINARY_CRAFTING_TABLE.get(), pos, state);
    }
    @Override protected RecipeHolder<ImaginaryCraftingRecipe> findRecipe(RecipeInput input) {
        return level.getRecipeManager().getRecipeFor(LyyRecipes.IMAGINARY_CRAFTING.get(), input, level).orElse(null);
    }
}
