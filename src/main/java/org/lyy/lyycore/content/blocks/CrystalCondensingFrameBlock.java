package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import org.lyy.lyycore.content.FrameProduction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class CrystalCondensingFrameBlock extends CondensingFrameBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CrystalCondensingFrameBlock() { this(FrameProduction.STANDARD); }

    public CrystalCondensingFrameBlock(FrameProduction production) {
        super(production);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean fill = stack.is(Items.WATER_BUCKET) && !state.getValue(WATERLOGGED);
        boolean drain = stack.is(Items.BUCKET) && state.getValue(WATERLOGGED);
        if (fill || drain) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(WATERLOGGED, fill), Block.UPDATE_ALL);
                if (fill) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(fill ? Items.BUCKET : Items.WATER_BUCKET)));
                level.playSound(null, pos, fill ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
                level.gameEvent(player, fill ? net.minecraft.world.level.gameevent.GameEvent.FLUID_PLACE : net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, pos);
            }
        } else openMenu(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected MapCodec<CrystalCondensingFrameBlock> codec() { return simpleCodec(properties -> new CrystalCondensingFrameBlock(production())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }
    @Override protected BlockEntityType<CrystalCondensingFrameBlockEntity> frameBlockEntityType() {
        return LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get();
    }
}
