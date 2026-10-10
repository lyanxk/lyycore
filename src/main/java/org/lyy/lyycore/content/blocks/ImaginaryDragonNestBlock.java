package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class ImaginaryDragonNestBlock extends LargeStructureBlock {
    public ImaginaryDragonNestBlock() { super("imaginary_dragon_nest", CENTER); }
    @Override protected MapCodec<ImaginaryDragonNestBlock> codec() { return simpleCodec(p -> new ImaginaryDragonNestBlock()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? new ImaginaryDragonNestBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.IMAGINARY_DRAGON_NEST.get(), ImaginaryDragonNestBlockEntity::serverTick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(center(pos, state)) instanceof ImaginaryDragonNestBlockEntity nest) nest.bind(player);
    }
    @Override protected void dismantle(BlockEntity entity) { ((ImaginaryDragonNestBlockEntity) entity).release(); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        useWithoutItem(state, level, pos, player, hit); return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var center = center(pos, state);
        if (player instanceof ServerPlayer server && level.getBlockEntity(center) instanceof ImaginaryDragonNestBlockEntity nest) {
            if (player.isShiftKeyDown()) nest.cycle(server);
            else server.openMenu(nest, center);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
