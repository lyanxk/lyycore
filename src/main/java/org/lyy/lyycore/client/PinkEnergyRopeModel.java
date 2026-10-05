package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Retains the authored coils and motion, fitting only the tether to live endpoints. */
final class PinkEnergyRopeModel {
    private static final Vec3 SOURCE_ROOT = new Vec3(-9, 5, -25);
    private static final Vec3 CONTACT = new Vec3(6.5, 12, 0);
    private final AnimatedMeshModel mesh = new AnimatedMeshModel("pink_energy_rope");
    private final float[] sampled = mesh.newPose();
    private final int wrap = mesh.boneIndex("wrap_one_turn"), knot = mesh.boneIndex("origin_knot");
    private final int[] tether = new int[24];
    private final int[] coils = new int[64], sparks = new int[8];
    private final int tip = mesh.boneIndex("capture_tip");
    private final Vec3[] pivots = new Vec3[24], rotations = new Vec3[24], points = new Vec3[25];
    private final float[] lengths = new float[24];

    PinkEnergyRopeModel() {
        for (int i = 0; i < coils.length; i++) coils[i] = mesh.boneIndex("coil_%02d".formatted(i + 1));
        for (int i = 0; i < sparks.length; i++) sparks[i] = mesh.boneIndex("spark_" + (i + 1));
        for (int i = 0; i < tether.length; i++) {
            tether[i] = mesh.boneIndex("tether_%02d".formatted(i + 1));
            pivots[i] = mesh.bonePivot(tether[i]); rotations[i] = mesh.boneRotation(tether[i]);
            lengths[i] = mesh.boneLength(tether[i]);
            if (lengths[i] <= 0) throw new IllegalStateException("Empty rope segment " + i);
        }
    }

    void render(PoseStack pose, MultiBufferSource buffers, String clip, float seconds, Vec3 caster, float fade) {
        mesh.sample(clip, seconds, sampled);
        Vec3 sourceEnd = CONTACT.add(position(wrap));
        // Flight is driven by the server projectile. Its tip is the live contact
        // point; sampling the completed extension prevents a second visual lag.
        for (int i = 0; i < tether.length; i++) {
            float weight = i / 24F;
            Vec3 authored = pivots[i].add(position(tether[i]));
            Vec3 bend = authored.subtract(SOURCE_ROOT.lerp(sourceEnd, weight));
            points[i] = caster.lerp(CONTACT, weight).add(bend);
        }
        points[24] = CONTACT;
        for (int i = 0; i < tether.length; i++) {
            int offset = tether[i] * 9;
            setPosition(tether[i], points[i].subtract(pivots[i]));
            Vec3 direction = points[i + 1].subtract(points[i]);
            var angles = direction.lengthSqr() < 1e-12 ? new Vector3f()
                    : new Quaternionf().rotationTo(new Vector3f(0, 0, 1), direction.normalize().toVector3f()).getEulerAnglesZYX(new Vector3f());
            sampled[offset + 3] = (float)Math.toDegrees(angles.x) - (float)rotations[i].x;
            sampled[offset + 4] = (float)Math.toDegrees(angles.y) - (float)rotations[i].y;
            sampled[offset + 5] = (float)Math.toDegrees(angles.z) - (float)rotations[i].z;
            sampled[offset + 8] = (float)direction.length() / lengths[i];
        }
        // The preview moves its dummy target. In game, the server moves the real
        // target and the renderer follows it, so that translation must not double.
        setPosition(wrap, Vec3.ZERO);
        setPosition(knot, caster.subtract(mesh.bonePivot(knot)));
        // Early cancellation fades the current partial wrap, never a completed ring.
        if (fade < 1) {
            for (int bone : coils) scale(bone, fade);
            for (int bone : sparks) scale(bone, fade);
            scale(tip, fade);
            for (int bone : tether) { sampled[bone * 9 + 6] *= fade; sampled[bone * 9 + 7] *= fade; }
        }
        scale(knot, clip.equals("release") ? Math.max(0, 1 - seconds / .18F) : fade);
        mesh.renderPose(pose, buffers, LightTexture.FULL_BRIGHT, sampled);
    }

    private Vec3 position(int bone) { int o = bone * 9; return new Vec3(sampled[o], sampled[o + 1], sampled[o + 2]); }
    private void setPosition(int bone, Vec3 point) {
        int o = bone * 9; sampled[o] = (float)point.x; sampled[o + 1] = (float)point.y; sampled[o + 2] = (float)point.z;
    }
    private void scale(int bone, float factor) { for (int axis = 6; axis < 9; axis++) sampled[bone * 9 + axis] *= factor; }
}
