package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.CaptureCageBlockEntity;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class CaptureCageBlock extends MachineBlock {
    public CaptureCageBlock() { super(Properties.of().strength(5).sound(SoundType.METAL).noOcclusion()); }
    @Override protected MapCodec<CaptureCageBlock> codec() { return MapCodec.unit(this); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CaptureCageBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, LyyBlockEntities.CAPTURE_CAGE.get(), CaptureCageBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof CaptureCageBlockEntity cage)
            server.openMenu(cage, buf -> { buf.writeBlockPos(pos); net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buf, cage.captiveName()); });
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CaptureCageBlockEntity cage
                ? List.of(cage.asItem()) : List.of(new ItemStack(this));
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof CaptureCageBlockEntity cage)
            popResource(level, pos, cage.asItem());
        return super.playerWillDestroy(level, pos, state, player);
    }
}
