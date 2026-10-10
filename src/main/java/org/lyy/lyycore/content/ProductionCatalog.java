package org.lyy.lyycore.content;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.lyy.lyycore.registry.LyyRecipes;

/** Union of factory outputs by item AND components, cached until recipes reload. */
@EventBusSubscriber(modid = "lyycore")
public final class ProductionCatalog {
    private static final Map<RecipeManager, List<ItemStack>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private ProductionCatalog() { }

    public static List<ItemStack> products(Level level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(), recipes -> {
            // Recipe order is otherwise unspecified. Both sides must assign the same menu indices.
            var byRecipe = new TreeMap<String, ItemStack>();
            for (var kind : ResourceFrameKind.values()) for (var recipe : recipes.getAllRecipesFor(kind.recipeType()))
                byRecipe.put(recipe.id().toString(), recipe.value().result().copyWithCount(1));
            for (var recipe : recipes.getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get()))
                byRecipe.put(recipe.id().toString(), recipe.value().getResultItem(level.registryAccess()).copyWithCount(1));

            var byItem = new TreeMap<String, List<ItemStack>>();
            for (var stack : byRecipe.values()) {
                if (stack.isEmpty()) continue;
                var variants = byItem.computeIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), ignored -> new ArrayList<>());
                if (variants.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack))) variants.add(stack);
            }
            var products = new ArrayList<ItemStack>();
            byItem.values().forEach(products::addAll);
            return new Products(products);
        });
    }
    /** A stable, read-only list identity for caches; callers never receive the cached mutable stacks. */
    private static final class Products extends AbstractList<ItemStack> implements RandomAccess {
        private final List<ItemStack> stacks;
        Products(List<ItemStack> stacks) { this.stacks = stacks.stream().map(ItemStack::copy).toList(); }
        @Override public ItemStack get(int index) { return stacks.get(index).copy(); }
        @Override public int size() { return stacks.size(); }
    }
    public static void clear() { CACHE.clear(); }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent event) { if (event.getPlayer() == null) clear(); }
}
