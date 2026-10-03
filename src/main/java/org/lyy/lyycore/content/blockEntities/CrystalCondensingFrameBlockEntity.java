package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.blocks.CondensingFrame;
import org.lyy.lyycore.content.menu.CrystalCondensingMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

public final class CrystalCondensingFrameBlockEntity extends CondensingFrameBlockEntity {
    public CrystalCondensingFrameBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get(), pos, state);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.crystal_condensing_frame"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CrystalCondensingMenu(id, inventory, this, getOutput(), getData());
    }
    @Override protected void produce(BlockState state) {
        var recipes = level.getRecipeManager().getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get());
        // One built-in recipe; stable ordering also makes datapack overrides predictable.
        var recipe = recipes.stream().min(java.util.Comparator.comparing(r -> r.id().toString())).orElse(null);
        if (recipe != null) produceItem(recipe.value().result(), recipe.value().energyCost(CondensingFrame.isWaterlogged(state)), state);
    }
}
