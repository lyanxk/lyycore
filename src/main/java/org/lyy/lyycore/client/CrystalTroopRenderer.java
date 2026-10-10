package org.lyy.lyycore.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.entity.CrystalTroop;
public final class CrystalTroopRenderer extends EntityRenderer<CrystalTroop> {
    private final AnimatedMeshModel model;
    public CrystalTroopRenderer(EntityRendererProvider.Context context, boolean assault) { super(context); model = new AnimatedMeshModel(assault ? "assault_crystal" : "recon_crystal"); }
    @Override public boolean shouldRender(CrystalTroop e, Frustum f, double x, double y, double z) {
        var bounds = e.getBoundingBox();
        if (!e.assault() && e.attackAge() >= 0 && e.attackAge() < 6) bounds = bounds.minmax(new net.minecraft.world.phys.AABB(e.position(), e.beamEnd()));
        return f.isVisible(bounds.inflate(1));
    }
    @Override public void render(CrystalTroop e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.attackAge() + partial;
        boolean attack = age >= 0 && age < (e.assault() ? 14 : 12.8f);
        boolean moving = e.charging() || e.position().distanceToSqr(new Vec3(e.xOld, e.yOld, e.zOld)) > .0001;
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        model.render(pose, buffers, light, attack ? "attack" : moving ? "move" : "idle", (attack ? age : e.tickCount + partial) / 20);
        pose.popPose();
        if (!e.assault() && age >= 0 && age < 6) {
            Vec3 start = new Vec3(0, e.getBbHeight() / 2, 0), end = e.beamEnd().subtract(e.getPosition(partial));
            SonnetDomeRenderer.beam(buffers.getBuffer(RenderType.lightning()), pose, start, end, .05f, (int)(230 * (1-age/6)));
        }
        super.render(e, yaw, partial, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(CrystalTroop e) { return ResourceLocation.parse("lyycore:textures/entity/crystal_troops/crystal_troops.png"); }
}
