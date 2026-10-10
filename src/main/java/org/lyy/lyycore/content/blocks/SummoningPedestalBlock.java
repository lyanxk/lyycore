package org.lyy.lyycore.content.blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.SummoningPedestalBlockEntity;
public class SummoningPedestalBlock extends BaseEntityBlock {
    private final net.minecraft.world.phys.shapes.VoxelShape shape;
    public SummoningPedestalBlock() { this(Properties.of(), "summoning_pedestal"); }
    protected SummoningPedestalBlock(Properties p, String model) {
        super(p.strength(5).sound(SoundType.METAL).noOcclusion()); shape = SquareMachineShapes.load(model)[0];
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return shape; }
    @Override protected MapCodec<? extends SummoningPedestalBlock> codec() { return MapCodec.unit(this); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SummoningPedestalBlockEntity(pos, state); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Empty hands also enter this hook; allow useWithoutItem to retrieve the offering.
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof SummoningPedestalBlockEntity pedestal) {
            if (!level.isClientSide && pedestal.items.getStackInSlot(0).isEmpty()) {
                pedestal.items.insertItem(0, stack.copyWithCount(1), false); stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SummoningPedestalBlockEntity pedestal) {
            var item = pedestal.items.extractItem(0, 1, false);
            if (!player.getInventory().add(item)) player.drop(item, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof SummoningPedestalBlockEntity pedestal)
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pedestal.items.getStackInSlot(0));
        super.onRemove(state, level, pos, replacement, moving);
    }
}
