package org.lyy.lyycore.content.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.lyy.lyycore.content.blockEntities.CondensingFrameBlockEntity;

public interface CondensingFrame {
    BlockEntityType<? extends CondensingFrameBlockEntity> frameBlockEntityType();

    static boolean isWaterlogged(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED);
    }
}
