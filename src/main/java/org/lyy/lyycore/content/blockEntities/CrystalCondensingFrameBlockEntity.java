package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.lyy.lyycore.content.menu.CrystalCondensingMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

public final class CrystalCondensingFrameBlockEntity extends CondensingFrameBlockEntity {
    private boolean productionPaused;
    private final ContainerData condensingData = new ContainerData() {
        @Override public int get(int index) {
            return index == 4 ? (productionPaused ? 1 : 0) : CrystalCondensingFrameBlockEntity.super.getData().get(index);
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 5; }
    };

    public CrystalCondensingFrameBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get(), pos, state);
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public ContainerData getData() { return condensingData; }
    public void setProductionPaused(boolean paused) {
        productionPaused = paused;
        setChanged();
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CrystalCondensingMenu(id, inventory, this, getOutput(), getData());
    }
    @Override protected void produce(BlockState state) {
        if (productionPaused) return;
        var recipes = level.getRecipeManager().getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get());
        // One built-in recipe; stable ordering also makes datapack overrides predictable.
        var recipe = recipes.stream().min(java.util.Comparator.comparing(r -> r.id().toString())).orElse(null);
        if (recipe != null) produceItem(recipe.value().result(), recipe.value().energyCost(state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ProductionPaused", productionPaused);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        productionPaused = tag.getBoolean("ProductionPaused");
    }
}
