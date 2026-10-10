package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** One placed item occupies a 5x5 plane. Only the bottom center owns inventory. */
public class ImaginaryGateBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 4);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 4);
    public static final MapCodec<ImaginaryGateBlock> CODEC = simpleCodec(p -> new ImaginaryGateBlock());

    public ImaginaryGateBlock() {
        super(Properties.of().strength(4).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> 15).pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(COLUMN, 2).setValue(ROW, 0));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, COLUMN, ROW); }
    public static boolean isController(BlockState state) { return state.getValue(COLUMN) == 2 && state.getValue(ROW) == 0; }
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getClockWise(), 2 - state.getValue(COLUMN)).below(state.getValue(ROW));
    }
    public static BlockPos partPos(BlockPos controller, Direction facing, int column, int row) {
        return controller.relative(facing.getClockWise(), column - 2).above(row);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
            BlockPos part = partPos(context.getClickedPos(), facing, column, row);
            if (context.getLevel().isOutsideBuildHeight(part) || !context.getLevel().getWorldBorder().isWithinBounds(part)
                    || !context.getLevel().hasChunkAt(part) || !context.getLevel().getBlockState(part).canBeReplaced(context)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
            if (column == 2 && row == 0) continue;
            level.setBlock(partPos(pos, state.getValue(FACING), column, row),
                    state.setValue(COLUMN, column).setValue(ROW, row), UPDATE_ALL);
        }
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? box(0, 0, 5, 16, 16, 11) : box(5, 0, 0, 11, 16, 16);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isController(state) ? new ImaginaryGateBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !isController(state) ? null
                : createTickerHelper(type, LyyBlockEntities.IMAGINARY_GATE.get(), ImaginaryGateBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos controller = controllerPos(pos, state);
        if (player instanceof ServerPlayer server && level.getBlockEntity(controller) instanceof ImaginaryGateBlockEntity gate && !gate.interceptOpening(server))
            server.openMenu(gate, controller);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) {
            BlockPos controller = controllerPos(pos, state);
            if (level.getBlockEntity(controller) instanceof ImaginaryGateBlockEntity gate) {
                Containers.dropItemStack(level, controller.getX(), controller.getY(), controller.getZ(), gate.items().getStackInSlot(0));
                // Remove the owner first so recursive part removals cannot drop inventory twice.
                level.removeBlockEntity(controller);
                for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
                    BlockPos part = partPos(controller, state.getValue(FACING), column, row);
                    BlockState existing = level.getBlockState(part);
                    if (!part.equals(pos) && existing.is(this) && controllerPos(part, existing).equals(controller))
                        level.setBlock(part, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
