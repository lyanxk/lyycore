package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.AlloyCauldronBlockEntity;

public final class AlloyCauldronBlock extends BaseEntityBlock {
    public enum Liquid implements StringRepresentable { WATER, PINK, BLUE, SILVER, STRANGE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Liquid> LIQUID = EnumProperty.create("liquid", Liquid.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final MapCodec<AlloyCauldronBlock> CODEC = simpleCodec(AlloyCauldronBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 3, 15), box(1, 3, 1, 3, 10, 15),
            box(13, 3, 1, 15, 10, 15), box(3, 3, 1, 13, 10, 3), box(3, 3, 13, 13, 16, 16));
    private static final java.util.Map<Direction, VoxelShape> SHAPES = rotatedShapes();
    private static java.util.Map<Direction, VoxelShape> rotatedShapes() {
        var shapes = new java.util.EnumMap<Direction, VoxelShape>(Direction.class);
        var shape = SHAPE;
        for (var direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            shapes.put(direction, shape);
            VoxelShape[] next = {Shapes.empty()};
            shape.forAllBoxes((x0,y0,z0,x1,y1,z1) -> next[0] = Shapes.or(next[0], Shapes.box(1-z1,y0,x0,1-z0,y1,x1)));
            shape = next[0];
        }
        return shapes;
    }
    public AlloyCauldronBlock() { this(Properties.of().strength(4).sound(SoundType.METAL).noOcclusion()); }
    private AlloyCauldronBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(LIQUID, Liquid.WATER).setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<AlloyCauldronBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIQUID, FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES.get(state.getValue(FACING)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlloyCauldronBlockEntity(pos, state); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof AlloyCauldronBlockEntity pot) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) pot.clear(true);
                else if (stack.is(Items.GLASS_BOTTLE)) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, pot.bottle()));
                } else {
                    if (!pot.add(stack.copyWithCount(1))) return ItemInteractionResult.CONSUME;
                    if (stack.is(org.lyy.lyycore.registry.LyyItems.INCOMPLETE_POTION))
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    else if (!player.getAbilities().instabuild) stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof AlloyCauldronBlockEntity pot) pot.clear(true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity item && !item.getItem().isEmpty()
                && level.getBlockEntity(pos) instanceof AlloyCauldronBlockEntity pot) {
            if (pot.add(item.getItem())) item.discard();
        }
    }
}
