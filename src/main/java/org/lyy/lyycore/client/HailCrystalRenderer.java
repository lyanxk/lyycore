package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.entity.HailCrystal;

public final class HailCrystalRenderer extends EntityRenderer<HailCrystal> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("hail_projectile");
    public HailCrystalRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(HailCrystal crystal, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(new AABB(crystal.start(), crystal.end()).inflate(1));
    }
    @Override public void render(HailCrystal crystal, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = crystal.age(partial);
        if (age > HailCrystal.FLIGHT_TICKS) return;
        var direction = crystal.end().subtract(crystal.start());
        var offset = direction.scale(Mth.clamp(age / HailCrystal.FLIGHT_TICKS, 0, 1));
        pose.pushPose(); pose.translate(offset.x, offset.y, offset.z);
        if (direction.lengthSqr() > 1e-8) pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), direction.normalize().toVector3f()));
        model.render(pose, buffers, light, "flight", age / 20);
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(HailCrystal crystal) { return ResourceLocation.parse("lyycore:textures/entity/hail/hail_projectile.png"); }
}
