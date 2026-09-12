package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.entity.SonnetDome;

public class SonnetDomeRenderer extends EntityRenderer<SonnetDome> {
    public SonnetDomeRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(SonnetDome entity) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
    @Override public boolean shouldRender(SonnetDome entity, Frustum frustum, double x, double y, double z) {
        return entity.isOpen() && entity.shouldRender(x, y, z)
                && frustum.isVisible(entity.getBoundingBox().inflate(2));
    }

    public static final net.minecraft.client.resources.model.ModelResourceLocation[] MODELS =
            java.util.stream.Stream.of("dome_shell", "dome_fractures", "dome_crown", "dome_trails")
                    .map(name -> net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath("lyycore", "crystal_sanctuary/" + name)))
                    .toArray(net.minecraft.client.resources.model.ModelResourceLocation[]::new);

    @Override public void render(SonnetDome dome, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!dome.isOpen()) return;
        pose.pushPose();
        float scale = (float) (SonnetDome.RADIUS / 12.0);
        pose.scale(scale, scale, scale);
        // Preserve the opaque, depth-writing shell while avoiding per-frame OBJ
        // traversal, vertex transforms and uploads for nearly 29,000 faces.
        SonnetDomeMesh.render(pose);
        pose.popPose();
        super.render(dome, yaw, partial, pose, buffers, light);
    }
    static void quad(VertexConsumer consumer, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int alpha) {
        for (Vec3 vertex : new Vec3[]{a, b, c, d})
            consumer.addVertex(pose.last().pose(), (float) vertex.x, (float) vertex.y, (float) vertex.z).setColor(255, 130, 208, alpha);
    }
    static void beam(VertexConsumer consumer, PoseStack pose, Vec3 start, Vec3 end, float width, int alpha) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(width);
        Vec3 up = direction.cross(side).normalize().scale(width);
        quad(consumer, pose, start.add(side), end.add(side), end.subtract(side), start.subtract(side), alpha);
        quad(consumer, pose, start.add(up), end.add(up), end.subtract(up), start.subtract(up), alpha);
    }
}
