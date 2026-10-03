package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.GrappleHook;

import java.util.List;

public final class GrappleRenderer extends EntityRenderer<GrappleHook> {
    public static final ModelResourceLocation EMPTY_HELD_MODEL = model("grapple/empty_held");
    public static final List<ModelResourceLocation> MODELS = List.of(model("grapple/body"), model("grapple/claw_1"),
            model("grapple/claw_2"), model("grapple/claw_3"), model("grapple/claw_4"), model("imaginary_chain_link"));
    // Contact portion of the supplied 1.2-second ground_grab animation.
    private static final float[] TIMES = {0.26F, 0.34F, 0.5F, 0.68F, 0.82F, 1.2F};
    private static final float[] CLAW_ANGLES = {-8, -3, 12, 9, 11, 11};
    private static final float[] ROOT_HEIGHTS = {15.25F, 15.7F, 15.42F, 15.65F, 15.55F, 15.55F};

    public GrappleRenderer(EntityRendererProvider.Context context) { super(context); }
    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "item/" + path));
    }
    @Override public boolean shouldRender(GrappleHook hook, Frustum frustum, double x, double y, double z) {
        // The chain may cross the screen while the small hook itself is off-screen.
        return true;
    }

    @Override public void render(GrappleHook hook, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (hook.isChainBroken()) return;
        Vec3 forward = hook.anchorEntity() != null && hook.getOwner() != null
                ? hook.position().subtract(hook.getOwner().getEyePosition()).normalize()
                : hook.attached() ? Vec3.atLowerCornerOf(hook.anchorFace().getNormal()).scale(-1)
                : hook.getDeltaMovement().normalize();
        if (forward.lengthSqr() < 0.01) forward = new Vec3(0, 1, 0);
        float age = hook.attachedAge(partial) / 20 + TIMES[0];
        float height = (hook.attached() ? sample(ROOT_HEIGHTS, age) : 15.55F) / 16;
        Vec3 root = forward.scale(-height);
        pose.pushPose();
        pose.translate(root.x, root.y, root.z);
        orient(pose, forward);
        draw(MODELS.getFirst(), pose, buffers, light, false);
        float angle = hook.attached() ? sample(CLAW_ANGLES, age) : -5;
        for (int claw = 0; claw < 4; claw++) {
            double x = claw == 0 ? 2.6 / 16 : claw == 2 ? -2.6 / 16 : 0;
            double z = claw == 1 ? 2.6 / 16 : claw == 3 ? -2.6 / 16 : 0;
            pose.pushPose();
            pose.translate(x, 5.0 / 16, z);
            pose.mulPose((claw % 2 == 0 ? Axis.ZP : Axis.XP).rotationDegrees(claw < 2 ? angle : -angle));
            pose.translate(-x, -5.0 / 16, -z);
            draw(MODELS.get(claw + 1), pose, buffers, light, false);
            pose.popPose();
        }
        pose.popPose();
        if (hook.getOwner() != null) {
            Vec3 end = hook.getOwner().getEyePosition(partial).add(0, -0.4, 0).subtract(hook.getPosition(partial));
            Vec3 start = root.subtract(forward.scale(1.5 / 16));
            Vec3 chain = end.subtract(start);
            int links = Math.min(256, (int) Math.ceil(chain.length() / (3.15 / 16)));
            for (int i = 0; i < links; i++) {
                Vec3 point = start.add(chain.scale((i + 0.5) / links));
                pose.pushPose();
                pose.translate(point.x, point.y, point.z);
                orient(pose, chain.normalize());
                pose.mulPose(Axis.YP.rotationDegrees(i % 2 * 90));
                draw(MODELS.get(5), pose, buffers, light, true);
                pose.popPose();
            }
        }
        super.render(hook, yaw, partial, pose, buffers, light);
    }
    private static void orient(PoseStack pose, Vec3 direction) {
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, (float) direction.x, (float) direction.y, (float) direction.z));
    }
    private static float sample(float[] values, float time) {
        for (int i = 1; i < TIMES.length; i++)
            if (time < TIMES[i]) return Mth.lerp(Mth.clamp((time - TIMES[i - 1]) / (TIMES[i] - TIMES[i - 1]), 0, 1), values[i - 1], values[i]);
        return values[values.length - 1];
    }
    private static void draw(ModelResourceLocation model, PoseStack pose, MultiBufferSource buffers, int light, boolean centered) {
        pose.pushPose();
        pose.translate(-0.5, centered ? -0.5 : 0, -0.5);
        var minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderModelLists(minecraft.getModelManager().getModel(model), ItemStack.EMPTY, light,
                OverlayTexture.NO_OVERLAY, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)));
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(GrappleHook hook) { return TextureAtlas.LOCATION_BLOCKS; }
}
