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
import net.minecraft.world.phys.Vec3;
import java.io.IOException;
import java.util.*;

/** Pre-baked meshes shared by companions, the challenge mobs and the player wings. */
final class AnimatedMeshModel {
    private record Face(int texture, float[][] vertices) { }
    private record Bone(String name, int parent, float[] pivot, float[] rotation, Face[] faces) { }
    private record Clip(float length, boolean loop, float[][][][] tracks) {
        float sample(int bone, int channel, int axis, float time) {
            float[][] keys = tracks[bone][channel];
            if (keys.length == 0) return channel == 2 ? 1 : 0;
            time = loop && length > 0 ? time % length : Mth.clamp(time, 0, length);
            if (time <= keys[0][0]) return keys[0][axis + 1];
            int low = 0, high = keys.length - 1;
            while (low + 1 < high) {
                int middle = (low + high) >>> 1;
                if (keys[middle][0] <= time) low = middle; else high = middle;
            }
            float[] a = keys[low], b = keys[high];
            if (a.length > 4 && a[4] == 1 && time < b[0]) return a[axis + 1];
            float progress = a[0] == b[0] ? 0 : Mth.clamp((time - a[0]) / (b[0] - a[0]), 0, 1);
            if (a.length > 4 && a[4] == 2 || b.length > 4 && b[4] == 2) {
                float p0 = keys[Math.max(0, low - 1)][axis + 1], p1 = a[axis + 1];
                float p2 = b[axis + 1], p3 = keys[Math.min(keys.length - 1, high + 1)][axis + 1];
                float t = progress;
                return .5F * (2*p1 + (-p0+p2)*t + (2*p0-5*p1+4*p2-p3)*t*t + (-p0+3*p1-3*p2+p3)*t*t*t);
            }
            return Mth.lerp(progress, a[axis + 1], b[axis + 1]);
        }
    }
    private final ResourceLocation[] textures;
    private final Set<Integer> emissiveTextures = new HashSet<>();
    private final Set<Integer> additiveTextures = new HashSet<>();
    private final Bone[] bones;
    private final int[][] children;
    private final PoseStack.Pose[] poses;
    private final float[][] values = new float[3][3];
    private final Map<String, Clip> clips = new HashMap<>();
    private record Locator(float[] position, int[] chain) { }
    private final Map<String, Locator> locators = new HashMap<>();
    private final int[] attackBoneIndices;
    private final WingAttackAnimation wingAttack;
    private final WingAttackAnimation.Sampler attackSampler = this::sampleBone;
    private final float[] wingValues;
    private final boolean[] hidden;
    // Locator queries share a sampled pose but never mutate the pose/visibility used for drawing.
    private final float[] locatorBase, locatorValues;
    private final Map<String, Vec3> locatorResults = new HashMap<>();
    private final PoseStack locatorPose = new PoseStack();
    private float locatorAttackTime;
    private boolean locatorCacheValid;

