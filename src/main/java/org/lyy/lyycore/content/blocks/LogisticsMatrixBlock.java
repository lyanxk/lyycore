package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.blockEntities.LogisticsMatrixBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class LogisticsMatrixBlock extends SquareMachineBlock {
    public LogisticsMatrixBlock() { super(Properties.of().strength(5).sound(SoundType.METAL), "logistics_matrix", 4, 5); }
    @Override protected MapCodec<LogisticsMatrixBlock> codec() { return MapCodec.unit(this); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == centerPart() ? new LogisticsMatrixBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != centerPart() ? null
                : createTickerHelper(type, LyyBlockEntities.LOGISTICS_MATRIX.get(), LogisticsMatrixBlockEntity::serverTick);
    }
    @Override protected void dropContents(Level level, BlockPos pos, BlockEntity entity) {
        if (entity instanceof LogisticsMatrixBlockEntity matrix) matrix.release();
    }
}
