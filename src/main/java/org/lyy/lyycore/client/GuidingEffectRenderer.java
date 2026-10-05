package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.entity.guiding.GuidingLaser;

/** The supplied laser showcase is sampled by gameplay phase, rather than its preview loop. */
public final class GuidingEffectRenderer<T extends Entity> extends EntityRenderer<T> {
    private final AnimatedMeshModel laserModel = new AnimatedMeshModel("endless_demand_laser_fx");
    public GuidingEffectRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(T entity) { return ResourceLocation.withDefaultNamespace("textures/block/amethyst_block.png"); }
    @Override public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().inflate(entity instanceof GuidingLaser ? GuidingLaser.LENGTH : 1));
    }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity instanceof GuidingLaser laser) {
            float age = laser.animationTime(partial);
            float charge = laser.tracking() ? 1 : 3.3F;
            float fireDuration = laser.tracking() ? 6 : .2F;
            float sourceTime = laser.firing() ? 2.02F + 1.48F * Math.clamp((age - charge) / fireDuration, 0, 1)
                    : .35F + 1.6F * Math.clamp(age / charge, 0, 1);
            pose.pushPose();
            pose.mulPose(new Quaternionf().rotationTo(new org.joml.Vector3f(0, 0, -1), laser.direction().toVector3f()));
            laserModel.render(pose, buffers, LightTexture.FULL_BRIGHT, "showcase", sourceTime);
            pose.popPose();
            // The asset's sub-pixel aim line disappears at distance; retain a readable gameplay telegraph.
            if (!laser.firing()) beam(pose, buffers, Vec3.ZERO, laser.direction().scale(GuidingLaser.LENGTH), .025, 140);
        } else {
            beam(pose, buffers, new Vec3(0, -.35, 0), new Vec3(0, .35, 0), .22, 230);
            beam(pose, buffers, new Vec3(-.35, 0, 0), new Vec3(.35, 0, 0), .12, 200);
        }
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    private static void beam(PoseStack pose, MultiBufferSource buffers, Vec3 start, Vec3 end, double radius, int alpha) {
        var direction = end.subtract(start).normalize();
        var side = direction.cross(Math.abs(direction.y) > .95 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize();
        var up = direction.cross(side).normalize();
        var vertices = buffers.getBuffer(RenderType.lightning());
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4, b = (i + 1) * Math.PI / 4;
            var first = side.scale(Math.cos(a) * radius).add(up.scale(Math.sin(a) * radius));
            var second = side.scale(Math.cos(b) * radius).add(up.scale(Math.sin(b) * radius));
            for (var point : new Vec3[]{start.add(first), start.add(second), end.add(second), end.add(first)})
                vertices.addVertex(pose.last(), (float)point.x, (float)point.y, (float)point.z).setColor(255, 90, 170, alpha);
        }
    }
}
