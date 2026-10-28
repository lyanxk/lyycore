package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.lyy.lyycore.content.equipment.SpacetimeArmorItem;
import org.lyy.lyycore.registry.LyyItems;

final class SpacetimeHaloLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType[] STAGES = new RenderType[4], FINALITY_STAGES = new RenderType[4];
    static {
        for (int i=0; i<4; i++) FINALITY_STAGES[i]=RenderType.entityTranslucentEmissive(ResourceLocation.parse("lyycore:textures/entity/finality/halo_"+(i+1)+".png"));
        for (int i = 0; i < STAGES.length; i++) STAGES[i] = RenderType.entityTranslucentEmissive(
                ResourceLocation.parse("lyycore:textures/entity/spacetime_halo/" + (i + 1) + ".png"));
    }
    private static final Model EMPTY_ARMOR = new Model(RenderType::entityCutoutNoCull) {
        @Override public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay, int color) { }
    };

    SpacetimeHaloLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    static void registerArmor(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return EMPTY_ARMOR; // Also suppress enchanted glint and trims on the worn armor.
            }
        }, LyyItems.SPACETIME_HELMET.get(), LyyItems.SPACETIME_CHESTPLATE.get(), LyyItems.SPACETIME_LEGGINGS.get(), LyyItems.SPACETIME_BOOTS.get(),
                org.lyy.lyycore.content.finality.FinalityContent.HELMET.get(), org.lyy.lyycore.content.finality.FinalityContent.CHEST.get(),
                org.lyy.lyycore.content.finality.FinalityContent.LEGS.get(), org.lyy.lyycore.content.finality.FinalityContent.BOOTS.get());
    }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 float walk, float speed, float partial, float age, float yaw, float pitch) {
        int pieces = SpacetimeArmorItem.equippedPieces(player) + org.lyy.lyycore.content.finality.FinalityArmorItem.pieces(player);
        if (pieces == 0 || player.isInvisible() || player.isSpectator()) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Keep the halo at the back-of-head height, but orient it with the torso rather than the gaze.
        pose.translate(0, -.25, .3);
        // Each supplied image already contains every ring for that stage: one quad, not four overlaid images.
        var vertices = buffers.getBuffer(org.lyy.lyycore.content.finality.FinalityArmorItem.pieces(player)>0 ? FINALITY_STAGES[pieces-1] : STAGES[pieces-1]);
        vertex(vertices, pose, -1, -1, 0, 0);
        vertex(vertices, pose, -1, 1, 0, 1);
        vertex(vertices, pose, 1, 1, 1, 1);
        vertex(vertices, pose, 1, -1, 1, 0);
        pose.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack pose, float x, float y, float u, float v) {
        vertices.addVertex(pose.last().pose(), x, y, 0).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
    }
}
