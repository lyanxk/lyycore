package org.lyy.lyycore.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;
import org.lyy.lyycore.content.blocks.LargeStructureBlock;
import org.lyy.lyycore.content.blocks.PureSmeltingPlantBlock;

/** A scanned port can refer to the same receiver as several other multiblock parts. */
public final class EnergyReceiver {
    private EnergyReceiver() { }

    /** Controller identifies one machine; input is the block exposing its energy capabilities. */
    public record Endpoint(BlockPos controller, BlockPos input) {
        public boolean loaded(Level level) { return level.hasChunkAt(controller) && level.hasChunkAt(input); }
    }

    public static Endpoint resolve(Level level, BlockPos connection) {
        if (!level.hasChunkAt(connection)) return new Endpoint(connection, connection);
        var state = level.getBlockState(connection);
        if (state.getBlock() instanceof SquareMachineBlock) {
            var center = SquareMachineBlock.center(connection, state);
            return new Endpoint(center, center);
        }
        if (state.getBlock() instanceof LargeStructureBlock) {
            var center = LargeStructureBlock.center(connection, state);
            var input = state.getBlock() instanceof PureSmeltingPlantBlock
                    ? LargeStructureBlock.partPos(center, state.getValue(LargeStructureBlock.FACING), LargeStructureBlock.FRONT)
                    : center;
            return new Endpoint(center, input);
        }
        return new Endpoint(connection, connection);
    }

    public static BlockPos controller(Level level, BlockPos connection) { return resolve(level, connection).controller(); }
}
