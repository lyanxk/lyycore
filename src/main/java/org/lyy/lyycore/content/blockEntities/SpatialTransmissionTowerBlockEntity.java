package org.lyy.lyycore.content.blockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.registry.LyyBlockEntities;
public final class SpatialTransmissionTowerBlockEntity extends TargetedEnergySourceBlockEntity {
    public static final int MAX_TARGETS = 20;
    public SpatialTransmissionTowerBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.SPATIAL_TRANSMISSION_TOWER.get(), pos, state, MAX_TARGETS, 10_000, 500_000);
    }
}
