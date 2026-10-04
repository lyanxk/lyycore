package org.lyy.lyycore.checks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.blockEntities.AbstractImaginaryCraftingBlockEntity;
import org.lyy.lyycore.content.recipes.*;
import org.lyy.lyycore.registry.*;
import java.util.*;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class CraftingCacheRegressions {
    private static final class Table extends AbstractImaginaryCraftingBlockEntity {
        int queries;
        Table(GameTestHelper test) {
            super(LyyBlockEntities.IMAGINARY_CRAFTING_TABLE.get(), test.absolutePos(new BlockPos(2, 1, 2)),
                    LyyBlocks.IMAGINARY_CRAFTING_TABLE.get().defaultBlockState());
            setLevel(test.getLevel());
        }
        @Override protected RecipeHolder<? extends OctagonalRecipe> findRecipe(RecipeInput input) {
            queries++;
            return level.getRecipeManager().getRecipeFor(LyyRecipes.IMAGINARY_CRAFTING.get(), input, level).orElse(null);
        }
        void fill(ImaginaryCraftingRecipe recipe) {
            for (int i = 0; i < 9; i++) {
                var ingredient = recipe.ingredients().get(i);
                items().setStackInSlot(i, ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0].copyWithCount(1));
            }
        }
        void tick(int count) {
            for (int i = 0; i < count; i++) serverTick(level, getBlockPos(), getBlockState(), this);
        }
    }
    @GameTest(template = "empty")
    public static void cachesMatchesAndMissesUntilInventoryChanges(GameTestHelper test) {
        var table = new Table(test);
        table.items().setStackInSlot(0, new ItemStack(Items.BEDROCK));
        table.tick(40);
        test.assertTrue(table.queries == 1 && table.data().get(0) == 0, "Unchanged invalid input repeatedly searched recipes");
        var recipe = test.getLevel().getRecipeManager().getAllRecipesFor(LyyRecipes.IMAGINARY_CRAFTING.get()).getFirst().value();
        table.fill(recipe); table.tick(30);
        test.assertTrue(table.queries == 2 && table.data().get(0) == 30, "Active recipe did not cache or input failed to invalidate");
        var saved = table.saveWithoutMetadata(test.getLevel().registryAccess());
        table.loadWithComponents(saved, test.getLevel().registryAccess()); table.tick(1);
        test.assertTrue(table.queries == 3 && table.data().get(0) == 31, "Loading retained a stale cache or lost progress");
        table.tick(69);
        test.assertTrue(ItemStack.matches(table.items().getStackInSlot(0), recipe.result()), "Cached recipe did not complete correctly");
        test.succeed();
    }
    @GameTest(template = "empty", batch = "crafting_reload", timeoutTicks = 400)
    public static void reloadRefreshesCachedMissAndChangedRecipe(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var holder = manager.getAllRecipesFor(LyyRecipes.IMAGINARY_CRAFTING.get()).getFirst();
        var recipe = holder.value();
        var without = original.stream().filter(entry -> !entry.id().equals(holder.id())).toList();
        try {
            manager.replaceRecipes(without);
            var idle = new Table(test); idle.fill(recipe); idle.tick(1);
            test.assertTrue(idle.data().get(0) == 0, "Missing-recipe fixture unexpectedly matched");
            manager.replaceRecipes(original);
            var unchanged = new Table(test); unchanged.fill(recipe); unchanged.tick(7);
            var changedRecipe = new ImaginaryCraftingRecipe(recipe.ingredients(), new ItemStack(Items.BEDROCK));
            var altered = new ArrayList<RecipeHolder<?>>(without);
            altered.add(new RecipeHolder<>(holder.id(), changedRecipe));
            manager.replaceRecipes(altered);
            var changed = new Table(test); changed.fill(recipe); changed.tick(7);
            level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds()).join();
            idle.tick(1); unchanged.tick(1); changed.tick(1);
            test.assertTrue(idle.queries == 2 && idle.data().get(0) == 1, "Reload did not invalidate cached miss");
            test.assertTrue(unchanged.data().get(0) == 8, "Unchanged recipe lost progress on reload");
            test.assertTrue(changed.data().get(0) == 1, "Changed output inherited stale progress");
            changed.tick(99);
            test.assertTrue(ItemStack.matches(changed.items().getStackInSlot(0), recipe.result()), "Stale result survived reload");
        } finally { manager.replaceRecipes(original); }
        test.succeed();
    }
}
