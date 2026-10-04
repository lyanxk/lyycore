package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.entity.LifeRevel;
import org.lyy.lyycore.content.entity.RevelMob;
import org.lyy.lyycore.content.entity.RevelDancer;

public final class RevelRenderer<T extends RevelMob> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("lyycore:textures/entity/life_revel/life_revel_v2.png");
    private final AnimatedMeshModel mesh;

    public RevelRenderer(EntityRendererProvider.Context context, String model) {
        super(context);
        mesh = new AnimatedMeshModel(model);
        shadowRadius = model.equals("life_revel") ? 1 : 0.35F;
    }
    @Override public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                // The summon column extends well above the body's collision box.
                || entity.animation().equals("summon") && frustum.isVisible(entity.getBoundingBox().expandTowards(0, 9, 0).inflate(2, 0, 2))
                || entity instanceof LifeRevel boss && boss.animation().startsWith("laser_")
                && frustum.isVisible(entity.getBoundingBox().inflate(LifeRevel.RANGE));
    }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot)));
        if (entity instanceof RevelDancer) {
            pose.translate(0, 0.5, 0);
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, entity.xRotO, entity.getXRot())));
            pose.translate(0, -0.5, 0);
        }
        String animation = entity.animation();
        float seconds = entity.animationTime(partial);
        // The supplied charge lasts 1.6 seconds; gameplay gives a full two-second telegraph.
        float modelTime = animation.equals("laser_charge") ? Math.min(1.6F, seconds * 0.8F) : seconds;
        if (animation.equals("idle")) modelTime = (entity.tickCount + partial) / 20F;
        mesh.render(pose, buffers, light, animation, modelTime);
        pose.popPose();
        if (entity instanceof LifeRevel boss) renderLaser(boss, pose, buffers, seconds);
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    private static void renderLaser(LifeRevel boss, PoseStack pose, MultiBufferSource buffers, float time) {
        boolean charging = boss.animation().equals("laser_charge");
        boolean firing = boss.animation().equals("laser_fire") && time < 0.3F;
        if (!charging && !firing) return;
        Vec3 direction = boss.beam().normalize();
        Vec3 side = direction.cross(Math.abs(direction.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 up = direction.cross(side).normalize();
        Vec3 start = boss.laserOrigin().subtract(boss.position());
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        if (charging) {
            tube(vertices, pose, start, direction, side, up, 10, 0.025, 255, 173, 216, 65);
            double radius = 0.12 + Math.min(time / 2, 1) * 0.25;
            ring(vertices, pose, start.add(direction.scale(0.5)), side, up, radius, time * 4, 200);
            ring(vertices, pose, start.add(direction.scale(0.7)), side, up, radius * 0.75, -time * 4, 240);
        } else {
            tube(vertices, pose, start, direction, side, up, 10, LifeRevel.LASER_RADIUS + 0.08, 255, 157, 215, 130);
            tube(vertices, pose, start, direction, side, up, 10, LifeRevel.LASER_RADIUS, 255, 247, 255, 245);
        }
    }
    private static Vec3 radial(Vec3 side, Vec3 up, double angle, double radius) {
        return side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
    }
    private static void tube(VertexConsumer vertices, PoseStack pose, Vec3 start, Vec3 direction, Vec3 side, Vec3 up,
                             double length, double radius, int red, int green, int blue, int alpha) {
        Vec3 end = start.add(direction.scale(length));
        for (int segment = 0; segment < 12; segment++) {
            Vec3 a = radial(side, up, segment * Math.PI / 6, radius), b = radial(side, up, (segment + 1) * Math.PI / 6, radius);
            vertex(vertices, pose, start.add(a), red, green, blue, alpha);
            vertex(vertices, pose, start.add(b), red, green, blue, alpha);
            vertex(vertices, pose, end.add(b), red, green, blue, alpha);
            vertex(vertices, pose, end.add(a), red, green, blue, alpha);
        }
    }
    private static void ring(VertexConsumer vertices, PoseStack pose, Vec3 center, Vec3 side, Vec3 up, double radius, double rotation, int alpha) {
        for (int segment = 0; segment < 24; segment++) {
            double a = rotation + segment * Math.PI / 12, b = rotation + (segment + 1) * Math.PI / 12;
            for (Vec3 offset : new Vec3[]{radial(side, up, a, radius), radial(side, up, b, radius),
                    radial(side, up, b, radius + 0.035), radial(side, up, a, radius + 0.035)})
                vertex(vertices, pose, center.add(offset), 255, 206, 239, alpha);
        }
    }
    private static void vertex(VertexConsumer vertices, PoseStack pose, Vec3 point, int red, int green, int blue, int alpha) {
        vertices.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z).setColor(red, green, blue, alpha);
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
}
