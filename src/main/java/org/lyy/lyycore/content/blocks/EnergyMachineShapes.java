package org.lyy.lyycore.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Disjoint collision solids matching the cleaned Blockbench exterior surfaces. */
final class EnergyMachineShapes {
    static final Map<Direction, VoxelShape> ENERGY_CELL = horizontal(
            Block.box(0.5, 0, 0.5, 3.0, 2.25, 3.0),
            Block.box(0.5, 0, 13, 3.0, 2.25, 15.5),
            Block.box(1, 1.5, 3.0, 15, 2.5, 13),
            Block.box(1, 2.25, 1, 15, 2.5, 3.0),
            Block.box(1, 2.25, 13, 15, 2.5, 15),
            Block.box(1, 13.0, 1, 3.75, 14.75, 3.75),
            Block.box(1, 13.0, 12.25, 15, 14.25, 13.875),
            Block.box(1, 13.0, 13.875, 15, 13.5, 14.75),
            Block.box(1, 13.0, 14.75, 3.75, 14.75, 15),
            Block.box(1, 13.5, 13.875, 3.75, 14.75, 14.75),
            Block.box(1, 14.25, 12.25, 3.75, 14.75, 13.875),
            Block.box(1.25, 2.5, 1.25, 3.5, 3.125, 3.5),
            Block.box(1.25, 2.5, 12.5, 14.75, 3.125, 12.75),
            Block.box(1.25, 2.5, 12.75, 3.5, 3.125, 14.75),
            Block.box(1.25, 12.5, 1.25, 14.75, 13.0, 14.75),
            Block.box(1.25, 13.0, 3.75, 14.75, 13.5, 12.25),
            Block.box(1.5, 3.125, 1.5, 3.25, 12.5, 3.25),
            Block.box(1.5, 3.125, 12.75, 3.25, 12.5, 14.5),
            Block.box(2, 0.5, 3.0, 14, 1.5, 13),
            Block.box(2.125, 13.5, 3.75, 13.875, 14.25, 12.25),
            Block.box(2.125, 14.25, 3.75, 2.25, 14.5, 12.25),
            Block.box(2.25, 3.25, 3.75, 13.75, 12.25, 4.25),
            Block.box(2.25, 3.25, 4.25, 13.75, 3.75, 12.25),
            Block.box(2.25, 3.75, 11.75, 13.75, 12.25, 12.25),
            Block.box(2.25, 5.25, 7, 13.75, 10.75, 7.25),
            Block.box(2.25, 5.25, 8.5, 13.75, 10.75, 8.75),
            Block.box(2.25, 11.75, 4.25, 13.75, 12.25, 11.75),
            Block.box(2.5, 3.75, 4.25, 13.5, 11.75, 7),
            Block.box(2.5, 3.75, 7, 13.5, 5.25, 11.75),
            Block.box(2.5, 5.25, 7.25, 13.5, 11.75, 8.5),
            Block.box(2.5, 5.25, 8.75, 13.5, 11.75, 11.75),
            Block.box(2.5, 10.75, 7, 13.5, 11.75, 7.25),
            Block.box(2.5, 10.75, 8.5, 13.5, 11.75, 8.75),
            Block.box(3.0, 0.5, 2, 15.5, 2.25, 3.0),
            Block.box(3.0, 0.5, 13, 15.5, 2.25, 14),
            Block.box(3.0, 1.5, 1, 15.5, 2.25, 2),
            Block.box(3.0, 1.5, 14, 15.5, 2.25, 15),
            Block.box(3.25, 2.5, 3.5, 12.75, 12.5, 3.75),
            Block.box(3.25, 2.5, 3.75, 12.75, 3.25, 12.5),
            Block.box(3.25, 3.125, 3.25, 12.75, 12.5, 3.5),
            Block.box(3.25, 3.125, 12.5, 12.75, 12.5, 12.75),
            Block.box(3.25, 3.25, 12.25, 12.75, 12.5, 12.5),
            Block.box(3.25, 12.25, 3.75, 12.75, 12.5, 12.25),
            Block.box(3.5, 2.5, 3.25, 14.75, 3.125, 3.5),
            Block.box(3.5, 3.125, 2.5, 5.5, 12.5, 3.25),
            Block.box(3.75, 3.25, 12.75, 12.25, 12.25, 13.25),
            Block.box(3.75, 13.0, 1.25, 15, 13.5, 3.75),
            Block.box(3.75, 13.5, 2.125, 15, 14.5, 2.25),
            Block.box(3.75, 13.5, 2.25, 15, 14.25, 3.75),
            Block.box(3.75, 14.25, 13.75, 15, 14.5, 13.875),
            Block.box(4.375, 3.5, 2.375, 4.625, 3.75, 2.5),
            Block.box(4.375, 11.875, 2.375, 4.625, 12.125, 2.5),
            Block.box(5.75, 3, 2, 10.25, 3.5, 3.25),
            Block.box(5.75, 3.5, 2, 6.25, 12.5, 3.25),
            Block.box(6.25, 3.5, 2.5, 10.25, 12.5, 3.25),
            Block.box(6.25, 12, 2, 10.25, 12.5, 2.5),
            Block.box(6.625, 4.875, 2.375, 9.375, 5.9375, 2.5),
            Block.box(6.625, 6.1875, 2.375, 9.375, 7.25, 2.5),
            Block.box(6.625, 7.5, 2.375, 9.375, 8.5625, 2.5),
            Block.box(6.625, 8.8125, 2.375, 9.375, 9.875, 2.5),
            Block.box(6.625, 10.125, 2.375, 9.375, 11.1875, 2.5),
            Block.box(6.75, 5.0, 2.125, 9.25, 5.8125, 2.375),
            Block.box(6.75, 6.3125, 2.125, 9.25, 7.125, 2.375),
            Block.box(6.75, 7.625, 2.125, 9.25, 8.4375, 2.375),
            Block.box(6.75, 8.9375, 2.125, 9.25, 9.75, 2.375),
            Block.box(6.75, 10.25, 2.125, 9.25, 11.0625, 2.375),
            Block.box(7.0625, 3.375, 1.875, 8.9375, 4.5625, 2),
            Block.box(7.0625, 3.5, 2, 8.9375, 4.5625, 2.5),
            Block.box(7.375, 3.5625, 1.75, 8.625, 4.375, 1.875),
            Block.box(7.875, 5.25, 13.25, 8.125, 10.75, 13.5),
            Block.box(9.75, 3.5, 2, 10.25, 12, 2.5),
            Block.box(10.5, 3.125, 2.5, 12.5, 12.5, 3.25),
            Block.box(11.375, 3.5, 2.375, 11.625, 3.75, 2.5),
            Block.box(11.375, 11.875, 2.375, 11.625, 12.125, 2.5),
            Block.box(12.25, 13.0, 1, 15, 14.75, 1.25),
            Block.box(12.25, 13.0, 14.75, 15, 14.75, 15),
            Block.box(12.25, 13.5, 1.25, 15, 14.75, 2.125),
            Block.box(12.25, 13.5, 13.875, 15, 14.75, 14.75),
            Block.box(12.25, 14.25, 2.25, 15, 14.75, 3.75),
            Block.box(12.25, 14.25, 12.25, 15, 14.75, 13.75),
            Block.box(12.25, 14.5, 2.125, 15, 14.75, 2.25),
            Block.box(12.25, 14.5, 13.75, 15, 14.75, 13.875),
            Block.box(12.5, 2.5, 1.25, 14.75, 3.125, 3.25),
            Block.box(12.5, 2.5, 12.75, 14.75, 3.125, 14.75),
            Block.box(12.75, 3.125, 1.5, 14.5, 12.5, 3.25),
            Block.box(12.75, 3.125, 12.75, 14.5, 12.5, 14.5),
            Block.box(13, 0, 0.5, 15.5, 2.25, 1),
            Block.box(13, 0, 1, 15.5, 1.5, 2),
            Block.box(13, 0, 2, 15.5, 0.5, 3.0),
            Block.box(13, 0, 13, 15.5, 0.5, 15.5),
            Block.box(13, 0.5, 14, 15.5, 1.5, 15.5),
            Block.box(13, 1.5, 15, 15.5, 2.25, 15.5),
            Block.box(13.25, 2.5, 7, 13.875, 3.25, 9),
            Block.box(13.75, 14.25, 3.75, 13.875, 14.5, 12.25));

