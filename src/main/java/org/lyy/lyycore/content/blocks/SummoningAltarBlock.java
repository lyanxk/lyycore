package org.lyy.lyycore.content.blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.SummoningAltarBlockEntity;
@net.neoforged.fml.common.EventBusSubscriber(modid = "lyycore")
public final class SummoningAltarBlock extends SummoningPedestalBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public SummoningAltarBlock() {
        super(Properties.of().randomTicks(), "summoning_altar"); registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override protected MapCodec<SummoningAltarBlock> codec() { return MapCodec.unit(this); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SummoningAltarBlockEntity(pos, state); }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SummoningAltarBlockEntity altar) altar.darknessTick();
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return super.useItemOn(stack, s, l, p, player, hand, hit);
        if (player instanceof ServerPlayer server && l.getBlockEntity(p) instanceof SummoningAltarBlockEntity altar) altar.summon(server);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return super.useWithoutItem(s, l, p, player, hit);
        if (player instanceof ServerPlayer server && l.getBlockEntity(p) instanceof SummoningAltarBlockEntity altar) altar.summon(server);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void sneakInteraction(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity().isShiftKeyDown() && e.getLevel().getBlockState(e.getPos()).getBlock() instanceof SummoningAltarBlock) {
            e.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
            e.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }
}
