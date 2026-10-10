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

public final class ErosionFactoryBlock extends SquareMachineBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
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
