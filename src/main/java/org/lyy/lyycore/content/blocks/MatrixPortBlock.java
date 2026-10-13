package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.MatrixPortBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

@net.neoforged.fml.common.EventBusSubscriber(modid = "lyycore")
public final class MatrixPortBlock extends BaseEntityBlock {
    public enum Kind { INPUT, OUTPUT, COLLECTOR }
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final Kind kind;
    public MatrixPortBlock(Kind kind) {
        super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    public Kind kind() { return kind; }
    @net.neoforged.bus.api.SubscribeEvent
    public static void sneakInteraction(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MatrixPortBlock block
                && block.kind == Kind.COLLECTOR) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
    }
    @Override protected MapCodec<MatrixPortBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MatrixPortBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || kind == Kind.INPUT ? null : createTickerHelper(type, LyyBlockEntities.MATRIX_PORT.get(), MatrixPortBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MatrixPortBlockEntity port) {
            if (kind == Kind.COLLECTOR) {
                var look = player.getLookAngle();
                var facing = Direction.getNearest(look.x, look.y, look.z);
                if (player.isShiftKeyDown()) facing = facing.getOpposite();
                level.setBlock(pos, state.setValue(FACING, facing), UPDATE_ALL);
            }
            server.openMenu(port, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MatrixPortBlockEntity port)
            port.dropContents();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
