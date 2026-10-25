package org.lyy.lyycore.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A closed photosphere with slow plasma advection and sparse, local filaments.
 * Standard buffers retain depth occlusion without requiring a shader pack or bloom. */
final class StellarCoreMesh {
    private static final ResourceLocation WHITE = ResourceLocation.parse("lyycore:dynamic/stellar_white");
    // The opaque beacon pass is unlit and writes depth: a star should not receive diffuse sunlight.
    private static final RenderType SURFACE = RenderType.beaconBeam(WHITE, false);
    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(WHITE);
    private static final int LONGITUDE = 64, LATITUDE = 32, RIM_SEGMENTS = 64, CURVE_SEGMENTS = 48;
    private static final double TAU = Math.PI * 2;
    private record Vertex(float x, float y, float z, float alpha) { }
    private static final Vertex[] SPHERE = sphere();
    private static final int[] FACES = faces();
    private static final Vertex[] CORONA = corona();
    private static final Vertex[] PROMINENCE = ribbon(true);
    private static final Vertex[][] FILAMENTS = filaments();
    private static final int[] FILAMENT_COUNT = {3, 5, 7}, PROMINENCE_COUNT = {1, 2, 5};
    private static final Field[] FIELDS = {new Field(), new Field(), new Field()};

    private StellarCoreMesh() { }

    static void prepareTexture() {
        // One solid texel carries vertex colors. No UV scroll, texture seams or bespoke shader.
        var pixel = new NativeImage(1, 1, false);
        pixel.setPixelRGBA(0, 0, 0xFFFFFFFF);
        Minecraft.getInstance().getTextureManager().register(WHITE, new DynamicTexture(pixel));
    }

