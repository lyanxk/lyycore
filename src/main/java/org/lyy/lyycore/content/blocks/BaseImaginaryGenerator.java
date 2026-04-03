package org.lyy.lyycore.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.BaseImaginaryGeneratorBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

import javax.annotation.Nullable;

public class BaseImaginaryGenerator extends BaseEntityBlock {
    public static final MapCodec<BaseImaginaryGenerator> CODEC = simpleCodec(p -> new BaseImaginaryGenerator());
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public BaseImaginaryGenerator() {
        super(BlockBehaviour.Properties.of().noOcclusion().strength(3));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BaseImaginaryGeneratorBlockEntity be) {
            BaseImaginaryGeneratorBlockEntity.getPositions(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BaseImaginaryGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.IGB.get(),
                BaseImaginaryGeneratorBlockEntity::serverTick);
    }
}
