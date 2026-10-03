package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.lyy.lyycore.content.entity.*;

public final class GuardianRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("lyycore", "textures/entity/imaginary_guardian.png");
    private final GuardianModel model;
    public GuardianRenderer(EntityRendererProvider.Context context, String mesh) {
        super(context);
        model = new GuardianModel(mesh);
    }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        float age = entity.tickCount + partial;
        boolean fastOrbit = false;
        if (entity instanceof ImaginaryGuardian guardian) {
            fastOrbit = guardian.spinTicks() > 0;
            float scale = guardian.summonTicks() == 0 ? 1 : Math.max(0.1F, 1 - (float) guardian.summonTicks() / ImaginaryGuardian.SUMMON_TICKS);
            pose.scale(scale, scale, scale);
        } else if (entity instanceof GuardianSpikes spikes) {
            float rise = age - spikes.delay();
            float height = rise < 0 ? 0.05F : Math.min(1, Math.min((rise + 1) / 4, (20 - rise) / 6));
            pose.scale(1, Math.max(0.01F, height), 1);
        }
        model.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), LightTexture.FULL_BRIGHT, age, fastOrbit);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
}
