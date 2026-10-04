package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.content.blockEntities.SpatialTransmissionTowerBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class SpatialTransmissionTowerBlock extends SquareMachineBlock {
    public static final MapCodec<SpatialTransmissionTowerBlock> CODEC = simpleCodec(SpatialTransmissionTowerBlock::new);
    public SpatialTransmissionTowerBlock() { this(Properties.of().strength(5).sound(SoundType.METAL)); }
    private SpatialTransmissionTowerBlock(Properties properties) { super(properties, "spatial_transmission_tower", 8); }
    @Override protected MapCodec<SpatialTransmissionTowerBlock> codec() { return CODEC; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack device = new ItemStack(LyyItems.COORDINATE_DEVICE.get());
            if (!player.getInventory().add(device)) player.drop(device, false);
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? new SpatialTransmissionTowerBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != CENTER ? null : createTickerHelper(type,
                LyyBlockEntities.SPATIAL_TRANSMISSION_TOWER.get(), SpatialTransmissionTowerBlockEntity::serverTick);
    }
}
