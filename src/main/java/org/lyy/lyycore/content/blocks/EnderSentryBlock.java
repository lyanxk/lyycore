package org.lyy.lyycore.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.EnderCompanions;

/** Looks like a dragon egg, but does not inherit its falling or teleport interactions. */
public final class EnderSentryBlock extends Block {
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);
    public EnderSentryBlock() { super(Properties.ofFullCopy(Blocks.DRAGON_EGG).noLootTable()); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) EnderCompanions.toggle(server, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
