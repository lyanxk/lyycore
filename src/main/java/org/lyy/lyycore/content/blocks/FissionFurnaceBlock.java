package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.FissionFurnaceBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class FissionFurnaceBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public FissionFurnaceBlock() {
        super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }
    @Override protected MapCodec<FissionFurnaceBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ACTIVE, FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new FissionFurnaceBlockEntity(pos, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.FISSION_FURNACE.get(), FissionFurnaceBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && l.getBlockEntity(p) instanceof FissionFurnaceBlockEntity furnace) server.openMenu(furnace, p);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s, Level l, BlockPos p, BlockState replacement, boolean moving) {
        if (!s.is(replacement.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof FissionFurnaceBlockEntity furnace) {
            for (int i = 0; i < 3; i++) Containers.dropItemStack(l, p.getX(), p.getY(), p.getZ(), furnace.items.getStackInSlot(i));
            furnace.awardExperience();
        }
        super.onRemove(s, l, p, replacement, moving);
    }
}
