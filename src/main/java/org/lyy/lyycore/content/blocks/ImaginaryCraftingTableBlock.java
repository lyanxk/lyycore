package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lyy.lyycore.content.blockEntities.ImaginaryCraftingTableBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class ImaginaryCraftingTableBlock extends BaseEntityBlock {
    public static final MapCodec<ImaginaryCraftingTableBlock> CODEC = simpleCodec(p -> new ImaginaryCraftingTableBlock());
    private static final VoxelShape SHAPE = createShape();

    /** Model coordinates in pixels, matching imaginary_workshop/altar_base and its crystal. */
    private static VoxelShape createShape() {
        var parts = new java.util.ArrayList<VoxelShape>();

        // Two tiers of the stepped octagonal base.
        parts.add(box(0, 0, 2, 16, 0.5, 14));
        parts.add(box(2, 0, 0, 14, 0.5, 2));
        parts.add(box(2, 0, 14, 14, 0.5, 16));
        parts.add(box(0.5, 0.5, 2.5, 15.5, 1, 13.5));
        parts.add(box(2.5, 0.5, 0.5, 13.5, 1, 2.5));
        parts.add(box(2.5, 0.5, 13.5, 13.5, 1, 15.5));

        // Each offering platform has a foot, a narrower stem, and a top plate.
        for (int platform = 0; platform < 8; platform++) {
            double angle = platform * Math.PI / 4;
            double x = 8 + Math.round(5.8 * Math.sin(angle) * 1_000_000) / 1_000_000.0;
            double z = 8 - Math.round(5.8 * Math.cos(angle) * 1_000_000) / 1_000_000.0;
            parts.add(box(x - 1.3, 1, z - 1.3, x + 1.3, 1.6, z + 1.3));
            parts.add(box(x - 0.7, 1.6, z - 0.7, x + 0.7, 3.2, z + 0.7));
            parts.add(box(x - 1.4, 3.2, z - 1.4, x + 1.4, 3.8, z + 1.4));
        }

        // Center pedestal; its diamond and four rim pieces form one solid top.
        parts.add(box(5.8, 1, 5.8, 10.2, 1.7, 10.2));
        parts.add(box(6.3, 1.7, 6.3, 9.7, 2.5, 9.7));
        parts.add(box(6, 2.5, 6, 10, 3.1, 10));

        // The crystal spans y=3.1..9.7, with a diamond cross-section of radius 2.25 at y=6.4.
        // VoxelShapes cannot slope: approximate each half with five 0.66-pixel layers
        // and 0.45-pixel strips, keeping the empty corners around the crystal open.
        for (int layer = 1; layer <= 5; layer++) {
            double bottom = 3.1 + (layer - 1) * 0.66;
            double top = 3.1 + layer * 0.66;
            for (int strip = -layer; strip < layer; strip++) {
                double minX = 8 + strip * 0.45;
                double maxX = minX + 0.45;
                double radiusZ = (layer - Math.min(Math.abs(strip), Math.abs(strip + 1))) * 0.45;
                parts.add(box(minX, bottom, 8 - radiusZ, maxX, top, 8 + radiusZ));
                parts.add(box(minX, 12.8 - top, 8 - radiusZ, maxX, 12.8 - bottom, 8 + radiusZ));
            }
        }
        return Shapes.or(Shapes.empty(), parts.toArray(VoxelShape[]::new)).optimize();
    }

    public ImaginaryCraftingTableBlock() { super(Properties.of().strength(3).sound(SoundType.AMETHYST).noOcclusion()); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ImaginaryCraftingTableBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.IMAGINARY_CRAFTING_TABLE.get(), ImaginaryCraftingTableBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ImaginaryCraftingTableBlockEntity table)
            server.openMenu(table, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ImaginaryCraftingTableBlockEntity table) {
            for (int slot = 0; slot < 9; slot++)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), table.items().getStackInSlot(slot));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
