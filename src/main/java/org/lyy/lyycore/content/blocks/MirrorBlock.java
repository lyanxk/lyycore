package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.equipment.AccessoryMenu;

/** The authored mirror occupies two cells; either half opens the player's own equipment. */
public final class MirrorBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape[][] SHAPES = shapes();
    private static VoxelShape[][] shapes() {
        var shapes = new VoxelShape[4][]; shapes[0] = SquareMachineShapes.load("mirror");
        for (int i = 1; i < 4; i++) shapes[i] = new VoxelShape[]{SquareMachineShapes.clockwise(shapes[i - 1][0]), SquareMachineShapes.clockwise(shapes[i - 1][1])};
        return shapes;
    }
    public MirrorBlock() {
        super(Properties.of().strength(5).explosionResistance(MachineBlock.BLAST_RESISTANCE).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<MirrorBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(HALF, FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var pos = context.getClickedPos();
        return pos.getY() < context.getLevel().getMaxBuildHeight() - 1 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
                ? defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()) : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (direction == (lower ? Direction.UP : Direction.DOWN) && (!neighbor.is(this) || neighbor.getValue(HALF) == state.getValue(HALF))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            var below = level.getBlockState(pos.below());
            if (below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER) level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 35);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[(state.getValue(FACING).get2DDataValue() + 2) % 4][state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1];
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var base = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (player instanceof ServerPlayer server) server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new AccessoryMenu(id, inventory, base), getName()), base);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
