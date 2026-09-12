package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.LyyCore;

public class SonnetArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "item/whisper_of_the_past/crystal_arrow"));

    public SonnetArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T arrow, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, arrow.yRotO, arrow.getYRot()) - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, arrow.xRotO, arrow.getXRot())));
        float shake = arrow.shakeTime - partialTick;
        if (shake > 0) pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3) * shake));
        pose.translate(-0.5, -0.5, -0.5);
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(MODEL);
        minecraft.getItemRenderer().renderModelLists(model, ItemStack.EMPTY, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS)));
        pose.popPose();
        super.render(arrow, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T arrow) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
