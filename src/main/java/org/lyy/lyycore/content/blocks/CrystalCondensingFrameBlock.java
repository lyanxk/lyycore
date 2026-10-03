package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class CrystalCondensingFrameBlock extends CondensingFrameBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CrystalCondensingFrameBlock> CODEC = simpleCodec(properties -> new CrystalCondensingFrameBlock());

    @Override protected MapCodec<CrystalCondensingFrameBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }
    @Override public BlockEntityType<CrystalCondensingFrameBlockEntity> frameBlockEntityType() {
        return LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get();
    }
}
