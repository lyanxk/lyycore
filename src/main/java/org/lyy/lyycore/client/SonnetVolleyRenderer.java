package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.entity.SonnetVolley;

public class SonnetVolleyRenderer extends EntityRenderer<SonnetVolley> {
    public static final net.minecraft.client.resources.model.ModelResourceLocation MODEL =
            net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    "lyycore", "item/sanctuary_bow/companion_arrow"));
    public SonnetVolleyRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(SonnetVolley entity) { return TextureAtlas.LOCATION_BLOCKS; }
    @Override public boolean shouldRender(SonnetVolley entity, Frustum frustum, double x, double y, double z) { return true; }
    @Override public void render(SonnetVolley volley, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var path = volley.path();
        float age = Math.max(0, volley.getAge() - 1 + partial);
        var vertices = buffers.getBuffer(RenderType.lightning());
        for (int i = 0; i < SonnetVolley.BOUNCES; i++) {
            float elapsed = age - (float) i / SonnetVolley.BOUNCES_PER_TICK;
            if (elapsed <= 0 || elapsed >= SonnetVolley.TRAIL_TICKS) continue;
            Vec3 start = path.get(i).subtract(volley.position());
            Vec3 end = path.get(i).lerp(path.get(i + 1), Math.min(1, elapsed * SonnetVolley.BOUNCES_PER_TICK)).subtract(volley.position());
            SonnetDomeRenderer.beam(vertices, pose, start, end, 0.055F, (int) (220 * (1 - Math.max(0, elapsed - 1) / 2)));
            SonnetDomeRenderer.beam(vertices, pose, start, end, 0.14F, (int) (45 * (1 - elapsed / 3)));
        }
        float bounceProgress = age * SonnetVolley.BOUNCES_PER_TICK;
        if (bounceProgress < SonnetVolley.BOUNCES) {
            int segment = (int) bounceProgress;
            Vec3 position = path.get(segment).lerp(path.get(segment + 1), bounceProgress - segment).subtract(volley.position());
            Vec3 direction = path.get(segment + 1).subtract(path.get(segment));
            pose.pushPose();
            pose.translate(position.x, position.y, position.z);
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(direction.x, direction.z)) - 90));
            pose.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()))));
            pose.translate(-0.5, -0.5, -0.5);
            var mc = Minecraft.getInstance();
            SonnetMaterialRenderer.render(mc.getModelManager().getModel(MODEL), pose, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, true, false);
            pose.popPose();
        }
    }
}
