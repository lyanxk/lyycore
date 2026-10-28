package org.lyy.lyycore.content.finality;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

import org.lyy.lyycore.content.blocks.MachineBlock;

public final class GenesisBlock extends MachineBlock {
    public GenesisBlock() {
        super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(
                stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<GenesisBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        HorizontalDirectionalBlock.FACING,
                        context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return org.lyy.lyycore.registry.LyyBlocks.OTHERWORLD_CHEST
                .get()
                .defaultBlockState()
                .setValue(
                        HorizontalDirectionalBlock.FACING,
                        state.getValue(HorizontalDirectionalBlock.FACING))
                .getShape(level, pos, context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GenesisBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type, FinalityContent.GENESIS_ENTITY.get(), GenesisBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof GenesisBlockEntity machine)
            server.openMenu(machine, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof GenesisBlockEntity machine)
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), machine.template.getStackInSlot(0));
        super.onRemove(state, level, pos, replacement, moving);
    }
}
