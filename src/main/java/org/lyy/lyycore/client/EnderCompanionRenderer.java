package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.entity.EnderCompanion;

public final class EnderCompanionRenderer extends EntityRenderer<EnderCompanion> {
    private final AnimatedMeshModel hatchling = new AnimatedMeshModel("ender_dragon_hatchling");
    private final AnimatedMeshModel juvenile = new AnimatedMeshModel("ender_dragon_juvenile");
    public EnderCompanionRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0.5F; }
    @Override public void render(EnderCompanion dragon, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, dragon.yBodyRotO, dragon.yBodyRot)));
        float walkTime = Mth.lerp(partial, dragon.previousWalkTime, dragon.walkTime);
        float flying = Mth.lerp(partial, dragon.previousFlightBlend, dragon.flightBlend);
        float walking = Mth.clamp(dragon.walkAnimation.speed(partial) * 4, 0, 1);
        float gliding = Mth.clamp((float) dragon.getDeltaMovement().horizontalDistance() * 8, 0, 1);
        (dragon.stage() == 0 ? hatchling : juvenile).renderBlended(pose, buffers, light, dragon.tickCount + partial, walkTime, walking, flying, gliding);
        pose.popPose();
        super.render(dragon, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(EnderCompanion entity) {
        return ResourceLocation.parse("lyycore:textures/entity/ender_dragon_" + (entity.stage() == 0 ? "hatchling" : "juvenile") + "/scales.png");
    }
}
