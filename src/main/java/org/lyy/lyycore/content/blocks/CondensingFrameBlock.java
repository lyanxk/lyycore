package org.lyy.lyycore.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.CondensingFrameBlockEntity;

public abstract class CondensingFrameBlock extends BaseEntityBlock implements CondensingFrame {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 1.5, 16), Block.box(0, 14.5, 0, 16, 16, 16),
            Block.box(0, 1.5, 0, 1.5, 14.5, 1.5), Block.box(14.5, 1.5, 0, 16, 14.5, 1.5),
            Block.box(0, 1.5, 14.5, 1.5, 14.5, 16), Block.box(14.5, 1.5, 14.5, 16, 14.5, 16),
            Block.box(5, 3, 5, 11, 13, 11));

    protected CondensingFrameBlock() {
        super(Properties.of().strength(3).sound(SoundType.AMETHYST).noOcclusion());
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (state.hasProperty(WATERLOGGED)) state = state.setValue(WATERLOGGED, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.hasProperty(WATERLOGGED)
                ? state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER)
                : state;
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return CondensingFrame.isWaterlogged(state) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (CondensingFrame.isWaterlogged(state))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.hasProperty(WATERLOGGED)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        boolean fill = stack.is(Items.WATER_BUCKET) && !CondensingFrame.isWaterlogged(state);
        boolean drain = stack.is(Items.BUCKET) && CondensingFrame.isWaterlogged(state);
        if (fill || drain) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(WATERLOGGED, fill), Block.UPDATE_ALL);
                if (fill) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(fill ? Items.BUCKET : Items.WATER_BUCKET)));
                level.playSound(null, pos, fill ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
                level.gameEvent(player, fill ? net.minecraft.world.level.gameevent.GameEvent.FLUID_PLACE : net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, pos);
            }
        } else openMenu(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        openMenu(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openMenu(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CondensingFrameBlockEntity be)
            serverPlayer.openMenu(be, pos);
    }

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
