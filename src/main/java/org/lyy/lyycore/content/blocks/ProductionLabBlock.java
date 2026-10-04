package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.ProductionLabBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** A 5 x 3 footprint with three occupied height layers; the base center owns the inventory. */
public final class ProductionLabBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 44);
    public static final int CONTROLLER = 7;
    public static final MapCodec<ProductionLabBlock> CODEC = simpleCodec(ProductionLabBlock::new);
    public ProductionLabBlock() { this(Properties.of().strength(4).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)); }
    private ProductionLabBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, CONTROLLER));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART); }
    public static BlockPos partPos(BlockPos controller, Direction facing, int part) {
        return controller.relative(facing.getCounterClockWise(), part % 5 - 2).relative(facing, part / 5 % 3 - 1).above(part / 15);
    }
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        BlockPos offset = partPos(BlockPos.ZERO, state.getValue(FACING), state.getValue(PART));
        return pos.subtract(offset);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (int part = 0; part < 45; part++) {
            BlockPos pos = partPos(context.getClickedPos(), facing, part);
            if (context.getLevel().isOutsideBuildHeight(pos) || !context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().hasChunkAt(pos) || !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        for (int part = 0; part < 45; part++) if (part != CONTROLLER)
            level.setBlock(partPos(pos, state.getValue(FACING), part), state.setValue(PART, part), UPDATE_ALL);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ProductionLabShapes.get(state.getValue(FACING), state.getValue(PART));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CONTROLLER ? new ProductionLabBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != CONTROLLER ? null
                : createTickerHelper(type, LyyBlockEntities.PRODUCTION_LAB.get(), ProductionLabBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos controller = controllerPos(pos, state);
        if (player instanceof ServerPlayer server && level.getBlockEntity(controller) instanceof ProductionLabBlockEntity lab)
            server.openMenu(lab, controller);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) {
            BlockPos controller = controllerPos(pos, state);
            if (level.getBlockEntity(controller) instanceof ProductionLabBlockEntity lab) {
                Containers.dropItemStack(level, controller.getX(), controller.getY(), controller.getZ(), lab.items().getStackInSlot(0));
                level.removeBlockEntity(controller);
                for (int part = 0; part < 45; part++) {
                    BlockPos other = partPos(controller, state.getValue(FACING), part);
                    BlockState existing = level.getBlockState(other);
                    if (!other.equals(pos) && existing.is(this) && controllerPos(other, existing).equals(controller))
                        level.setBlock(other, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
