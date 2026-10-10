package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import org.lyy.lyycore.content.entity.HailFlower;

public final class HailFlowerRenderer extends EntityRenderer<HailFlower> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("hail_flower_large");
    public HailFlowerRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public boolean shouldRender(HailFlower flower, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(flower.getBoundingBox().inflate(4));
    }
    @Override public void render(HailFlower flower, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = flower.age(partial);
        // The supplied explode clip bursts 0.18 seconds in: line its burst up with the damage at tick 20.
        float explosion = (age - 20) / 20 + .18F;
        model.render(pose, buffers, light, explosion >= 0 ? "explode" : "idle", explosion >= 0 ? explosion : age / 20);
    }
    @Override public ResourceLocation getTextureLocation(HailFlower flower) { return ResourceLocation.parse("lyycore:textures/entity/hail/hail_flower_large.png"); }
}
