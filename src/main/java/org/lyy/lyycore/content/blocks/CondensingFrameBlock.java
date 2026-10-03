package org.lyy.lyycore.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.FrameProduction;
import org.lyy.lyycore.content.blockEntities.CondensingFrameBlockEntity;

public abstract class CondensingFrameBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 1.5, 16), Block.box(0, 14.5, 0, 16, 16, 16),
            Block.box(0, 1.5, 0, 1.5, 14.5, 1.5), Block.box(14.5, 1.5, 0, 16, 14.5, 1.5),
            Block.box(0, 1.5, 14.5, 1.5, 14.5, 16), Block.box(14.5, 1.5, 14.5, 16, 14.5, 16),
            Block.box(5, 3, 5, 11, 13, 11));

    private final FrameProduction production;

    protected CondensingFrameBlock(FrameProduction production) {
        super(Properties.of().strength(3).sound(SoundType.AMETHYST).noOcclusion());
        this.production = production;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public FrameProduction production() { return production; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Vanilla fluid spreading checks collision faces. Seal the frame while
        // keeping its detailed outline and real water state for rendering/buckets.
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        openMenu(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    protected final void openMenu(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CondensingFrameBlockEntity be)
            serverPlayer.openMenu(be, pos);
    }

    protected abstract BlockEntityType<? extends CondensingFrameBlockEntity> frameBlockEntityType();

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return frameBlockEntityType().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, frameBlockEntityType(), CondensingFrameBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CondensingFrameBlockEntity be) {
            while (be.getOutputCount() > 0)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getOutput().extractItem(0, 64, false));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
