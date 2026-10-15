package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.ReforgingTableBlockEntity;

public final class ReforgingTableBlock extends MachineBlock {
    private static final VoxelShape SHAPE = SquareMachineShapes.load("reforging_table")[0];
    public ReforgingTableBlock() { super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion()); }
    @Override protected MapCodec<ReforgingTableBlock> codec() { return MapCodec.unit(this); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ReforgingTableBlockEntity(pos, state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ReforgingTableBlockEntity table) server.openMenu(table, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ReforgingTableBlockEntity table && !level.isClientSide)
            for (int i = 0; i < 2; i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), table.items.getStackInSlot(i));
        super.onRemove(state, level, pos, next, moving);
    }
}
