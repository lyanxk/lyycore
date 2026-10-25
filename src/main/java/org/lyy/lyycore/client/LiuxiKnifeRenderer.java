package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.entity.LiuxiKnife;
import org.lyy.lyycore.registry.LyyItems;

public final class LiuxiKnifeRenderer extends EntityRenderer<LiuxiKnife> {
    private final ItemRenderer items;
    private final ItemStack knife = new ItemStack(LyyItems.LIUXI.get());

    public LiuxiKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
    }

    @Override public void render(LiuxiKnife entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.getXRot() - 45));
        items.renderStatic(knife, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(LiuxiKnife entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
