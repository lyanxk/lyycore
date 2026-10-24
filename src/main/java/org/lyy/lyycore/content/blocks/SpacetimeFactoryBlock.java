package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class SpacetimeFactoryBlock extends MachineBlock {
    public static final MapCodec<SpacetimeFactoryBlock> CODEC = simpleCodec(p -> new SpacetimeFactoryBlock());
    private static final VoxelShape COLLISION = box(0, 0, 0, 16, 14, 16);
    public SpacetimeFactoryBlock() { super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion()); }
    @Override protected MapCodec<? extends MachineBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpacetimeFactoryBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.SPACETIME_FACTORY.get(), SpacetimeFactoryBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof SpacetimeFactoryBlockEntity factory) server.openMenu(factory, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof SpacetimeFactoryBlockEntity factory) factory.dropContents();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
