package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.skills.FeatherAttack;

final class AegisWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final AnimatedMeshModel mesh = new AnimatedMeshModel("white_aegis_wings");

    AegisWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 float walk, float speed, float partial, float age, float yaw, float pitch) {
        if (!AegisWings.unlocked(player) || !AegisWings.visible(player) || player.isInvisible() || player.isSpectator()) return;
        float hitTime = (player.level().getGameTime() - AegisWings.shieldStarted(player) + partial) / 20F;
        String clip = "idle";
        float time = age / 20;
        if (hitTime >= 0 && hitTime < 1.29F) {
            if (hitTime < 0.45F) { clip = "shield_deploy"; time = hitTime; }
            else if (hitTime < 0.69F) { clip = "shield_hit"; time = hitTime - 0.45F; }
            else { clip = "shield_recall"; time = hitTime - 0.69F; }
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Source models use feet-origin, upward Y; the player model uses downward Y.
        pose.translate(0, 1.5, 0);
        pose.scale(1, -1, 1);
        float attackTime = FeatherAttackRenderer.age(player, partial);
        if (attackTime >= 0 && attackTime < FeatherAttack.DURATION) {
            mesh.renderWings(pose, buffers, light, clip, time, attackTime);
        } else mesh.render(pose, buffers, light, clip, time);
        pose.popPose();
    }
}