    AnimatedMeshModel(String name) {
        ResourceLocation path = ResourceLocation.parse("lyycore:geometry/" + name + ".json");
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(path).openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("additive_textures")) for (var texture : root.getAsJsonArray("additive_textures")) additiveTextures.add(texture.getAsInt());
            if (root.has("emissive_textures")) for (var texture : root.getAsJsonArray("emissive_textures")) emissiveTextures.add(texture.getAsInt());
            textures = new ResourceLocation[root.getAsJsonArray("textures").size()];
            for (int i = 0; i < textures.length; i++) textures[i] = ResourceLocation.parse(root.getAsJsonArray("textures").get(i).getAsString());
            JsonArray entries = root.getAsJsonArray("bones");
            bones = new Bone[entries.size()];
            wingValues = new float[bones.length * 9];
            locatorBase = new float[wingValues.length]; locatorValues = new float[wingValues.length];
            hidden = new boolean[bones.length];
            poses = new PoseStack.Pose[entries.size()];
            var boneIndices = new HashMap<String, Integer>();
            for (int i = 0; i < bones.length; i++) {
                JsonObject bone = entries.get(i).getAsJsonObject();
                JsonArray surfaces = bone.getAsJsonArray("faces");
                Face[] faces = new Face[surfaces.size()];
                for (int f = 0; f < faces.length; f++) {
                    JsonObject face = surfaces.get(f).getAsJsonObject();
                    faces[f] = new Face(face.get("texture").getAsInt(), matrix(face.getAsJsonArray("vertices")));
                }
                bones[i] = new Bone(bone.get("name").getAsString(), bone.get("parent").getAsInt(), vector(bone.getAsJsonArray("pivot")), vector(bone.getAsJsonArray("rotation")), faces);
                boneIndices.putIfAbsent(bones[i].name, i);
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
                clips.put(entry.getKey(), new Clip(animation.get("length").getAsFloat(), !animation.has("loop") || animation.get("loop").getAsBoolean(), tracks));
            }
            if (root.has("locators")) for (var entry : root.getAsJsonObject("locators").entrySet()) {
                var locator = entry.getValue().getAsJsonObject();
                locators.put(entry.getKey(), new Locator(vector(locator.getAsJsonArray("position")), ancestorChain(locator.get("bone").getAsInt())));
            }
            var attackIndices = new ArrayList<Integer>();
            if (root.has("attack_feathers")) for (var entry : root.getAsJsonArray("attack_feathers")) {
                var boneName = entry.getAsJsonObject().get("bone").getAsString();
                var index = boneIndices.get(boneName);
                if (index == null) throw new IllegalArgumentException("Unknown attack feather bone: " + boneName);
                attackIndices.add(index);
            }
            attackBoneIndices = attackIndices.stream().mapToInt(Integer::intValue).toArray();
            wingAttack = new WingAttackAnimation(attackBoneIndices);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load animated mesh " + path, error);
        }
    }
    /** Resolve parent walks once at the resource boundary rather than on every locator query. */
    private int[] ancestorChain(int bone) {
        var chain = new ArrayList<Integer>();
        for (int current = bone; current >= 0; current = bones[current].parent) {
            if (current >= bones.length || chain.size() >= bones.length)
                throw new IllegalArgumentException("Invalid locator bone hierarchy");
            chain.add(current);
        }
        Collections.reverse(chain);
        return chain.stream().mapToInt(Integer::intValue).toArray();
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
    void renderBlended(PoseStack pose, MultiBufferSource buffers, int light, float age, float walkTime, float walking, float flying, float gliding) {
        Arrays.fill(hidden, false);
        Clip idle = clips.get("idle"), walk = clips.get("walk"), fly = clips.getOrDefault("fly", idle), glide = clips.getOrDefault("glide", fly);
        for (int i = 0; i < bones.length; i++) if (bones[i].parent < 0)
            updatePoses(pose, i, idle, walk, fly, glide, age / 20, walkTime, walking, flying, gliding);
        drawFaces(buffers, light);
    }
    void render(PoseStack pose, MultiBufferSource buffers, int light, String animation, float seconds) {
        Arrays.fill(hidden, false);
        Clip clip = clips.getOrDefault(animation, clips.get("idle"));
        for (int i = 0; i < bones.length; i++) if (bones[i].parent < 0)
            updatePoses(pose, i, clip, clip, clip, clip, seconds, seconds, 0, 0, 0);
        drawFaces(buffers, light);
    }
    void renderWings(PoseStack pose, MultiBufferSource buffers, int light, String base, float baseTime, float attackTime, int featherCount) {
        Arrays.fill(hidden, false);
        for (int i = 0; i < bones.length; i++) if (bones[i].parent < 0)
            updateWings(pose, i, clips.get(base), baseTime, attackTime, featherCount);
        drawFaces(buffers, light);
    }
    private void updateWings(PoseStack pose, int index, Clip base, float baseTime, float attackTime, int featherCount) {
        int feather = WingAttackAnimation.legacyFeather(bones[index].name);
        var phase = WingAttackAnimation.phase(feather, attackTime, featherCount);
        Clip clip = phase == null ? base : clips.get(phase.clip);
        float time = phase == null ? baseTime : WingAttackAnimation.seconds(phase, feather, attackTime, featherCount);
        pose.pushPose();
        transform(pose, index, clip, clip, clip, clip, time, time, 0, 0, 0);
        poses[index] = pose.last().copy();
        for (int child : children[index]) updateWings(pose, child, base, baseTime, attackTime, featherCount);
        pose.popPose();
    }
    private void drawFaces(MultiBufferSource buffers, int light) {
        // Finish one texture before requesting another; these render types share a backing buffer.
        for (int texture = 0; texture < textures.length; texture++) {
            boolean effect = additiveTextures.contains(texture) || textures[texture].getPath().endsWith("_fx.png");
            VertexConsumer vertices = buffers.getBuffer(effect ? RenderType.eyes(textures[texture]) : RenderType.entityCutoutNoCull(textures[texture]));
            int illumination = (emissiveTextures.contains(texture) || textures[texture].getPath().endsWith("eye_light.png") || effect) ? LightTexture.FULL_BRIGHT : light;
            for (int bone = 0; bone < bones.length; bone++) if (!hidden[bone]) for (Face face : bones[bone].faces) if (face.texture == texture)
                for (float[] v : face.vertices) vertices.addVertex(poses[bone], v[0], v[1], v[2])
                        .setColor(255, 255, 255, 255).setUv(v[3], v[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(illumination).setNormal(poses[bone], v[5], v[6], v[7]);
        }
    }
    float[] newPose() { return new float[bones.length * 9]; }
    void sample(String animation, float time, float[] result) {
        var clip = clips.getOrDefault(animation, clips.get("idle"));
        for (int bone = 0; bone < bones.length; bone++) for (int channel = 0; channel < 3; channel++) for (int axis = 0; axis < 3; axis++)
            result[bone * 9 + channel * 3 + axis] = clip.sample(bone, channel, axis, time);
    }
    private void sampleBone(String animation, int bone, float time, float[] result) {
        var clip = clips.get(animation);
        for (int channel = 0; channel < 3; channel++) for (int axis = 0; axis < 3; axis++)
            result[bone * 9 + channel * 3 + axis] = clip.sample(bone, channel, axis, time);
    }
    void renderWingPose(PoseStack pose, MultiBufferSource buffers, int light, float[] base, float attackTime, int count) {
        System.arraycopy(base, 0, wingValues, 0, base.length);
        Arrays.fill(hidden, false);
        wingAttack.apply(wingValues, hidden, attackTime, count, attackSampler);
        for (int i = 0; i < bones.length; i++) if (bones[i].parent < 0) updateWingPose(pose, i);
        drawFaces(buffers, light);
    }
    private void updateWingPose(PoseStack pose, int bone) {
        pose.pushPose();
        transformPose(pose, bone, wingValues, false);
        poses[bone] = pose.last().copy();
        for (int child : children[bone]) { hidden[child] |= hidden[bone]; updateWingPose(pose, child); }
        pose.popPose();
    }
    private void transformPose(PoseStack pose, int index, float[] values, boolean ignoreScale) {
        var bone = bones[index];
        float[] parent = bone.parent < 0 ? null : bones[bone.parent].pivot;
        int offset = index * 9;
        pose.translate((bone.pivot[0] - (parent == null ? 0 : parent[0]) + values[offset]) / 16,
                (bone.pivot[1] - (parent == null ? 0 : parent[1]) + values[offset + 1]) / 16,
                (bone.pivot[2] - (parent == null ? 0 : parent[2]) + values[offset + 2]) / 16);
        pose.mulPose(Axis.ZP.rotationDegrees(bone.rotation[2] + values[offset + 5]));
        pose.mulPose(Axis.YP.rotationDegrees(bone.rotation[1] + values[offset + 4]));
        pose.mulPose(Axis.XP.rotationDegrees(bone.rotation[0] + values[offset + 3]));
        if (!ignoreScale) pose.scale(values[offset + 6], values[offset + 7], values[offset + 8]);
    }
    Vec3 attackLocator(int feather, String kind, float[] base, float attackTime) {
        String name = "attack_" + kind + "_" + (feather + 1);
        var locator = locators.get(name);
        if (locator == null) return Vec3.ZERO;
        // Base arrays are reused and mutated by the caller: identity alone is not a valid cache key.
        if (!locatorCacheValid || Float.compare(attackTime, locatorAttackTime) != 0 || !Arrays.equals(base, locatorBase)) {
            System.arraycopy(base, 0, locatorBase, 0, base.length);
            System.arraycopy(base, 0, locatorValues, 0, base.length);
            wingAttack.apply(locatorValues, null, attackTime, 16, attackSampler);
            locatorAttackTime = attackTime; locatorCacheValid = true; locatorResults.clear();
        }
        var cached = locatorResults.get(name);
        if (cached != null) return cached;
        locatorPose.pushPose();
        try {
            for (int bone : locator.chain) transformPose(locatorPose, bone, locatorValues, true);
            var point = locatorPose.last().pose().transformPosition(new org.joml.Vector3f(locator.position[0], locator.position[1], locator.position[2]));
            var result = new Vec3(point.x, point.y, point.z);
            locatorResults.put(name, result);
            return result;
        } finally { locatorPose.popPose(); }
    }
    /** Draw the actual detached feather, with its tip along projectile-local -Z. */
    void renderAttackFeather(PoseStack pose, MultiBufferSource buffers, int light, int feather) {
        var bone = bones[attackBoneIndices[feather]];
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90));
        for (int texture = 0; texture < textures.length; texture++) {
            var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(textures[texture]));
            for (Face face : bone.faces) if (face.texture == texture)
                for (float[] v : face.vertices) vertices.addVertex(pose.last(), v[0], v[1], v[2])
                        .setColor(255, 255, 255, 255).setUv(v[3], v[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(pose.last(), v[5], v[6], v[7]);
        }
        pose.popPose();
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
