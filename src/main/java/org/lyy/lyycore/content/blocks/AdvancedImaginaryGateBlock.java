package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.AdvancedImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** Uses the same placement, dismantling and offering rules as the original gate. */
public final class AdvancedImaginaryGateBlock extends ImaginaryGateBlock {
    public static final MapCodec<AdvancedImaginaryGateBlock> CODEC = simpleCodec(p -> new AdvancedImaginaryGateBlock());
    @Override protected MapCodec<? extends ImaginaryGateBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isController(state) ? new AdvancedImaginaryGateBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !isController(state) ? null
                : createTickerHelper(type, LyyBlockEntities.ADVANCED_IMAGINARY_GATE.get(), AdvancedImaginaryGateBlockEntity::serverTick);
    }
}
