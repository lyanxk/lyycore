package org.lyy.lyycore.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.lyy.lyycore.content.recipes.ResourceGatheringRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyRecipes;

public enum ResourceFrameKind {
    TREE("tree", "tree_gathering_frame", "tree_gathering", "frame_grove", true, 200, 0xFF92B979),
    OVERWORLD("overworld", "overworld_gathering_frame", "overworld_gathering", "frame_river", true, 200, 0xFFC6AF7D),
    MINERAL("mineral", "mineral_gathering_frame", "mineral_gathering", "frame_mine", true, 200, 0xFF8BBBD1),
    NETHER("nether", "nether_gathering_frame", "nether_gathering", "frame_nether", false, 200, 0xFFD18E7F),
    CONCRETE("concrete", "miniature_concrete_factory", "concrete_production", "frame_concrete", false, 100, 0xFFD4B2CC),
    GARDEN("garden", "miniature_garden", "garden_production", "frame_garden", false, 100, 0xFFE6ACBB),
    OCEAN("ocean", "miniature_ocean", "ocean_production", "frame_ocean", false, 100, 0xFF86BED8),
    MONUMENT("monument", "miniature_monument", "monument_production", "frame_monument", false, 100, 0xFF81BEB1);

    public static final java.util.List<ResourceFrameKind> FACTORY_KINDS = java.util.List.of(TREE, OVERWORLD, MINERAL, NETHER);

    public String factoryId() { return "miniature_" + id + "_factory"; }
    public Block factoryBlock() { return LyyBlocks.RESOURCE_FACTORIES.get(this).get(); }

    private final String id, blockId, recipeId, model;
    private final boolean water;
    private final int defaultCost, color;
    ResourceFrameKind(String id, String blockId, String recipeId, String model, boolean water, int defaultCost, int color) {
        this.id = id;
        this.blockId = blockId;
        this.recipeId = recipeId;
        this.model = model;
        this.water = water;
        this.defaultCost = defaultCost;
        this.color = color;
    }
    public String id() { return id; }
    public String blockId() { return blockId; }
    public String recipeId() { return recipeId; }
    public String coreId() { return id + "_core"; }
    public String model() { return model; }
    public boolean supportsWater() { return water; }
    public int defaultDryCost() { return defaultCost; }
    public int defaultWetCost() { return water ? 100 : defaultCost; }
    public int color() { return color; }
    public Block block() { return LyyBlocks.RESOURCE_FRAMES.get(this).get(); }
    public Item core() { return LyyItems.RESOURCE_CORES.get(this).get(); }
    public RecipeType<ResourceGatheringRecipe> recipeType() { return LyyRecipes.RESOURCE_GATHERING.get(this).get(); }
    public RecipeSerializer<ResourceGatheringRecipe> serializer() { return LyyRecipes.RESOURCE_GATHERING_SERIALIZERS.get(this).get(); }
}
