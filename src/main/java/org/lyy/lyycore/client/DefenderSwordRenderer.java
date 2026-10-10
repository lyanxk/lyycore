package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.entity.sovereign.DefenderSword;

public final class DefenderSwordRenderer extends EntityRenderer<DefenderSword> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("defender_sword");
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("lyycore:textures/entity/sovereign/white_gold_knight.png");

    public DefenderSwordRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(DefenderSword sword, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(sword.getBoundingBox().inflate(2));
    }
    @Override public void render(DefenderSword sword, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        var velocity = sword.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-6)
            pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, -1, 0), velocity.normalize().toVector3f()));
        pose.scale(DefenderSword.MODEL_SCALE, DefenderSword.MODEL_SCALE, DefenderSword.MODEL_SCALE);
        model.render(pose, buffers, light, "idle", 0);
        pose.popPose();
        super.render(sword, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(DefenderSword sword) { return TEXTURE; }
}
