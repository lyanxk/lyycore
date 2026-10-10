package org.lyy.lyycore.content.blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.blockEntities.TargetedEnergySourceBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;
public final class PhantomMatrixBlock extends EnergyTransmitterBlock {
    public PhantomMatrixBlock() { super(Properties.of().strength(5).sound(SoundType.METAL), "phantom_matrix", 4, 5); }
    @Override protected MapCodec<PhantomMatrixBlock> codec() { return MapCodec.unit(this); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == centerPart() ? create(pos, state) : null;
    }
    public static TargetedEnergySourceBlockEntity create(BlockPos pos, BlockState state) {
        return new TargetedEnergySourceBlockEntity(LyyBlockEntities.PHANTOM_MATRIX.get(), pos, state, 30, 1_000_000, 50_000_000);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != centerPart() ? null : createTickerHelper(type, LyyBlockEntities.PHANTOM_MATRIX.get(), TargetedEnergySourceBlockEntity::serverTick);
    }
}
