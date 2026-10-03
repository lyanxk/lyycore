package org.lyy.lyycore.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.io.IOException;
import java.util.*;

/** Renders the supplied Blockbench crystal meshes, including their per-face UVs. */
final class GuardianModel {
    private record Bone(String name, String parent, Vec3 pivot, Vec3 rotation, List<Cube> cubes) { }
    private record Cube(Vec3 origin, Vec3 size, Map<String, float[]> uv) { }
    private final List<Bone> bones;
    private final Map<String, Bone> byName = new HashMap<>();
    private final float textureWidth;
    private final float textureHeight;

    GuardianModel(String name) {
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath("lyycore", "models/entity/guardian_" + name + ".json");
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(path).openAsReader()) {
            JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            textureWidth = description.get("texture_width").getAsFloat();
            textureHeight = description.get("texture_height").getAsFloat();
            bones = new ArrayList<>();
            for (JsonElement entry : geometry.getAsJsonArray("bones")) {
                JsonObject bone = entry.getAsJsonObject();
                List<Cube> cubes = new ArrayList<>();
                if (bone.has("cubes")) for (JsonElement box : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = box.getAsJsonObject();
                    Map<String, float[]> faces = new HashMap<>();
                    for (var face : cube.getAsJsonObject("uv").entrySet()) {
                        JsonObject uv = face.getValue().getAsJsonObject();
                        JsonArray start = uv.getAsJsonArray("uv"), size = uv.getAsJsonArray("uv_size");
                        faces.put(face.getKey(), new float[]{start.get(0).getAsFloat(), start.get(1).getAsFloat(), size.get(0).getAsFloat(), size.get(1).getAsFloat()});
                    }
                    cubes.add(new Cube(vector(cube, "origin"), vector(cube, "size"), faces));
                }
                Bone part = new Bone(bone.get("name").getAsString(), bone.has("parent") ? bone.get("parent").getAsString() : null,
                        vector(bone, "pivot"), vector(bone, "rotation"), cubes);
                bones.add(part);
                byName.put(part.name, part);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load guardian model " + path, exception);
        }
    }
    private static Vec3 vector(JsonObject object, String field) {
        if (!object.has(field)) return Vec3.ZERO;
        JsonArray array = object.getAsJsonArray(field);
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }
    void render(PoseStack pose, VertexConsumer vertices, int light, float age, boolean fastOrbit) {
        for (Bone bone : bones) {
            if (bone.cubes.isEmpty() || bone.name.startsWith("volley_")) continue;
            pose.pushPose();
            transform(pose, bone, age, fastOrbit);
            for (Cube cube : bone.cubes) renderCube(pose, vertices, light, cube, bone.pivot);
            pose.popPose();
        }
    }
    private void transform(PoseStack pose, Bone bone, float age, boolean fastOrbit) {
        Bone parent = byName.get(bone.parent);
        if (parent != null) transform(pose, parent, age, fastOrbit);
        Vec3 pivot = bone.pivot.subtract(parent == null ? Vec3.ZERO : parent.pivot).scale(1.0 / 16);
        pose.translate(pivot.x, pivot.y, pivot.z);
        if (bone.name.equals("body")) pose.translate(0, Math.sin(age * 0.06) * 0.06, 0);
        if (bone.name.equals("orbit_horizontal")) pose.mulPose(Axis.YP.rotationDegrees(age * (fastOrbit ? 25 : 2)));
        if (bone.name.equals("projectile_spin")) pose.mulPose(Axis.YP.rotationDegrees(age * 18));
        pose.mulPose(Axis.ZP.rotationDegrees((float) bone.rotation.z));
        pose.mulPose(Axis.YP.rotationDegrees((float) bone.rotation.y));
        pose.mulPose(Axis.XP.rotationDegrees((float) bone.rotation.x));
    }
    private void renderCube(PoseStack pose, VertexConsumer out, int light, Cube cube, Vec3 pivot) {
        Vec3 min = cube.origin.subtract(pivot).scale(1.0 / 16);
        Vec3 max = min.add(cube.size.scale(1.0 / 16));
        float x0 = (float) min.x, y0 = (float) min.y, z0 = (float) min.z;
        float x1 = (float) max.x, y1 = (float) max.y, z1 = (float) max.z;
        face(pose, out, light, cube.uv.get("north"), new float[][]{{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}}, 0,0,-1);
        face(pose, out, light, cube.uv.get("south"), new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}}, 0,0,1);
        face(pose, out, light, cube.uv.get("west"), new float[][]{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}}, -1,0,0);
        face(pose, out, light, cube.uv.get("east"), new float[][]{{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}}, 1,0,0);
        face(pose, out, light, cube.uv.get("up"), new float[][]{{x0,y1,z1},{x1,y1,z1},{x1,y1,z0},{x0,y1,z0}}, 0,1,0);
        face(pose, out, light, cube.uv.get("down"), new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1}}, 0,-1,0);
    }
    private void face(PoseStack pose, VertexConsumer out, int light, float[] uv, float[][] corners, float nx, float ny, float nz) {
        if (uv == null) return;
        float u0 = uv[0] / textureWidth, v0 = uv[1] / textureHeight;
        float u1 = (uv[0] + uv[2]) / textureWidth, v1 = (uv[1] + uv[3]) / textureHeight;
        for (int i = 0; i < 4; i++) {
            float[] point = corners[i];
            out.addVertex(pose.last(), point[0], point[1], point[2]).setColor(255, 255, 255, 255)
                    .setUv(i == 0 || i == 3 ? u0 : u1, i < 2 ? v1 : v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), nx, ny, nz);
        }
    }
}