    static final Map<Direction, VoxelShape> GENERATOR = horizontal(
            Block.box(0.5, 0, 0.5, 3.0, 2.25, 3.0),
            Block.box(0.5, 0, 13, 3.0, 2.25, 15.5),
            Block.box(1, 1.5, 3.0, 15, 2.5, 13),
            Block.box(1, 2.25, 1, 15, 2.5, 3.0),
            Block.box(1, 2.25, 13, 15, 2.5, 15),
            Block.box(1, 13.0, 1, 3.75, 14.75, 3.75),
            Block.box(1, 13.0, 12.25, 3.75, 14.75, 15),
            Block.box(1.25, 2.5, 1.25, 3.5, 3.125, 3.5),
            Block.box(1.25, 2.5, 12.5, 14.75, 3.125, 13),
            Block.box(1.25, 2.5, 13, 3.5, 3.125, 14.75),
            Block.box(1.5, 3.125, 1.5, 3.25, 13.0, 3.25),
            Block.box(1.5, 3.125, 12.75, 14.5, 4.75, 13),
            Block.box(1.5, 3.125, 13, 3.25, 13.0, 14.5),
            Block.box(1.5, 4.75, 12.75, 3.25, 13.0, 13),
            Block.box(1.5, 13.0, 3.75, 3.25, 14.25, 12.25),
            Block.box(2, 0.5, 3.0, 14, 1.5, 13),
            Block.box(2.875, 2.875, 7.875, 13.125, 3.75, 8.125),
            Block.box(3.0, 0.5, 2, 15.5, 2.25, 3.0),
            Block.box(3.0, 0.5, 13, 15.5, 2.25, 14),
            Block.box(3.0, 1.5, 1, 15.5, 2.25, 2),
            Block.box(3.0, 1.5, 14, 15.5, 2.25, 15),
            Block.box(3.0, 2.5, 3.5, 13, 4.25, 7.875),
            Block.box(3.0, 2.5, 7.875, 13, 2.875, 12.5),
            Block.box(3.0, 2.875, 8.125, 13, 4.25, 12.5),
            Block.box(3.0, 3.125, 3.25, 13, 4.25, 3.5),
            Block.box(3.0, 3.125, 12.5, 13, 4.25, 12.75),
            Block.box(3.0, 3.75, 7.875, 13, 4.25, 8.125),
            Block.box(3.25, 3.125, 3.0, 14.5, 4.25, 3.25),
            Block.box(3.25, 4.25, 13, 14.5, 4.75, 13.5),
            Block.box(3.25, 4.75, 13, 3.75, 13.0, 13.25),
            Block.box(3.5, 2.5, 2.5, 14.75, 3.125, 3.5),
            Block.box(3.5, 3.125, 2.5, 12.5, 4.75, 3.0),
            Block.box(3.75, 13.0, 12.75, 15, 14, 14.25),
            Block.box(4.5, 3, 2.125, 11.5, 3.125, 2.5),
            Block.box(4.5, 3.125, 2.125, 4.625, 4.0, 2.5),
            Block.box(4.625, 3.875, 2.125, 11.5, 4.0, 2.5),
            Block.box(4.75, 3.125, 2.375, 11.25, 3.875, 2.5),
            Block.box(5.5, 4.25, 5.5, 10.5, 4.75, 10.5),
            Block.box(6.5, 4.75, 6.5, 9.5, 5.25, 9.5),
            Block.box(11.375, 3.125, 2.125, 11.5, 3.875, 2.5),
            Block.box(12.25, 4.75, 13, 14.5, 13.0, 13.25),
            Block.box(12.25, 13.0, 1, 15, 14.75, 3.75),
            Block.box(12.25, 13.0, 12.25, 15, 14.75, 12.75),
            Block.box(12.25, 13.0, 14.25, 15, 14.75, 15),
            Block.box(12.25, 14, 12.75, 15, 14.75, 14.25),
            Block.box(12.5, 2.5, 1.25, 14.75, 3.125, 2.5),
            Block.box(12.5, 2.5, 13, 14.75, 3.125, 14.75),
            Block.box(12.75, 3.125, 1.5, 14.5, 13.0, 3.0),
            Block.box(12.75, 3.125, 13, 14.5, 4.25, 14.5),
            Block.box(12.75, 4.25, 3.0, 14.5, 13.0, 3.25),
            Block.box(12.75, 4.25, 13.5, 14.5, 13.0, 14.5),
            Block.box(12.75, 4.75, 12.75, 14.5, 13.0, 13),
            Block.box(12.75, 4.75, 13.25, 14.5, 13.0, 13.5),
            Block.box(12.75, 13.0, 3.75, 14.5, 14.25, 12.25),
            Block.box(13, 0, 0.5, 15.5, 2.25, 1),
            Block.box(13, 0, 1, 15.5, 1.5, 2),
            Block.box(13, 0, 2, 15.5, 0.5, 3.0),
            Block.box(13, 0, 13, 15.5, 0.5, 15.5),
            Block.box(13, 0.5, 14, 15.5, 1.5, 15.5),
            Block.box(13, 1.5, 15, 15.5, 2.25, 15.5));

    private static Map<Direction, VoxelShape> horizontal(VoxelShape... parts) {
        Map<Direction, VoxelShape> result = new EnumMap<>(Direction.class);
        VoxelShape north = Shapes.empty();
        for (VoxelShape part : parts) {
            north = Shapes.or(north, part);
        }
        result.put(Direction.NORTH, north.optimize());
        VoxelShape current = north;
        // Same clockwise Y rotation as blockstates: north -> east -> south -> west.
        for (Direction direction : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0],
                            Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            current = rotated[0].optimize();
            result.put(direction, current);
        }
        return Map.copyOf(result);
    }

    private EnergyMachineShapes() {}
}
