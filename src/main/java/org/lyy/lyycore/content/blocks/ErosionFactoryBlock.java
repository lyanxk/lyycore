package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;
import net.minecraft.world.phys.shapes.*;

public final class ErosionFactoryBlock extends SquareMachineBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape[][] SHAPES = rotatedShapes();
    private static VoxelShape[][] rotatedShapes() {
        var shapes = new VoxelShape[4][27];
        shapes[0] = SquareMachineShapes.load("erosion_factory");
        for (int turn = 1; turn < 4; turn++) for (int part = 0; part < 27; part++) {
            int x = part % 3 - 1, z = part / 3 % 3 - 1;
            // PART identifies a world cell, so rotate both the cell and its local shape.
            int rotated = 1 - z + (x + 1) * 3 + part / 9 * 9;
            shapes[turn][rotated] = SquareMachineShapes.clockwise(shapes[turn - 1][part]);
        }
        return shapes;
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        int turn = (state.getValue(FACING).get2DDataValue() + 2) % 4;
        return part < 27 ? SHAPES[turn][part] : Shapes.empty();
    }
    public ErosionFactoryBlock() {
        super(Properties.of().strength(6).sound(SoundType.METAL), "erosion_factory", 3);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<ErosionFactoryBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == CENTER ? new ErosionFactoryBlockEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != CENTER ? null : createTickerHelper(type, LyyBlockEntities.EROSION_FACTORY.get(), ErosionFactoryBlockEntity::serverTick);
    }
    @Override protected void dropContents(Level level, BlockPos pos, BlockEntity entity) {
        if (entity instanceof ErosionFactoryBlockEntity factory) for (int slot = 0; slot < factory.items.getSlots(); slot++)
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), factory.items.getStackInSlot(slot));
    }
}
