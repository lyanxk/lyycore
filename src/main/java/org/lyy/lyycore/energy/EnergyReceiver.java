package org.lyy.lyycore.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;

/** A scanned port can refer to the same receiver as several other multiblock parts. */
public final class EnergyReceiver {
    private EnergyReceiver() { }

    public static BlockPos controller(Level level, BlockPos connection) {
        if (!level.hasChunkAt(connection)) return connection;
        var state = level.getBlockState(connection);
        return state.getBlock() instanceof SquareMachineBlock ? SquareMachineBlock.center(connection, state) : connection;
    }
}
