package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/** Pre-lit inventory images; other contexts keep the original model and transforms. */
final class InventoryIconModel {
    static final ModelResourceLocation EMBERFALL = icon("emberfall");
    static final ModelResourceLocation THUNDER_SCYTHE = icon("thunder_scythe");

    private static ModelResourceLocation icon(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("lyycore", "item/icons/" + name));
    }

    static BakedModel wrap(BakedModel source, BakedModel icon) {
        if (icon == null) return source;
        return new BakedModelWrapper<>(source) {
            // GuiGraphics chooses lighting before ItemRenderer calls applyTransform.
            @Override public boolean usesBlockLight() { return false; }
            @Override public boolean isGui3d() { return false; }
            @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
                return (context == ItemDisplayContext.GUI ? icon : source).applyTransform(context, pose, leftHand);
            }
        };
    }
}
