package org.lyy.lyycore.registry;

import net.neoforged.bus.api.IEventBus;

public class LyyRegistries {
    public static void registerAll(IEventBus bus) {
        LyyBlocks.BLOCKS.register(bus);
        LyyItems.ITEMS.register(bus);
        LyyBlockEntities.BLOCK_ENTITIES.register(bus);
        LyyMenus.MENUS.register(bus);
        LyyRecipes.RECIPE_TYPES.register(bus);
        LyyRecipes.RECIPE_SERIALIZERS.register(bus);
        LyyCreativeTab.TABS.register(bus);
    }
}
