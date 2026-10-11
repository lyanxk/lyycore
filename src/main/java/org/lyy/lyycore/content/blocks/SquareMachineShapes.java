package org.lyy.lyycore.content.blocks;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.phys.shapes.*;

/** Import-time bounds, clipped to each occupied cell and cached on both logical sides. */
final class SquareMachineShapes {
    private SquareMachineShapes() { }
    static VoxelShape clockwise(VoxelShape source) {
        var result = new java.util.ArrayList<VoxelShape>();
        source.forAllBoxes((x1, y1, z1, x2, y2, z2) -> result.add(Shapes.box(1-z2, y1, x1, 1-z1, y2, x2)));
        return Shapes.or(Shapes.empty(), result.toArray(VoxelShape[]::new));
    }
    static VoxelShape[] load(String name) {
        String path = "/assets/lyycore/geometry/" + name + "_shapes.json";
        try (var input = SquareMachineShapes.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing machine collision: " + path);
            var parts = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            VoxelShape[] shapes = new VoxelShape[parts.size()];
            for (int part = 0; part < shapes.length; part++) {
                VoxelShape shape = Shapes.empty();
                for (var element : parts.get(part).getAsJsonArray()) {
                    var b = element.getAsJsonArray();
                    shape = Shapes.joinUnoptimized(shape, Shapes.box(b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble(),
                            b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble()), BooleanOp.OR);
                }
                shapes[part] = shape.optimize();
            }
            return shapes;
        } catch (IOException error) { throw new IllegalStateException("Cannot load " + path, error); }
    }
}
