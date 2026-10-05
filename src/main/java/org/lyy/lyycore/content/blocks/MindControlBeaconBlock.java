package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.MindControlBeaconBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class MindControlBeaconBlock extends SquareMachineBlock {
    public static final MapCodec<MindControlBeaconBlock> CODEC = simpleCodec(MindControlBeaconBlock::new);
    public MindControlBeaconBlock() { this(Properties.of().strength(5).sound(SoundType.AMETHYST)); }
    private MindControlBeaconBlock(Properties properties) { super(properties, "mind_control_beacon", 6); }
    @Override protected MapCodec<MindControlBeaconBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == CENTER ? new MindControlBeaconBlockEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != CENTER ? null : createTickerHelper(type, LyyBlockEntities.MIND_CONTROL_BEACON.get(), MindControlBeaconBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var controller = center(pos, state);
        if (player instanceof ServerPlayer server && level.getBlockEntity(controller) instanceof MindControlBeaconBlockEntity beacon && beacon.activate(server))
            server.openMenu(beacon, controller);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(center(pos, state)) instanceof MindControlBeaconBlockEntity beacon)
            beacon.release();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
