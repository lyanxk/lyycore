package org.lyy.lyycore.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.io.IOException;
import java.util.*;

/** A small renderer for the two supplied, pre-baked meshes and their linear animation tracks. */
final class EnderCompanionModel {
    private record Face(int texture, float[][] vertices) { }
    private record Bone(int parent, float[] pivot, float[] rotation, Face[] faces) { }
    private record Clip(float length, float[][][][] tracks) {
        float sample(int bone, int channel, int axis, float time) {
            float[][] keys = tracks[bone][channel];
            if (keys.length == 0) return channel == 2 ? 1 : 0;
            time %= length;
            if (time <= keys[0][0]) return keys[0][axis + 1];
            // Binary search the sampled walk tracks (up to 101 keys per channel).
            int low = 0, high = keys.length - 1;
            while (low + 1 < high) {
                int middle = (low + high) >>> 1;
                if (keys[middle][0] <= time) low = middle; else high = middle;
            }
            float[] a = keys[low], b = keys[high];
            float progress = a[0] == b[0] ? 0 : Mth.clamp((time - a[0]) / (b[0] - a[0]), 0, 1);
            return Mth.lerp(progress, a[axis + 1], b[axis + 1]);
        }
    }
    private final ResourceLocation[] textures;
    private final Bone[] bones;
    private final int[][] children;
    private final PoseStack.Pose[] poses;
    private final float[][] values = new float[3][3];
    private final Map<String, Clip> clips = new HashMap<>();

    EnderCompanionModel(String stage) {
        ResourceLocation path = ResourceLocation.parse("lyycore:geometry/ender_dragon_" + stage + ".json");
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(path).openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            textures = new ResourceLocation[root.getAsJsonArray("textures").size()];
            for (int i = 0; i < textures.length; i++) textures[i] = ResourceLocation.parse(root.getAsJsonArray("textures").get(i).getAsString());
            JsonArray entries = root.getAsJsonArray("bones");
            bones = new Bone[entries.size()];
            poses = new PoseStack.Pose[entries.size()];
            for (int i = 0; i < bones.length; i++) {
                JsonObject bone = entries.get(i).getAsJsonObject();
                JsonArray surfaces = bone.getAsJsonArray("faces");
                Face[] faces = new Face[surfaces.size()];
                for (int f = 0; f < faces.length; f++) {
                    JsonObject face = surfaces.get(f).getAsJsonObject();
                    faces[f] = new Face(face.get("texture").getAsInt(), matrix(face.getAsJsonArray("vertices")));
                }
                bones[i] = new Bone(bone.get("parent").getAsInt(), vector(bone.getAsJsonArray("pivot")), vector(bone.getAsJsonArray("rotation")), faces);
            }
            children = new int[bones.length][];
            for (int i = 0; i < bones.length; i++) {
                final int parent = i;
                children[i] = java.util.stream.IntStream.range(i + 1, bones.length).filter(child -> bones[child].parent == parent).toArray();
            }
            for (var entry : root.getAsJsonObject("animations").entrySet()) {
                JsonObject animation = entry.getValue().getAsJsonObject();
                float[][][][] tracks = new float[bones.length][3][][];
                for (var bone : tracks) Arrays.setAll(bone, channel -> new float[0][]);
                for (var track : animation.getAsJsonObject("tracks").entrySet()) {
                    JsonObject channels = track.getValue().getAsJsonObject();
                    String[] names = {"position", "rotation", "scale"};
                    for (int c = 0; c < names.length; c++) if (channels.has(names[c]))
                        tracks[Integer.parseInt(track.getKey())][c] = matrix(channels.getAsJsonArray(names[c]));
                }
                clips.put(entry.getKey(), new Clip(animation.get("length").getAsFloat(), tracks));
            }
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load ender companion model " + path, error);
        }
    }
    private static float[] vector(JsonArray array) {
        float[] result = new float[array.size()];
        for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsFloat();
        return result;
    }
    private static float[][] matrix(JsonArray array) {
        float[][] result = new float[array.size()][];
        for (int i = 0; i < result.length; i++) result[i] = vector(array.get(i).getAsJsonArray());
        return result;
    }
    void render(PoseStack pose, MultiBufferSource buffers, int light, float age, float walkTime, float walking, float flying, float gliding) {
        Clip idle = clips.get("idle"), walk = clips.get("walk"), fly = clips.getOrDefault("fly", idle), glide = clips.getOrDefault("glide", fly);
        for (int i = 0; i < bones.length; i++) if (bones[i].parent < 0)
            updatePoses(pose, i, idle, walk, fly, glide, age / 20, walkTime, walking, flying, gliding);
        // Finish one texture before requesting another; these render types share a backing buffer.
        for (int texture = 0; texture < textures.length; texture++) {
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(textures[texture]));
            int illumination = textures[texture].getPath().endsWith("eye_light.png") ? LightTexture.FULL_BRIGHT : light;
            for (int bone = 0; bone < bones.length; bone++) for (Face face : bones[bone].faces) if (face.texture == texture)
                for (float[] v : face.vertices) vertices.addVertex(poses[bone], v[0], v[1], v[2])
                        .setColor(255, 255, 255, 255).setUv(v[3], v[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(illumination).setNormal(poses[bone], v[5], v[6], v[7]);
        }
    }
    private void updatePoses(PoseStack pose, int index, Clip idle, Clip walk, Clip fly, Clip glide,
                            float time, float walkTime, float walking, float flying, float gliding) {
        pose.pushPose();
        transform(pose, index, idle, walk, fly, glide, time, walkTime, walking, flying, gliding);
        poses[index] = pose.last().copy();
        for (int child : children[index]) updatePoses(pose, child, idle, walk, fly, glide, time, walkTime, walking, flying, gliding);
        pose.popPose();
    }
    private void transform(PoseStack pose, int index, Clip idle, Clip walk, Clip fly, Clip glide,
                           float time, float walkTime, float walking, float flying, float gliding) {
        Bone bone = bones[index];
        float[] parent = bone.parent < 0 ? null : bones[bone.parent].pivot;
        for (int channel = 0; channel < 3; channel++) for (int axis = 0; axis < 3; axis++) {
            float ground = Mth.lerp(walking, idle.sample(index, channel, axis, time), walk.sample(index, channel, axis, walkTime));
            float air = Mth.lerp(gliding, fly.sample(index, channel, axis, time), glide.sample(index, channel, axis, time));
            values[channel][axis] = Mth.lerp(flying, ground, air);
        }
        pose.translate((bone.pivot[0] - (parent == null ? 0 : parent[0]) + values[0][0]) / 16,
                (bone.pivot[1] - (parent == null ? 0 : parent[1]) + values[0][1]) / 16,
                (bone.pivot[2] - (parent == null ? 0 : parent[2]) + values[0][2]) / 16);
        pose.mulPose(Axis.ZP.rotationDegrees(bone.rotation[2] + values[1][2]));
        pose.mulPose(Axis.YP.rotationDegrees(bone.rotation[1] + values[1][1]));
        pose.mulPose(Axis.XP.rotationDegrees(bone.rotation[0] + values[1][0]));
        pose.scale(values[2][0], values[2][1], values[2][2]);
    }
}
