package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.SpacetimeMatrixBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** The base occupies nine cells; the animated star is rendered above its controller. */
public final class SpacetimeMatrixBlock extends SquareMachineBlock {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 1, 10);

    public SpacetimeMatrixBlock() {
        super(Properties.of().strength(5).sound(SoundType.METAL), "spacetime_matrix", 1, 3);
        registerDefaultState(defaultBlockState().setValue(POWER, 1));
    }

    @Override protected MapCodec<SpacetimeMatrixBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == centerPart() ? new SpacetimeMatrixBlockEntity(pos, state) : null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != centerPart() ? null
                : createTickerHelper(type, LyyBlockEntities.SPACETIME_MATRIX.get(), SpacetimeMatrixBlockEntity::serverTick);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof SpacetimeMatrixBlockEntity matrix)) return;
        int power = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("Power");
        matrix.restorePower(power);
        if (placer instanceof ServerPlayer owner) matrix.ensureChannel(owner);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var controller = center(pos, state);
        if (player instanceof ServerPlayer server && level.getBlockEntity(controller) instanceof SpacetimeMatrixBlockEntity matrix) {
            matrix.ensureChannel(server);
            server.openMenu(matrix);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(center(pos, state)) instanceof SpacetimeMatrixBlockEntity matrix ? matrix : null;
    }

    @Override protected void dropContents(Level level, BlockPos pos, BlockEntity entity) {
        if (!(entity instanceof SpacetimeMatrixBlockEntity matrix)) return;
        matrix.release();
        for (int slot = 0; slot < matrix.items.getSlots(); slot++)
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    matrix.items.extractItem(slot, Integer.MAX_VALUE, false));
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var drop = new ItemStack(this);
        int power = state.getValue(POWER);
        CustomData.update(DataComponents.CUSTOM_DATA, drop, tag -> tag.putInt("Power", power));
        return List.of(drop);
    }
}
