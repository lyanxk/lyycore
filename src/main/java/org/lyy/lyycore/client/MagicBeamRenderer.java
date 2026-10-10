package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;
import org.lyy.lyycore.content.entity.MagicBeam;

public final class MagicBeamRenderer extends EntityRenderer<MagicBeam> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("dragon_laser");
    private final float[] sampled = model.newPose();
    private final int length = model.boneIndex("beam_length")*9;
    private final int impact = model.boneIndex("impact")*9;
    private final int ring = model.boneIndex("shock_ring")*9;
    public MagicBeamRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(MagicBeam e, Frustum f, double x, double y, double z) { return f.isVisible(e.getBoundingBox().expandTowards(e.delta()).inflate(2)); }
    @Override public void render(MagicBeam e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float distance = (float)e.delta().length(); if (distance < .01) return;
        model.sample("fire", (e.tickCount+partial)/6*.42F, sampled);
        sampled[length+6] *= distance/6;
        sampled[impact] -= distance*16-96;
        sampled[ring] *= distance/6;
        pose.pushPose(); pose.mulPose(new Quaternionf().rotationTo(new Vector3f(-1, 0, 0), e.delta().normalize().toVector3f()));
        model.renderPose(pose, buffers, LightTexture.FULL_BRIGHT, sampled); pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(MagicBeam e) { return ResourceLocation.parse("lyycore:textures/entity/dragon_laser/alien_dragon_laser.png"); }
}
