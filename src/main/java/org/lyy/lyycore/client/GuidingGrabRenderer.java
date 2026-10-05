package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.entity.guiding.GuidingBoss;
import org.lyy.lyycore.content.entity.guiding.GuidingGrab;

public final class GuidingGrabRenderer extends EntityRenderer<GuidingGrab> {
    private final PinkEnergyRopeModel model = new PinkEnergyRopeModel();
    public GuidingGrabRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(GuidingGrab entity) {
        return ResourceLocation.parse("lyycore:textures/entity/guiding/pink_energy.png");
    }
    @Override public boolean shouldRender(GuidingGrab entity, Frustum frustum, double x, double y, double z) {
        var owner = entity.owner();
        var bounds = owner == null ? entity.getBoundingBox() : new AABB(owner.position(), entity.position());
        return frustum.isVisible(bounds.inflate(4));
    }
    @Override public void render(GuidingGrab entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!(entity.owner() instanceof GuidingBoss boss)) return;
        var phase = entity.phase();
        var target = entity.target();
        Vec3 entityPosition = entity.getPosition(partial);
        boolean attached = phase != GuidingGrab.Phase.EXTEND && phase != GuidingGrab.Phase.RELEASE;
        Vec3 center = attached && target != null ? target.getPosition(partial).add(0, target.getBbHeight() * .5, 0) : entityPosition;
        Vec3 origin = boss.laserOrigin().add(boss.getPosition(partial).subtract(boss.position()));
        float scale = Math.max(1, target == null ? 1 : (target.getBbWidth() * .5F + .12F) / .40625F);
        Vec3 offset = origin.subtract(center);
        float angle = (float)(Math.atan2(offset.x, offset.z) - Math.atan2(-9, -25));
        Vec3 caster = offset.yRot(-angle).scale(16 / scale).add(0, 12, 0);
        float seconds = entity.phaseSeconds(partial), fade = 1;
        if (phase == GuidingGrab.Phase.RELEASE && (entity.releasedFrom() == GuidingGrab.Phase.EXTEND || entity.releasedFrom() == GuidingGrab.Phase.WRAP)) {
            fade = Math.max(0, 1 - seconds / .18F);
            phase = entity.releasedFrom(); seconds = entity.releaseTime();
        }
        String clip = switch (phase) {
            case EXTEND -> "grab_extend";
            case WRAP -> "grab_wrap";
            case PULL -> "grab_pull";
            case BOUND -> "bound_idle";
            case RELEASE -> "release";
        };
        if (phase == GuidingGrab.Phase.EXTEND) seconds = .24F;
        pose.pushPose();
        Vec3 translation = center.subtract(entityPosition);
        pose.translate(translation.x, translation.y, translation.z);
        pose.mulPose(Axis.YP.rotation(angle));
        pose.scale(scale, scale, scale); pose.translate(0, -.75, 0);
        model.render(pose, buffers, clip, seconds, caster, fade);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
}
