package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.blocks.ResourceGatheringFrameBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.lyy.lyycore.content.menu.ResourceGatheringMenu;
import org.lyy.lyycore.content.recipes.ResourceGatheringRecipe;
import org.lyy.lyycore.registry.LyyBlockEntities;

import java.util.Comparator;
import java.util.List;

public final class ResourceGatheringFrameBlockEntity extends CondensingFrameBlockEntity {
    private ResourceLocation selectedRecipe;
    private final ContainerData gatheringData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 4) return ResourceGatheringFrameBlockEntity.super.getData().get(index);
            int selection = getSelectedIndex();
            return index == 4 ? selection & 0xFFFF : selection >>> 16;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ResourceGatheringFrameBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.RESOURCE_GATHERING_FRAME.get(), pos, state);
    }

    public ResourceFrameKind kind() {
        return ((ResourceGatheringFrameBlock) getBlockState().getBlock()).kind();
    }

    public static List<RecipeHolder<ResourceGatheringRecipe>> recipes(Level level, ResourceFrameKind kind) {
        return level.getRecipeManager().getAllRecipesFor(kind.recipeType()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString())).toList();
    }

    public List<RecipeHolder<ResourceGatheringRecipe>> recipes() {
        return recipes(level, kind());
    }

    public int getSelectedIndex() {
        var recipes = recipes();
        for (int i = 0; i < recipes.size(); i++) if (recipes.get(i).id().equals(selectedRecipe)) return i;
        return -1;
    }

    public boolean selectRecipe(int index) {
        var recipes = recipes();
        if (index < 0 || index >= recipes.size()) return false;
        selectedRecipe = recipes.get(index).id();
        setChanged();
        return true;
    }

    @Override
    protected void produce(BlockState state) {
        if (selectedRecipe == null) return;
        var holder = level.getRecipeManager().byKey(selectedRecipe).orElse(null);
        if (holder == null || !(holder.value() instanceof ResourceGatheringRecipe recipe) || recipe.kind() != kind())
            return;
        produceItem(recipe.result(), recipe.energyCost(state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)));
    }

    @Override
    public ContainerData getData() {
        return gatheringData;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ResourceGatheringMenu(id, inventory, this, getOutput(), gatheringData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (selectedRecipe != null) tag.putString("SelectedRecipe", selectedRecipe.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedRecipe = ResourceLocation.tryParse(tag.getString("SelectedRecipe"));
    }
}
