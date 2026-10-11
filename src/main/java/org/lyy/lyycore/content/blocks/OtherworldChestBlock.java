package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.OtherworldChestBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class OtherworldChestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape[] SHAPES = rotatedShapes();
    private static VoxelShape[] rotatedShapes() {
        var shapes = new VoxelShape[4];
        shapes[0] = SquareMachineShapes.load("otherworld_chest")[0];
        for (int i = 1; i < 4; i++) shapes[i] = SquareMachineShapes.clockwise(shapes[i - 1]);
        return shapes;
    }
    public OtherworldChestBlock() {
        super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }
    @Override protected MapCodec<OtherworldChestBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, OPEN); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[(state.getValue(FACING).get2DDataValue() + 2) % 4];
    }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new OtherworldChestBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.OTHERWORLD_CHEST.get(), OtherworldChestBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof OtherworldChestBlockEntity chest) server.openMenu(chest, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
