package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.entity.guiding.GuidingBoss;
import org.lyy.lyycore.content.entity.AnimatedMonster;

/** Source models face -Z and use feet-origin coordinates; no humanoid transforms are applied. */
public final class GuidingMobRenderer<T extends AnimatedMonster> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("lyycore:textures/entity/guiding/white_shell.png");
    private final AnimatedMeshModel model;
    private final AnimatedMeshModel shell;

    public GuidingMobRenderer(EntityRendererProvider.Context context, String name) {
        super(context);
        model = new AnimatedMeshModel(name);
        shell = name.equals("endless_demand") ? new AnimatedMeshModel("guiding_light") : null;
        shadowRadius = name.equals("guiding_light") ? 1 : name.equals("endless_demand") ? 1.5F : .7F;
    }
    @Override public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().inflate(4, 4, 4));
    }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // The replacement entity renders both the opening shell and emerging beast.
        if (entity instanceof GuidingBoss boss && !boss.secondPhase() && !boss.isAlive()) return;
        String clip = entity.animation();
        float time = entity.animationTime(partial);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot)));
        if (entity instanceof GuidingBoss boss) {
            if (boss.action() == GuidingBoss.Action.ABSORB && shell != null) {
                shell.render(pose, buffers, light, "hatch_transition", time);
                time *= .6F; // Fit the 1.2-second hatch into the gameplay's two-second absorption.
            } else if (clip.equals("crystal_pursuit")) time *= 1.35F / 3;
            else if (clip.equals("death")) time = (entity.deathTime + partial) / 20F * 1.8F;
        } else if (clip.equals("aim")) time *= 1.05F / 3;
        model.render(pose, buffers, light, clip, time);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
}
