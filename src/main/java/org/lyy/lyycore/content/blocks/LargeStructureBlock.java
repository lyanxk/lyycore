package org.lyy.lyycore.content.blocks;

import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;

/** Oriented 3×3×3 structures. Only the bottom center owns a block entity. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "lyycore")
public abstract class LargeStructureBlock extends BaseEntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 26);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final int CENTER = 4, FRONT = 1;
    @net.neoforged.bus.api.SubscribeEvent
    public static void sneakInteraction(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof LargeStructureBlock) {
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
            event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }
    private final VoxelShape[][] shapes = new VoxelShape[4][27];
    protected LargeStructureBlock(String model, int placementPart) {
        super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(PART, placementPart).setValue(FACING, Direction.NORTH));
        shapes[0] = SquareMachineShapes.load(model);
        for (int rotation = 1; rotation < 4; rotation++) for (int part = 0; part < 27; part++) {
            VoxelShape[] rotated = {Shapes.empty()};
            shapes[rotation - 1][part].forAllBoxes((x1, y1, z1, x2, y2, z2) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(1-z2, y1, x1, 1-z1, y2, x2)));
            shapes[rotation][part] = rotated[0];
        }
    }
    public static BlockPos partPos(BlockPos center, Direction facing, int part) {
        return center.relative(facing.getClockWise(), part % 3 - 1)
                .relative(facing.getOpposite(), part / 3 % 3 - 1).above(part / 9);
    }
    public static BlockPos center(BlockPos pos, BlockState state) {
        return pos.subtract(partPos(BlockPos.ZERO, state.getValue(FACING), state.getValue(PART)));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rotation = switch (state.getValue(FACING)) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        return shapes[rotation][state.getValue(PART)];
    }
    @Override protected VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.empty(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        var center = center(context.getClickedPos(), state);
        for (int part = 0; part < 27; part++) {
            var pos = partPos(center, state.getValue(FACING), part);
            if (!context.getLevel().hasChunkAt(pos) || context.getLevel().isOutsideBuildHeight(pos)
                    || !context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        }
        return state;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        var center = center(pos, state);
        for (int part = 0; part < 27; part++) {
            var other = partPos(center, state.getValue(FACING), part);
            if (!other.equals(pos)) level.setBlock(other, state.setValue(PART, part), UPDATE_ALL);
        }
    }
    protected void dismantle(BlockEntity entity) { }
    /** Shell activation preserves the footprint and deliberately bypasses dismantling. */
    protected boolean sameStructure(BlockState replacement) { return replacement.is(this); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!sameStructure(replacement) && !level.isClientSide) {
            var center = center(pos, state);
            var entity = level.getBlockEntity(center);
            if (entity != null) {
                dismantle(entity);
                level.removeBlockEntity(center);
                for (int part = 0; part < 27; part++) {
                    var other = partPos(center, state.getValue(FACING), part);
                    var existing = level.getBlockState(other);
                    if (!other.equals(pos) && existing.is(this) && center(other, existing).equals(center))
                        level.setBlock(other, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                    level.invalidateCapabilities(other);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
