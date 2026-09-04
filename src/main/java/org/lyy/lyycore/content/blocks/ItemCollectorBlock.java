package org.lyy.lyycore.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.ItemCollectorBlockEntity;

import javax.annotation.Nullable;

public class ItemCollectorBlock extends BaseEntityBlock {
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 1);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final MapCodec<ItemCollectorBlock> CODEC = simpleCodec(p -> new ItemCollectorBlock());
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public ItemCollectorBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f));
        registerDefaultState(stateDefinition.any().setValue(MODE, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemCollectorBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(MODE, 0).setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ItemCollectorBlockEntity collector) {
            ItemCollectorBlockEntity.Mode next = collector.toggleMode();
            level.setBlock(pos, state.setValue(MODE, next == ItemCollectorBlockEntity.Mode.SMALL ? 0 : 1), 3);
            int diameter = next.radius() * 2 + 1;
            player.displayClientMessage(Component.translatable("message.lyycore.collector_range",
                    diameter, diameter, diameter), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
                (lvl, pos, st, be) -> {
                    if (be instanceof ItemCollectorBlockEntity collector)
                        ItemCollectorBlockEntity.serverTick(lvl, pos, st, collector);
                };
    }
}
