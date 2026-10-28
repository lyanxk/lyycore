package org.lyy.lyycore.client.finality;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Vector3f;
import org.lyy.lyycore.client.finality.model.*;
import org.lyy.lyycore.content.finality.*;

public final class FinalitySummonRenderer extends EntityRenderer<FinalitySummon> {
    private static final String[] TEXTURES = {
        "finality_sigil_cube",
        "finality_core_cube",
        "finality_floating_crystal",
        "finality_floating_sword",
        "prayer_book",
        "heart_control_anchor"
    };
    private final ModelPart[] roots = {
        FinalitySigilCubeGeometry.createBodyLayer().bakeRoot(),
        FinalityCoreCubeGeometry.createBodyLayer().bakeRoot(),
        FinalityFloatingCrystalGeometry.createBodyLayer().bakeRoot(),
        FinalityFloatingSwordGeometry.createBodyLayer().bakeRoot(),
        PrayerBookGeometry.createBodyLayer().bakeRoot(),
        HeartControlAnchorGeometry.createBodyLayer().bakeRoot()
    };
    private final AnimatedModel crystal = new AnimatedModel(roots[2]),
            book = new AnimatedModel(roots[4]);
    private final Vector3f animationCache = new Vector3f();

    public FinalitySummonRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    static ResourceLocation texture(String name) {
        return FinalityContent.id("textures/entity/finality/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(FinalitySummon entity) {
        return texture(TEXTURES[entity.kind().ordinal()]);
    }

    @Override
    public void render(
            FinalitySummon entity,
            float yaw,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int light) {
        int kind = entity.kind().ordinal();
        var root = roots[kind];
        root.getAllParts().forEach(ModelPart::resetPose);
        long millis = (long) ((entity.tickCount + partial) * 50);
        if (kind == 2)
            KeyframeAnimations.animate(
                    crystal, FinalityCrystalAnimations.crystal_spin, millis, 1, animationCache);
        if (kind == 4)
            KeyframeAnimations.animate(
                    book, PrayerBookAnimations.page_turn, millis, 1, animationCache);
        pose.pushPose();
        pose.translate(0, 1.5, 0);
        pose.scale(-1, -1, 1);
        root.render(
                pose,
                buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static final class AnimatedModel extends HierarchicalModel<FinalitySummon> {
        private final ModelPart root;

        AnimatedModel(ModelPart root) {
            this.root = root;
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(
                FinalitySummon entity,
                float walk,
                float speed,
                float age,
                float yaw,
                float pitch) {}
    }
}
