package org.lyy.lyycore.client;

import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.trial.TrialGuide;

public final class TrialGuideRenderer extends EntityRenderer<TrialGuide> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("trial_guide");
    private final Map<TrialGuide, Transition> transitions = new WeakHashMap<>();
    private final class Transition {
        String clip;
        final float[] pose = model.newPose(), previous = model.newPose();
    }
    public TrialGuideRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .7F; }
    @Override public boolean shouldRender(TrialGuide entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().inflate(6));
    }
    @Override public void render(TrialGuide entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var state = transitions.computeIfAbsent(entity, ignored -> new Transition());
        double time = entity.animationTime(partial);
        if (!entity.animation().equals(state.clip)) {
            if (state.clip == null) model.sample(entity.animation(), time, state.pose);
            System.arraycopy(state.pose, 0, state.previous, 0, state.pose.length);
            state.clip = entity.animation();
        }
        model.sample(state.clip, time, state.pose);
        float weight = (float) Math.min(1, time / .25);
        for (int i=0; i<state.pose.length && weight<1; i++) {
            int channel = i % 9;
            state.pose[i] = channel >= 3 && channel < 6 ? Mth.rotLerp(weight, state.previous[i], state.pose[i])
                    : Mth.lerp(weight, state.previous[i], state.pose[i]);
        }
        pose.pushPose();
        var anchorOffset = entity.anchor().subtract(entity.getPosition(partial));
        pose.translate(anchorOffset.x, anchorOffset.y, anchorOffset.z);
        pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot)));
        var body = entity.pose(partial);
        var offset = body.modelOffset();
        pose.translate(offset.x, offset.y, offset.z);
        pose.scale((float)body.scale(), (float)body.scale(), (float)body.scale());
        pose.translate(0, -.2875, 0); // Source feet start at 4.6 model units.
        model.renderPose(pose, buffers, light, state.pose);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(TrialGuide entity) { return TrialAttackRenderer.TEXTURE; }
}
