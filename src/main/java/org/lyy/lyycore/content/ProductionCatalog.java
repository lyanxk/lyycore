package org.lyy.lyycore.content;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.lyy.lyycore.registry.LyyRecipes;

/** Union of factory outputs by item and components. Cached stacks are templates; copy before use. */
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

            var unique = ItemStackLinkedSet.createTypeAndComponentsSet();
            return byRecipe.values().stream().filter(unique::add)
                    .sorted(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())).toList();
        });
    }
    public static void clear() { CACHE.clear(); }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent event) { if (event.getPlayer() == null) clear(); }
}
