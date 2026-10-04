package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.blockEntities.ImaginaryCondensingBeaconBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class ImaginaryCondensingBeaconBlock extends SquareMachineBlock {
    public static final MapCodec<ImaginaryCondensingBeaconBlock> CODEC = simpleCodec(ImaginaryCondensingBeaconBlock::new);
    public ImaginaryCondensingBeaconBlock() { this(Properties.of().strength(5).sound(SoundType.AMETHYST)); }
    private ImaginaryCondensingBeaconBlock(Properties properties) { super(properties, "imaginary_condensing_beacon", 4); }
    @Override protected MapCodec<ImaginaryCondensingBeaconBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? new ImaginaryCondensingBeaconBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != CENTER ? null : createTickerHelper(type,
                LyyBlockEntities.IMAGINARY_CONDENSING_BEACON.get(), ImaginaryCondensingBeaconBlockEntity::serverTick);
    }
}
