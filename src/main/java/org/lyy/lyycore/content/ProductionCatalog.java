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

/** Union of the nine factory outputs, cached until recipes reload. */
@EventBusSubscriber(modid = "lyycore")
public final class ProductionCatalog {
    private static final Map<RecipeManager, List<ItemStack>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    public static List<ItemStack> products(Level level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(), recipes -> {
            var items = new TreeMap<String, ItemStack>();
            for (var kind : ResourceFrameKind.values()) for (var recipe : recipes.getAllRecipesFor(kind.recipeType())) {
                var stack = recipe.value().result(); items.putIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.copyWithCount(1));
            }
            for (var recipe : recipes.getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get())) {
                var stack = recipe.value().getResultItem(level.registryAccess()); items.putIfAbsent(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.copyWithCount(1));
            }
            return List.copyOf(items.values());
        });
    }
    public static void clear() { CACHE.clear(); }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent event) { if (event.getPlayer() == null) clear(); }
}
