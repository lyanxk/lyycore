package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.wings.FeatherAttack;

final class AegisWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final AnimatedMeshModel mesh = new AnimatedMeshModel("white_aegis_wings");

    AegisWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 float walk, float speed, float partial, float age, float yaw, float pitch) {
        if (!AegisWings.unlocked(player) || player.isInvisible() || player.isSpectator()) return;
        if (WingsScoopRenderer.active(player) || WingsPursuitRenderer.active(player)) return;
        if (AegisWings.level(player) >= 2) {
            var animation = FourWingAnimation.get(player);
            animation.update(player, partial);
            if (!AegisWings.visible(player) && animation.folded()) return;
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            pose.translate(0, 1.5, 0);
            pose.scale(1, -1, 1);
            var attack = FeatherAttackRenderer.current(player);
            FourWingAnimation.model().renderWingPose(pose, buffers, light, animation.pose(),
                    FeatherAttackRenderer.age(player, partial), attack == null ? 16 : attack.featherCount());
            pose.popPose();
            return;
        }
        if (!AegisWings.visible(player)) return;
        float hitTime = (player.level().getGameTime() - AegisWings.shieldStarted(player) + partial) / 20F;
        float attackTime = FeatherAttackRenderer.age(player, partial);
        var attack = FeatherAttackRenderer.current(player);
        boolean attacking = attack != null && attackTime >= 0 && attackTime < FeatherAttack.duration(attack.featherCount());
        boolean shielding = hitTime >= 0 && hitTime < 1.29F;
        boolean guarding = player.getPersistentData().getBoolean("lyycore:wings_guarding");
        // The first-tier model has no folding clips; keep it stowed until flight or an action needs it.
        if (player.onGround() && !player.getAbilities().flying && !player.isFallFlying()
                && !attacking && !shielding && !guarding) return;
        String clip = "idle";
        float time = age / 20;
        if (shielding) {
            if (hitTime < 0.45F) { clip = "shield_deploy"; time = hitTime; }
            else if (hitTime < 0.69F) { clip = "shield_hit"; time = hitTime - 0.45F; }
            else { clip = "shield_recall"; time = hitTime - 0.69F; }
        } else if (guarding) {
            clip = "shield_hold";
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Source models use feet-origin, upward Y; the player model uses downward Y.
        pose.translate(0, 1.5, 0);
        pose.scale(1, -1, 1);
        if (attacking) {
            mesh.renderWings(pose, buffers, light, clip, time, attackTime, attack.featherCount());
        } else mesh.render(pose, buffers, light, clip, time);
        pose.popPose();
    }
}
