package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.entity.MagicBeam;

public final class MagicBeamRenderer extends EntityRenderer<MagicBeam> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("dragon_laser");
    private final float[] sampled = model.newPose();
    private final int length = model.boneIndex("beam_length")*9;
    private final int impact = model.boneIndex("impact")*9;
    private final int ring = model.boneIndex("shock_ring")*9;
    public MagicBeamRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(MagicBeam e, Frustum f, double x, double y, double z) { return f.isVisible(e.getBoundingBox().expandTowards(e.delta()).inflate(2)); }
    @Override public void render(MagicBeam e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float distance = (float)e.delta().length(); if (distance < .01) return;
        if (e.rifle()) { renderRifle(e, partial, pose, buffers); return; }
        model.sample("fire", (e.tickCount+partial)/6*.42F, sampled);
        sampled[length+6] *= distance/6;
        sampled[impact] -= distance*16-96;
        sampled[ring] *= distance/6;
        pose.pushPose(); pose.mulPose(new Quaternionf().rotationTo(new Vector3f(-1, 0, 0), e.delta().normalize().toVector3f()));
        model.renderPose(pose, buffers, LightTexture.FULL_BRIGHT, sampled); pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(MagicBeam e) { return ResourceLocation.parse("lyycore:textures/entity/dragon_laser/alien_dragon_laser.png"); }

    // Uniform light texture: the similarly named GUI background contains dark artwork.
    private static final ResourceLocation WHITE = ResourceLocation.parse("lyycore:textures/crystal_sanctuary/white.png");
    private static void renderRifle(MagicBeam beam, float partial, PoseStack pose, MultiBufferSource buffers) {
        var direction = beam.delta().normalize();
        var side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < .001) side = new Vec3(1, 0, 0);
        side = side.normalize();
        var up = direction.cross(side).normalize();
        float fade = Math.clamp((beam.lifetime() - beam.tickCount - partial) / 6F, 0, 1);
        var vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        rifleTube(vertices, pose, beam.delta(), side, up, .10, 0x258CFF, (int) (150 * fade));
        rifleTube(vertices, pose, beam.delta(), side, up, .028, 0x9AE8FF, (int) (255 * fade));
    }
    private static void rifleTube(VertexConsumer vertices, PoseStack pose, Vec3 end, Vec3 side, Vec3 up,
                                  double radius, int rgb, int alpha) {
        int color = alpha << 24 | rgb;
        // An eight-sided beam with an end cap remains visible when viewed along its axis.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4, b = (i + 1) * Math.PI / 4;
            Vec3 from = side.scale(Math.cos(a) * radius).add(up.scale(Math.sin(a) * radius));
            Vec3 to = side.scale(Math.cos(b) * radius).add(up.scale(Math.sin(b) * radius));
            Vec3 normal = from.add(to).normalize();
            vertex(vertices, pose, from, normal, color, 0, 0);
            vertex(vertices, pose, to, normal, color, 0, 1);
            vertex(vertices, pose, end.add(to), normal, color, 1, 1);
            vertex(vertices, pose, end.add(from), normal, color, 1, 0);
            normal = end.normalize();
            vertex(vertices, pose, end, normal, color, 0, 0);
            vertex(vertices, pose, end.add(from), normal, color, 1, 0);
            vertex(vertices, pose, end.add(to), normal, color, 1, 1);
            vertex(vertices, pose, end, normal, color, 0, 1);
        }
    }
    private static void vertex(VertexConsumer vertices, PoseStack pose, Vec3 point, Vec3 normal, int color, float u, float v) {
        vertices.addVertex(pose.last().pose(), (float)point.x, (float)point.y, (float)point.z)
                .setColor(color).setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), (float)normal.x, (float)normal.y, (float)normal.z);
    }
}
