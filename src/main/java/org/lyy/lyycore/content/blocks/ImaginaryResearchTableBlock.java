package org.lyy.lyycore.content.blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.lyy.lyycore.content.blockEntities.ResearchTableBlockEntity;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;
public final class ImaginaryResearchTableBlock extends BaseEntityBlock {
    public static final MapCodec<ImaginaryResearchTableBlock> CODEC = simpleCodec(properties -> new ImaginaryResearchTableBlock(properties));
    // Bounds exported from the static model; floating book and glyphs are not solid.
    private static final VoxelShape SHAPE = Shapes.or(Shapes.empty(),
                box(0, 0, 0, 16, 1.25, 16),
                box(0.3, 1.25, 0.3, 15.7, 1.55, 15.7),
                box(0.8, 1.55, 0.8, 15.2, 2.5, 15.2),
                box(2.9, 2.5, 2.9, 13.1, 9, 13.1),
                box(0.1, 1.55, 0.1, 3.4, 3.2, 3.4),
                box(0.48, 3.2, 0.48, 3.02, 3.6, 3.02),
                box(0.65, 3.6, 0.65, 2.85, 9.8, 2.85),
                box(0.48, 9.8, 0.48, 3.02, 10.2, 3.02),
                box(0.2, 10.2, 0.2, 3.3, 11.1, 3.3),
                box(0.1, 1.55, 12.6, 3.4, 3.2, 15.9),
                box(0.48, 3.2, 12.98, 3.02, 3.6, 15.52),
                box(0.65, 3.6, 13.15, 2.85, 9.8, 15.35),
                box(0.48, 9.8, 12.98, 3.02, 10.2, 15.52),
                box(0.2, 10.2, 12.7, 3.3, 11.1, 15.8),
                box(12.6, 1.55, 0.1, 15.9, 3.2, 3.4),
                box(12.98, 3.2, 0.48, 15.52, 3.6, 3.02),
                box(13.15, 3.6, 0.65, 15.35, 9.8, 2.85),
                box(12.98, 9.8, 0.48, 15.52, 10.2, 3.02),
                box(12.7, 10.2, 0.2, 15.8, 11.1, 3.3),
                box(12.6, 1.55, 12.6, 15.9, 3.2, 15.9),
                box(12.98, 3.2, 12.98, 15.52, 3.6, 15.52),
                box(13.15, 3.6, 13.15, 15.35, 9.8, 15.35),
                box(12.98, 9.8, 12.98, 15.52, 10.2, 15.52),
                box(12.7, 10.2, 12.7, 15.8, 11.1, 15.8),
                box(5.2, 9, 5.2, 10.8, 9.2, 10.8),
                box(6.8, 9.2, 6.8, 9.2, 9.45, 9.2),
                box(0, 10.8, 0, 16, 12.5, 5.5),
                box(0, 10.8, 10.5, 16, 12.5, 16),
                box(0, 10.8, 5.5, 5.5, 12.5, 10.5),
                box(10.5, 10.8, 5.5, 16, 12.5, 10.5),
                box(5.3, 12.5, 5.3, 10.7, 12.7, 5.5),
                box(5.3, 12.5, 10.5, 10.7, 12.7, 10.7),
                box(5.3, 12.5, 5.5, 5.5, 12.7, 10.5),
                box(10.5, 12.5, 5.5, 10.7, 12.7, 10.5),
                box(6.75, 8.75, 13.11, 9.25, 10.5, 13.4),
                box(7.4, 8.3, 13.41, 8.6, 9.9, 13.7),
                box(0.25, 10.5, 0.25, 15.75, 10.8, 5.5),
                box(0.25, 10.5, 10.5, 15.75, 10.8, 15.75),
                box(0.25, 10.5, 5.5, 5.5, 10.8, 10.5),
                box(10.5, 10.5, 5.5, 15.75, 10.8, 10.5),
                box(5.2, 9.2, 5.2, 10.8, 10.5, 5.5),
                box(5.2, 9.2, 10.5, 10.8, 10.5, 10.8),
                box(5.2, 9.2, 5.5, 5.5, 10.5, 10.5),
                box(10.5, 9.2, 5.5, 10.8, 10.5, 10.5)).optimize();
    public ImaginaryResearchTableBlock() {
        this(Properties.of().strength(3).sound(SoundType.STONE).noOcclusion().lightLevel(state -> 15));
    }
    private ImaginaryResearchTableBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResearchTableBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, LyyBlockEntities.RESEARCH_TABLE.get(), ResearchTableBlockEntity::clientTick) : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            ResearchMenu.openTable(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
