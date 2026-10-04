package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.item.SonnetBowItem;

final class SonnetItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static SonnetItemRenderer instance;
    private static BakedModel selectedModel;
    private static ItemDisplayContext selectedContext;
    private static PoseStack selectedPose;

    private SonnetItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static SonnetItemRenderer instance() {
        if (instance == null) instance = new SonnetItemRenderer();
        return instance;
    }

    static void select(BakedModel model, ItemDisplayContext context, PoseStack pose) {
        selectedModel = model;
        selectedContext = context;
        selectedPose = pose;
    }

    static void clearSelection() {
        selectedModel = null;
        selectedContext = null;
        selectedPose = null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = selectedContext == context && selectedPose == pose ? selectedModel : null;
        clearSelection();
        if (model == null) {
            // Support callers which invoke BEWLR directly without ItemRenderer's transform hook.
            var mc = Minecraft.getInstance();
            model = SonnetBowModel.unwrap(mc.getItemRenderer().getModel(stack, mc.level, null, 0));
        }
        SonnetMaterialRenderer.renderBow(model, pose, buffers, light, overlay,
                SonnetBowItem.isCrystal(stack), context);
    }
}
