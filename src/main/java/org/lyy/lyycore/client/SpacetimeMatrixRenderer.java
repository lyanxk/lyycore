package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.blockEntities.SpacetimeMatrixBlockEntity;

/** The common foundation is baked into chunks; each stage keeps its authored idle rings. */
public final class SpacetimeMatrixRenderer implements BlockEntityRenderer<SpacetimeMatrixBlockEntity> {
    private final AnimatedMeshModel[] stages = new AnimatedMeshModel[3];
    // Model units, from the three blue_lavender_star meshes. No stage rescales the foundation.
    private static final float[] CORE_HEIGHT = {25, 34.103F, 42.505F};
    private static final float[] CORE_RADIUS = {8, 15, 22.95F};

    public SpacetimeMatrixRenderer(BlockEntityRendererProvider.Context context) {
        StellarCoreMesh.prepareTexture();
        for (int stage = 0; stage < stages.length; stage++)
            stages[stage] = new AnimatedMeshModel("spacetime_matrix_tier_" + (stage + 1));
    }

    @Override public AABB getRenderBoundingBox(SpacetimeMatrixBlockEntity matrix) {
        // Include the full swept ring volume of stage three, even outside the foundation.
        var pos = matrix.getBlockPos();
        return new AABB(pos.getX() - 2.5, pos.getY(), pos.getZ() - 2.5,
                pos.getX() + 3.5, pos.getY() + 5.7, pos.getZ() + 3.5);
    }

    @Override public void render(SpacetimeMatrixBlockEntity matrix, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (matrix.getLevel() == null) return;
        int stage = Mth.clamp(matrix.stage(), 1, 3) - 1;
        // All stellar cycles divide 1,200 seconds; integer tick modulo avoids long-world float jitter.
        double seconds = (matrix.getLevel().getGameTime() % 24000L + partial) / 20.0;
        pose.pushPose();
        pose.translate(.5, 0, .5);
        stages[stage].render(pose, buffers, light, "idle", seconds);
        pose.translate(0, CORE_HEIGHT[stage] / 16, 0);
        float radius = CORE_RADIUS[stage] / 16;
        pose.scale(radius, radius, radius);
        StellarCoreMesh.render(pose, buffers, seconds, stage);
        pose.popPose();
    }
}
