package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.blockEntities.SpatialTransmissionTowerBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class SpatialTransmissionTowerBlock extends EnergyTransmitterBlock {
    public static final MapCodec<SpatialTransmissionTowerBlock> CODEC = simpleCodec(SpatialTransmissionTowerBlock::new);
    public SpatialTransmissionTowerBlock() { this(Properties.of().strength(5).sound(SoundType.METAL)); }
    private SpatialTransmissionTowerBlock(Properties properties) { super(properties, "spatial_transmission_tower", 8, 3); }
    @Override protected MapCodec<SpatialTransmissionTowerBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? new SpatialTransmissionTowerBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != CENTER ? null : createTickerHelper(type,
                LyyBlockEntities.SPATIAL_TRANSMISSION_TOWER.get(), SpatialTransmissionTowerBlockEntity::serverTick);
    }
}
