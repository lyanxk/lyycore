package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.lyy.lyycore.content.entity.FrostChainHook;

public final class FrostChainRenderer extends EntityRenderer<FrostChainHook> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
            ResourceLocation.parse("lyycore:entity/frost_chain_tether"));
    public FrostChainRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(FrostChainHook hook, Frustum frustum, double x, double y, double z) {
        return true; // The tether may be visible even when its endpoint is offscreen.
    }

    @Override public void render(FrostChainHook hook, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var owner = hook.getOwner();
        if (owner == null) return;
        var minecraft = Minecraft.getInstance();
        Vec3 origin = owner.getEyePosition(partial).add(0, -0.4, 0);
        if (owner == minecraft.player && owner == minecraft.getCameraEntity()
                && minecraft.options.getCameraType().isFirstPerson()) {
            var camera = minecraft.gameRenderer.getMainCamera();
            // Anchor in the lower-right of the view, following camera pitch and FOV.
            origin = camera.getPosition().add(camera.getNearPlane().getPointOnPlane(0.65F, -0.65F).scale(8));
        }
        Vec3 start = origin.subtract(hook.getPosition(partial));
        double length = start.length();
        if (length < 1.0E-4) return;
        Vec3 direction = start.scale(-1 / length);
        pose.pushPose();
        pose.translate(start.x, start.y, start.z);
        pose.mulPose(new Quaternionf().rotationTo(0, 0, -1, (float) direction.x, (float) direction.y, (float) direction.z));
        // Authored tether: root (0, 24, 0), forward -Z, length 16 model units.
        // Its live endpoints drive extension; no preview animation or item display transform is applied twice.
        pose.scale(1, 1, (float) length);
        pose.translate(0, -24.0 / 16, 0);
        minecraft.getItemRenderer().renderModelLists(minecraft.getModelManager().getModel(MODEL), ItemStack.EMPTY,
                light, OverlayTexture.NO_OVERLAY, pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)));
        pose.popPose();
        super.render(hook, yaw, partial, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(FrostChainHook hook) { return TextureAtlas.LOCATION_BLOCKS; }
}
