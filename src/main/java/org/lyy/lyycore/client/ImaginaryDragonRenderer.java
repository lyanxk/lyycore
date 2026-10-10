package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import org.lyy.lyycore.content.entity.ImaginaryDragon;

public final class ImaginaryDragonRenderer extends EntityRenderer<ImaginaryDragon> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("imaginary_dragon_adult");
    private final float[] teleportPose = model.newPose();
    private final int teleportMotion = model.boneIndex("teleport_motion")*9;
    private final int entryPortal = model.boneIndex("teleport_entry")*9;
    private final int exitPortal = model.boneIndex("teleport_exit")*9;
    public ImaginaryDragonRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(ImaginaryDragon d, Frustum f, double x, double y, double z) { return f.isVisible(d.getBoundingBox().inflate(64)); }
    @Override public void render(ImaginaryDragon dragon, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        float seconds = dragon.actionTicks(partial)/20;
        switch (dragon.action()) {
            case TELEPORT_IN -> teleport(pose, buffers, light, seconds, true);
            case TELEPORT_OUT -> teleport(pose, buffers, light, seconds, false);
            case BREATH -> model.render(pose, buffers, light, "breath_ultimate", ImaginaryDragon.breathAnimationSeconds(dragon.actionTicks(partial)));
            case CHARGE -> model.renderBlended(pose, buffers, light, dragon.tickCount+partial, 0, 0, 1, Math.min(1, seconds*3));
            case FALL -> model.render(pose, buffers, light, "glide", seconds);
            case IDLE -> model.render(pose, buffers, light, "fold_wings", Math.min(1.5F, seconds));
        }
        pose.popPose(); super.render(dragon, yaw, partial, pose, buffers, light);
    }
    private void teleport(PoseStack pose, MultiBufferSource buffers, int light, float seconds, boolean arriving) {
        float time = arriving ? Math.min(5.8F, 2.2F+seconds) : Math.min(2.8F, seconds);
        model.sample("teleport", time, teleportPose);
        // The editor previews both portals 520 model units apart. Anchor only the active half
        // to this entity, ending the arrival at the origin for the following attack animation.
        int hiddenPortal = arriving ? entryPortal : exitPortal;
        for (int axis = 0; axis < 3; axis++) teleportPose[hiddenPortal+6+axis] = 0;
        teleportPose[teleportMotion+1] -= 18;
        if (arriving) {
            teleportPose[teleportMotion] -= 520;
            teleportPose[teleportMotion+2] += 450;
            teleportPose[exitPortal] -= 520;
            teleportPose[exitPortal+1] -= 18;
            teleportPose[exitPortal+2] += 450;
            if (time < 2.6F) for (int axis = 0; axis < 3; axis++) teleportPose[teleportMotion+6+axis] = 0;
        } else {
            teleportPose[entryPortal+1] -= 18;
            if (time >= 2.35F) for (int axis = 0; axis < 3; axis++) teleportPose[teleportMotion+6+axis] = 0;
        }
        model.renderPose(pose, buffers, light, teleportPose);
    }
    @Override public ResourceLocation getTextureLocation(ImaginaryDragon d) { return ResourceLocation.parse("lyycore:textures/entity/imaginary_dragon_adult/imaginary_dragon.png"); }
}
