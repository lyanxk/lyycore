package org.lyy.lyycore.content.blockEntities;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.lyy.lyycore.content.logistics.*;
import org.lyy.lyycore.content.menu.LogisticsMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;

public final class LogisticsMatrixBlockEntity extends BlockEntity implements MenuProvider {
    private boolean tickets;
    private long channelRevision = -1;
    private List<LogisticsChannels.Channel> channels = List.of();
    public LogisticsMatrixBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.LOGISTICS_MATRIX.get(), pos, state); }
    public GlobalPos address() { return GlobalPos.of(level.dimension(), worldPosition); }
    private void chunks(boolean force) {
        if (!(level instanceof ServerLevel server)) return;
        int cx = worldPosition.getX() >> 4, cz = worldPosition.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++)
            LogisticsTickets.TICKETS.forceChunk(server, worldPosition, x, z, force, true);
        tickets = force;
    }
    public void release() {
        if (level instanceof ServerLevel server) {
            LogisticsChannels.get(server).removeMatrix(address()); chunks(false);
        }
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, LogisticsMatrixBlockEntity matrix) {
        if (!matrix.tickets) matrix.chunks(true);
        var directory = LogisticsChannels.get((ServerLevel) level);
        if (matrix.channelRevision != directory.revision()) {
            matrix.channels = directory.atSource(matrix.address()); matrix.channelRevision = directory.revision();
        }
        for (var channel : matrix.channels) channel.transfer();
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new LogisticsMenu(id, inventory, this); }
}
