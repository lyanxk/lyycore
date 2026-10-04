package org.lyy.lyycore.content.blocks;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.phys.shapes.*;

/** Import-time bounds, clipped to each occupied cell and cached on both logical sides. */
final class SquareMachineShapes {
    private SquareMachineShapes() { }
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
