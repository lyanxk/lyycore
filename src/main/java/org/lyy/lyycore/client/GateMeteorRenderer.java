package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.entity.GateMeteor;

public final class GateMeteorRenderer extends EntityRenderer<GateMeteor> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse(
            "lyycore:textures/entity/crystal_meteor/crystal_meteor.png");
    private final AnimatedMeshModel model = new AnimatedMeshModel("crystal_meteor");

    public GateMeteorRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(GateMeteor meteor, Frustum frustum, double x, double y, double z) {
        // The mesh is centered on the entity and extends below its vanilla bounding box.
        return frustum.isVisible(meteor.getBoundingBox().inflate(2));
    }

    @Override public void render(GateMeteor meteor, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // The artist's forward axis is -Z; flight rotates around this axis every four seconds.
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), meteor.flightDirection().toVector3f()));
        model.render(pose, buffers, light, "flight", (meteor.tickCount + partial) / 20F);
        pose.popPose();
        super.render(meteor, yaw, partial, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(GateMeteor meteor) { return TEXTURE; }
}
