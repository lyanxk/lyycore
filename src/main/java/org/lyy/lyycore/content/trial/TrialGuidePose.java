package org.lyy.lyycore.content.trial;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;

/** Bounds are baked alongside the mesh. Gameplay never evaluates the mesh's bones or faces. */
public record TrialGuidePose(AABB bounds, double scale) {
    private static final Map<String, double[][]> FRAMES = load();
    private static Map<String, double[][]> load() {
        var result = new HashMap<String, double[][]>();
        try (var stream = TrialGuidePose.class.getResourceAsStream("/assets/lyycore/shapes/trial_guide.json")) {
            if (stream == null) throw new IllegalStateException("Missing trial guide bounds");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            json.entrySet().forEach(entry -> {
                var frames = entry.getValue().getAsJsonArray();
                var rows = new double[frames.size()][6];
                for (int i=0; i<rows.length; i++) for (int j=0; j<6; j++) rows[i][j] = frames.get(i).getAsJsonArray().get(j).getAsDouble();
                result.put(entry.getKey(), rows);
            });
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot load trial guide bounds", error); }
        return Map.copyOf(result);
    }
    public static TrialGuidePose sample(String clip, float seconds, float yaw) {
        var frames = FRAMES.getOrDefault(clip, FRAMES.get("idle"));
        double frame = Math.max(0, seconds*20);
        frame = clip.equals("idle") ? frame%(frames.length-1) : Math.min(frame, frames.length-1);
        int a = (int)frame, b = Math.min(a+1, frames.length-1);
        double[] values = new double[6];
        for (int i=0; i<6; i++) values[i] = Mth.lerp(frame-a, frames[a][i], frames[b][i]);
        var bounds = new AABB(values[0],values[1],values[2],values[3],values[4],values[5]);
        double angle = Math.toRadians(180-yaw), c = Math.abs(Math.cos(angle)), s = Math.abs(Math.sin(angle));
        double extent = Math.max(bounds.getYsize(), Math.max(bounds.getXsize()*c+bounds.getZsize()*s, bounds.getXsize()*s+bounds.getZsize()*c));
        // The expanded showcase is over six blocks wide. Fit the complete model within the specified three-block body.
        return new TrialGuidePose(bounds, Math.min(1, 2.8/Math.max(.001, extent)));
    }
    public Vec3 modelOffset() { return bounds.getCenter().scale(1-scale); }
    public AABB worldBounds(Vec3 anchor, float yaw) {
        var center = bounds.getCenter();
        var scaled = AABB.ofSize(center, Math.max(.01,bounds.getXsize()*scale), Math.max(.01,bounds.getYsize()*scale), Math.max(.01,bounds.getZsize()*scale));
        double angle = Math.toRadians(180-yaw), c = Math.cos(angle), s = Math.sin(angle);
        var rotated = new Vec3(center.x*c+center.z*s, center.y, center.z*c-center.x*s).add(anchor);
        return AABB.ofSize(rotated, scaled.getXsize()*Math.abs(c)+scaled.getZsize()*Math.abs(s), scaled.getYsize(),
                scaled.getXsize()*Math.abs(s)+scaled.getZsize()*Math.abs(c));
    }
}
