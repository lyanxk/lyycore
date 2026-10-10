package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.entity.sovereign.LifeSpell;

public final class LifeSpellRenderer extends EntityRenderer<LifeSpell> {
    private final AnimatedMeshModel meteorModel = new AnimatedMeshModel("crystal_meteor");
    private final AnimatedMeshModel missileModel = new AnimatedMeshModel("life_spell_missile");
    private final AnimatedMeshModel spikesModel = new AnimatedMeshModel("life_spell_spikes");
    private final AnimatedMeshModel meteorEffects = new AnimatedMeshModel("life_spell_meteor_fx");
    private final AnimatedMeshModel prismModel = new AnimatedMeshModel("life_tracking_prism");
    private final float[] prismPose = prismModel.newPose();
    private final int prismAim = prismModel.boneIndex("aim") * 9;
    private final int aimLine = prismModel.boneIndex("aim_line") * 9;
    private final int laserBeam = prismModel.boneIndex("laser_beam") * 9;
    public LifeSpellRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(LifeSpell spell, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        var bounds = spell.getBoundingBox();
        if (spell.kind() == LifeSpell.Kind.CRYSTAL) bounds = bounds.expandTowards(spell.aim());
        return frustum.isVisible(bounds.inflate(spell.kind() == LifeSpell.Kind.METEOR ? 14 : 2));
    }
    @Override public void render(LifeSpell spell, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float ticks = spell.animationTicks(partial), seconds = ticks / 20F;
        switch (spell.kind()) {
            case MISSILE -> {
                Vec3 velocity = spell.getDeltaMovement();
                if (velocity.lengthSqr() > 1.0E-6)
                    pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, -1, 0), velocity.normalize().toVector3f()));
                pose.scale(.76F, .76F, .76F);
                missileModel.render(pose, buffers, LightTexture.FULL_BRIGHT, "idle", seconds);
            }
            case SPIKE -> spikesModel.render(pose, buffers, LightTexture.FULL_BRIGHT, spell.kind().animation, seconds);
            case METEOR -> renderMeteor(pose, buffers, light, seconds);
            case CRYSTAL -> renderPrism(spell, pose, buffers, light, ticks);
        }
        pose.popPose(); super.render(spell, yaw, partial, pose, buffers, light);
    }
    private void renderMeteor(PoseStack pose, MultiBufferSource buffers, int light, float seconds) {
        meteorEffects.render(pose, buffers, LightTexture.FULL_BRIGHT, "cast_meteor", seconds);
        if (seconds < .65F || seconds >= 2.2F) return;
        float formation = Mth.clamp((seconds - .65F) / .55F, 0, 1);
        float fall = Mth.clamp((seconds - 1.5F) / .62F, 0, 1);
        float fade = 1 - Mth.clamp((seconds - 2.12F) / .08F, 0, 1);
        pose.pushPose();
        pose.translate(0, 12 * (1 - fall * fall), 0);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(formation * fade, formation * fade, formation * fade);
        meteorModel.render(pose, buffers, light, "flight", seconds);
        pose.popPose();
    }
    private void renderPrism(LifeSpell spell, PoseStack pose, MultiBufferSource buffers, int light, float ticks) {
        float step = Math.max(0, ticks - 1) % LifeSpell.CRYSTAL_CYCLE_TICKS;
        int fireStarts = LifeSpell.CRYSTAL_TRACK_TICKS + LifeSpell.CRYSTAL_LOCK_TICKS;
        if (ticks < 24) prismModel.sample("summon", ticks / 20F, prismPose);
        else if (step < LifeSpell.CRYSTAL_TRACK_TICKS) prismModel.sample("idle", ticks / 20F, prismPose);
        else if (step < fireStarts) prismModel.sample("lock_on", (step - LifeSpell.CRYSTAL_TRACK_TICKS) / LifeSpell.CRYSTAL_LOCK_TICKS, prismPose);
        else prismModel.sample("laser_fire", (step - fireStarts) / 20F, prismPose);

        Vec3 aim = spell.aim();
        Vec3 direction = aim.lengthSqr() < 1.0E-6 ? new Vec3(0, -1, 0) : aim.normalize();
        if (step >= LifeSpell.CRYSTAL_TRACK_TICKS) {
            // Keep the actual ray origin fixed; preview recoil must not move the damaging beam.
            prismPose[prismAim] = prismPose[prismAim + 1] = prismPose[prismAim + 2] = 0;
            float length = (float)Math.max(0, aim.length() - 10.2 / 16);
            prismPose[aimLine + 8] = length / 5;
            prismPose[laserBeam + 8] = length / 5;
        }
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), direction.toVector3f()));
        pose.translate(0, -18.0 / 16, 0); // Entity position is the crystal center, not the model's foot origin.
        prismModel.renderPose(pose, buffers, light, prismPose);
    }
    @Override public ResourceLocation getTextureLocation(LifeSpell spell) {
        return ResourceLocation.parse(spell.kind() == LifeSpell.Kind.METEOR
                ? "lyycore:textures/entity/crystal_meteor/crystal_meteor.png"
                : "lyycore:textures/entity/sovereign/" + (spell.kind() == LifeSpell.Kind.CRYSTAL ? "pink_tracking_prism" : "violet_blue_mage") + ".png");
    }
}
