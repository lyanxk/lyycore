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
    TREE("tree", "tree_core", "frame_grove", true, 0xFF92B979),
    OVERWORLD("overworld", "overworld_core", "frame_river", true, 0xFFC6AF7D),
    MINERAL("mineral", "mineral_core", "frame_mine", true, 0xFF8BBBD1),
    NETHER("nether", "nether_core", "frame_nether", false, 0xFFD18E7F);

    private final String id, coreId, model;
    private final boolean water;
    private final int color;
    ResourceFrameKind(String id, String coreId, String model, boolean water, int color) {
        this.id = id; this.coreId = coreId; this.model = model; this.water = water; this.color = color;
    }
    public String id() { return id; }
    public String blockId() { return id + "_gathering_frame"; }
    public String recipeId() { return id + "_gathering"; }
    public String coreId() { return coreId; }
    public String model() { return model; }
    public boolean supportsWater() { return water; }
    public int color() { return color; }
    public Block block() { return LyyBlocks.RESOURCE_FRAMES.get(this).get(); }
    public Item core() { return LyyItems.RESOURCE_CORES.get(this).get(); }
    public RecipeType<ResourceGatheringRecipe> recipeType() { return LyyRecipes.RESOURCE_GATHERING.get(this).get(); }
    public RecipeSerializer<ResourceGatheringRecipe> serializer() { return LyyRecipes.RESOURCE_GATHERING_SERIALIZERS.get(this).get(); }
}
