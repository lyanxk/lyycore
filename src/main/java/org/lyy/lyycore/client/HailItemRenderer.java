package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.lyy.lyycore.registry.LyyItems;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class HailItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static HailItemRenderer renderer;
    private AnimatedMeshModel model;
    private HailItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
    public static void reload() { if (renderer != null) renderer.model = null; }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new HailItemRenderer(); return renderer;
            }
        }, LyyItems.HAIL.get());
    }
    public static BakedModel wrap(BakedModel original) {
        return new BakedModelWrapper<>(original) {
            @Override public boolean isCustomRenderer() { return true; }
            @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                original.applyTransform(context, pose, leftHand); return this;
            }
        };
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new AnimatedMeshModel("hail_flower");
        var mc = Minecraft.getInstance();
        float time = mc.level == null ? 0 : (mc.level.getGameTime() % 240 + mc.getTimer().getGameTimeDeltaPartialTick(true)) / 20;
        pose.pushPose(); pose.translate(.5, .3, .5);
        model.render(pose, buffers, light, "idle", time); pose.popPose();
    }
}