    static void render(PoseStack pose, MultiBufferSource buffers, double seconds, int stage) {
        Field field = FIELDS[stage];
        field.update(seconds, stage);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) (seconds % 240 * 1.5)));
        var surface = buffers.getBuffer(SURFACE);
        for (int index : FACES) {
            Vertex v = SPHERE[index];
            int color = field.colors[index];
            // Beacon uses BLOCK rather than NEW_ENTITY: UV2 is required, overlay UV1 is absent.
            surface.addVertex(pose.last(), v.x, v.y, v.z)
                    .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                    .setUv(.5F, .5F).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), v.x, v.y, v.z);
        }

        var glow = buffers.getBuffer(GLOW);
        // Local spirals rotate about their own surface normals, rather than sliding an entire map.
        for (int i = 0; i < FILAMENT_COUNT[stage]; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 137.508F));
            pose.mulPose(Axis.XP.rotationDegrees(-48 + i * 16));
            double turn = seconds % 240 / 240 * (stage == 0 ? 1 : 2) * (i % 2 == 0 ? 1 : -1);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (i * 79 + turn * 360)));
            int alpha = (int) ((stage == 0 ? 48 : 100) * field.brightness);
            for (Vertex v : FILAMENTS[i % FILAMENTS.length])
                vertex(glow, pose.last(), v, 0xCFDBFF, (int) (v.alpha * alpha),
                        v.x / 1.003F, v.y / 1.003F, v.z / 1.003F);
            pose.popPose();
        }

        for (int i = 0; i < PROMINENCE_COUNT[stage]; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 137.508F + 18));
            pose.mulPose(Axis.ZP.rotationDegrees(28 + i * 67));
            pose.mulPose(Axis.XP.rotationDegrees(i * 31));
            int alpha = (int) ((72 + 22 * Math.sin(seconds % 24 * TAU / 24 + i * 1.7)) * field.brightness);
            for (Vertex v : PROMINENCE)
                vertex(glow, pose.last(), v, 0xC2B8FF, (int) (v.alpha * alpha), 0, 0, 1);
            pose.popPose();
        }
        pose.popPose();

        // One narrow, feathered billboard annulus keeps a crisp silhouette without a transparent shell.
        pose.pushPose();
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        for (Vertex v : CORONA)
            vertex(glow, pose.last(), v, 0xB2A0FF, (int) (v.alpha * 40 * field.brightness), 0, 0, 1);
        pose.popPose();
    }

    /** Shared across matrices of a stage; evaluate the flow once per unique vertex and frame. */
    private static final class Field {
        final int[] colors = new int[SPHERE.length];
        double sampledAt = Double.NaN;
        float brightness = 1;

        void update(double seconds, int stage) {
            if (seconds == sampledAt) return;
            sampledAt = seconds;
            double phase = seconds % 120 * TAU / 120;
            double activity = stage == 0 ? .35 : stage == 1 ? .8 : 1;
            brightness = stage == 2 ? (float) (.96 + .04 * Math.sin(seconds % 20 * TAU / 20)) : 1;
            for (int i = 0; i < SPHERE.length; i++) {
                Vertex v = SPHERE[i];
                // 3D domain warping is continuous at both poles and the meridian.
                // Differential rotation folds broad plasma bands into slowly evolving eddies.
                double twist = phase + activity * (1.3 * Math.sin(2.5 * v.y + phase)
                        + .65 * Math.sin(2.8 * v.z - phase));
                double c = Math.cos(twist), s = Math.sin(twist);
                double x = v.x * c - v.z * s, z = v.x * s + v.z * c;
                double y = v.y + activity * .24 * Math.sin(3 * x - phase);
                double broad = Math.sin(4 * x + 1.5 * Math.sin(3 * y))
                        + .65 * Math.sin(4 * z - 2 * y + phase * 2);
                double folded = Math.sin(7 * y + 3 * x + activity * 2.5 * broad - phase);
                double plasma = .5 + .22 * broad + .10 * folded;
                // Narrow folded crests occupy a small part of the surface, not a blanket of fire noise.
                double crest = Math.max(0, folded);
                crest *= crest;
                crest *= crest;
                crest *= crest * (stage == 0 ? .45 : 1);
                double breakup = .6 + .4 * Math.sin(5 * z - 3 * x + 1.5 * Math.sin(3 * y) + phase);
                crest *= breakup * breakup;
                int red = (int) ((120 + 75 * plasma + 55 * crest) * brightness);
                int green = (int) ((148 + 70 * plasma + 35 * crest) * brightness);
                int blue = (int) ((235 + 15 * plasma + 4 * crest) * brightness);
                colors[i] = red << 16 | green << 8 | blue;
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vertex v, int rgb,
                               int alpha, float nx, float ny, float nz) {
        consumer.addVertex(pose, v.x, v.y, v.z).setColor(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, alpha)
                .setUv(.5F, .5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    private static Vertex[] sphere() {
        Vertex[] result = new Vertex[(LATITUDE - 1) * LONGITUDE + 2];
        result[0] = new Vertex(0, 1, 0, 1);
        result[result.length - 1] = new Vertex(0, -1, 0, 1);
        for (int y = 1; y < LATITUDE; y++) for (int x = 0; x < LONGITUDE; x++) {
            double latitude = (double) y / LATITUDE * Math.PI, longitude = (double) x / LONGITUDE * TAU;
            result[sphereIndex(x, y)] = new Vertex((float) (Math.sin(latitude) * Math.cos(longitude)),
                    (float) Math.cos(latitude), (float) (Math.sin(latitude) * Math.sin(longitude)), 1);
        }
        return result;
    }

    private static int sphereIndex(int x, int y) {
        if (y == 0) return 0;
        if (y == LATITUDE) return (LATITUDE - 1) * LONGITUDE + 1;
        return 1 + (y - 1) * LONGITUDE + x % LONGITUDE;
    }

    private static int[] faces() {
        int[] result = new int[LONGITUDE * LATITUDE * 4];
        int index = 0;
        for (int y = 0; y < LATITUDE; y++) for (int x = 0; x < LONGITUDE; x++) {
            // Outward winding; pole triangles repeat one corner for the standard QUADS buffer.
            result[index++] = sphereIndex(x, y);
            result[index++] = sphereIndex(x + 1, y);
            result[index++] = sphereIndex(x + 1, y + 1);
            result[index++] = sphereIndex(x, y + 1);
        }
        return result;
    }

    private static Vertex[] corona() {
        Vertex[] result = new Vertex[RIM_SEGMENTS * 3 * 4];
        float[] radii = {.995F, 1.018F, 1.055F, 1.105F};
        float[] opacity = {0, 1, .3F, 0};
        int index = 0;
        for (int band = 0; band < 3; band++) for (int segment = 0; segment < RIM_SEGMENTS; segment++) {
            result[index++] = rimPoint(segment, radii[band], opacity[band]);
            result[index++] = rimPoint(segment, radii[band + 1], opacity[band + 1]);
            result[index++] = rimPoint(segment + 1, radii[band + 1], opacity[band + 1]);
            result[index++] = rimPoint(segment + 1, radii[band], opacity[band]);
        }
        return result;
    }

    private static Vertex rimPoint(int segment, float radius, float alpha) {
        double angle = segment % RIM_SEGMENTS * TAU / RIM_SEGMENTS;
        return new Vertex(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle), 0, alpha);
    }

    private static Vertex[][] filaments() {
        Vertex[] flat = ribbon(false);
        Vertex[][] result = new Vertex[3][flat.length];
        for (int size = 0; size < result.length; size++) for (int i = 0; i < flat.length; i++) {
            Vertex v = flat[i];
            float x = v.x * (.75F + size * .12F), y = v.y * (.75F + size * .12F);
            // A small clearance prevents z-fighting with the photosphere's polygon chords.
            result[size][i] = new Vertex(x, y, (float) Math.sqrt(1.003F * 1.003F - x * x - y * y), v.alpha);
        }
        return result;
    }

    /** Bright center, transparent edges and tapered endpoints for both kinds of ribbon. */
    private static Vertex[] ribbon(boolean prominence) {
        Vertex[] result = new Vertex[CURVE_SEGMENTS * 2 * 4];
        int index = 0;
        for (int band = -1; band < 1; band++) for (int segment = 0; segment < CURVE_SEGMENTS; segment++) {
            result[index++] = curvePoint(segment, band, prominence);
            result[index++] = curvePoint(segment, band + 1, prominence);
            result[index++] = curvePoint(segment + 1, band + 1, prominence);
            result[index++] = curvePoint(segment + 1, band, prominence);
        }
        return result;
    }

    private static Vertex curvePoint(int segment, int side, boolean prominence) {
        double t = (double) segment / CURVE_SEGMENTS, taper = Math.sin(t * Math.PI);
        double angle = prominence ? (t - .5) * .54 : t * 5.6 + .22 * Math.sin(t * TAU);
        double radius = prominence ? .99 + .17 * taper + side * .014 * taper
                : .49 - .39 * t + side * .009 * taper;
        float x = (float) (radius * Math.sin(angle)), y = (float) (radius * Math.cos(angle));
        return new Vertex(x, y, 0, side == 0 ? (float) (taper * taper) : 0);
    }
}
