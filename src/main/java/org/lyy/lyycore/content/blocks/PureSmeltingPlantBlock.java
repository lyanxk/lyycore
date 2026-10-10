package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.PureSmeltingPlantBlockEntity;
import org.lyy.lyycore.registry.*;

public final class PureSmeltingPlantBlock extends LargeStructureBlock {
    private final boolean active;
    public PureSmeltingPlantBlock(boolean active) {
        super(active ? "pure_smelting_plant" : "pure_smelting_plant_shell", FRONT);
        this.active = active;
    }
    public boolean active() { return active; }
    @Override protected MapCodec<PureSmeltingPlantBlock> codec() { return simpleCodec(p -> new PureSmeltingPlantBlock(active)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? new PureSmeltingPlantBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !active ? null : createTickerHelper(type, LyyBlockEntities.PURE_SMELTING_PLANT.get(), PureSmeltingPlantBlockEntity::serverTick);
    }
    @Override protected boolean sameStructure(BlockState replacement) { return replacement.getBlock() instanceof PureSmeltingPlantBlock; }
    @Override protected void dismantle(BlockEntity entity) { ((PureSmeltingPlantBlockEntity) entity).dropContents(); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) { interact(state, level, pos, player, ItemStack.EMPTY); return ItemInteractionResult.sidedSuccess(level.isClientSide); }
        if (!active && stack.is(LyyItems.UNEXTINGUISHED_DESIRE.get())) {
            if (!level.isClientSide) {
                var center = center(pos, state);
                var next = LyyBlocks.PURE_SMELTING_PLANT.get().defaultBlockState().setValue(FACING, state.getValue(FACING));
                for (int part = 0; part < 27; part++) level.setBlock(partPos(center, state.getValue(FACING), part), next.setValue(PART, part), UPDATE_ALL);
                if (!player.hasInfiniteMaterials()) stack.shrink(1);
                level.invalidateCapabilities(center);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (active && state.getValue(PART) == FRONT) {
            interact(state, level, pos, player, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        interact(state, level, pos, player, ItemStack.EMPTY);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private void interact(BlockState state, Level level, BlockPos pos, Player player, ItemStack held) {
        if (!active || !(player instanceof ServerPlayer server)) return;
        var center = center(pos, state);
        if (!(level.getBlockEntity(center) instanceof PureSmeltingPlantBlockEntity plant)) return;
        if (player.isShiftKeyDown()) server.openMenu(plant, center);
        else if (state.getValue(PART) == FRONT) plant.interact(server, held);
    }
}
