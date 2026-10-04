package org.lyy.lyycore.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.AbstractImaginaryCraftingBlockEntity;

/** Shared 3 x 3 footprint; only the bottom center owns state, inventories and a ticker. */
public abstract class SquareMachineBlock extends BaseEntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 71);
    public static final int CENTER = 4;
    private final int height;
    private final VoxelShape[] shapes;
    protected SquareMachineBlock(Properties properties, String model, int height) {
        super(properties.noOcclusion().pushReaction(PushReaction.BLOCK));
        this.height = height;
        this.shapes = SquareMachineShapes.load(model);
        registerDefaultState(stateDefinition.any().setValue(PART, CENTER));
    }
    public int height() { return height; }
    public static BlockPos partPos(BlockPos center, int part) { return center.offset(part % 3 - 1, part / 9, part / 3 % 3 - 1); }
    public static BlockPos center(BlockPos pos, BlockState state) { return pos.subtract(partPos(BlockPos.ZERO, state.getValue(PART))); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        return part < shapes.length ? shapes[part] : Shapes.empty();
    }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (int part = 0; part < height * 9; part++) {
            var pos = partPos(context.getClickedPos(), part);
            if (!context.getLevel().hasChunkAt(pos) || context.getLevel().isOutsideBuildHeight(pos)
                    || !context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        }
        return defaultBlockState();
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) for (int part = 0; part < height * 9; part++) if (part != CENTER)
            level.setBlock(partPos(pos, part), state.setValue(PART, part), UPDATE_ALL);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var controller = center(pos, state);
        if (player instanceof ServerPlayer server && level.getBlockEntity(controller) instanceof MenuProvider menu)
            server.openMenu(menu, controller);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) {
            var controller = center(pos, state);
            var entity = level.getBlockEntity(controller);
            if (entity != null) {
                if (entity instanceof AbstractImaginaryCraftingBlockEntity table) for (int i = 0; i < 9; i++)
                    Containers.dropItemStack(level, controller.getX(), controller.getY(), controller.getZ(), table.items().getStackInSlot(i));
                level.removeBlockEntity(controller);
                for (int part = 0; part < height * 9; part++) {
                    var other = partPos(controller, part);
                    var existing = level.getBlockState(other);
                    if (!other.equals(pos) && existing.is(this) && center(other, existing).equals(controller))
                        level.setBlock(other, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                    level.invalidateCapabilities(other);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
