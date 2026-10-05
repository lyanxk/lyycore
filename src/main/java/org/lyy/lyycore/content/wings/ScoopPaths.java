package org.lyy.lyycore.content.wings;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.phys.Vec3;

/** Small, pre-baked trajectories; the dedicated server never loads the wing mesh. */
public final class ScoopPaths {
    public static final int COUNT = 6;
    private static final Vec3[][] PATHS = load();

    public static Vec3 position(int path, int tick, Vec3 origin, float yaw) {
        Vec3 local = PATHS[path][Math.clamp(tick, 0, WingsScoop.DURATION)];
        return origin.add(local.yRot((float) Math.toRadians(-yaw)));
    }

    private static Vec3[][] load() {
        try (var stream = ScoopPaths.class.getResourceAsStream("/data/lyycore/wings/scoop_paths.json")) {
            if (stream == null) throw new IllegalStateException("Missing scoop paths");
            var paths = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("paths");
            if (paths.size() != COUNT) throw new IllegalStateException("Expected six scoop paths");
            var result = new Vec3[COUNT][WingsScoop.DURATION + 1];
            for (int i = 0; i < COUNT; i++) {
                var path = paths.get(i).getAsJsonArray();
                if (path.size() != result[i].length) throw new IllegalStateException("Incomplete scoop path");
                for (int tick = 0; tick < path.size(); tick++) {
                    var point = path.get(tick).getAsJsonArray();
                    result[i][tick] = new Vec3(point.get(0).getAsDouble(), point.get(1).getAsDouble(), point.get(2).getAsDouble());
                }
            }
            return result;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load scoop paths", exception);
        }
    }

    private ScoopPaths() { }
}
