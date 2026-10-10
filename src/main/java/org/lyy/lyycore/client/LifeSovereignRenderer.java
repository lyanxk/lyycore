package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.entity.sovereign.LifeSovereign;

public final class LifeSovereignRenderer extends EntityRenderer<LifeSovereign> {
    private final AnimatedMeshModel model;
    private final AnimatedMeshModel cocoon;
    private final float scale;
    private final ResourceLocation texture;
    public LifeSovereignRenderer(EntityRendererProvider.Context context, String name, float scale) {
        super(context); model = new AnimatedMeshModel(name); this.scale = scale; shadowRadius = 1;
        cocoon = name.equals("life_usurper") ? new AnimatedMeshModel("life_cocoon") : null;
        texture = ResourceLocation.parse("lyycore:textures/entity/sovereign/" + switch (name) {
            case "life_defender" -> "white_gold_knight.png";
            case "life_cocoon" -> "white_phase_cocoon.png";
            default -> "violet_blue_mage.png";
        });
    }
    @Override public boolean shouldRender(LifeSovereign entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().inflate(3));
    }
    @Override public void render(LifeSovereign entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot))); pose.scale(scale, scale, scale);
        if (entity.phase() == LifeSovereign.Phase.DEFENDER && entity.animation().equals("idle")) {
            // Vanilla limb motion follows actual movement and eases out when navigation stops.
            // A full 1.2-second walk cycle corresponds to roughly 2.4 blocks of travel.
            float walking = entity.onGround() ? Mth.clamp(entity.walkAnimation.speed(partial) * 4, 0, 1) : 0;
            float walkTime = entity.walkAnimation.position(partial) / 8;
            model.renderBlended(pose, buffers, light, entity.tickCount + partial, walkTime, walking, 0, 0);
        } else {
            model.render(pose, buffers, light, entity.animation(), entity.animationTime(partial));
        }
        if (cocoon != null && entity.animation().equals("phase_emerge") && entity.animationTime(partial) < 2.4F) {
            pose.scale(.6F/scale, .6F/scale, .6F/scale);
            cocoon.render(pose, buffers, light, "hatch", entity.animationTime(partial));
        }
        pose.popPose(); super.render(entity, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(LifeSovereign entity) { return texture; }
}
