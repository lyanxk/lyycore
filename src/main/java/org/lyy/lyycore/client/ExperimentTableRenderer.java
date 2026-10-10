package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.blockEntities.ExperimentTableBlockEntity;
import org.lyy.lyycore.content.blocks.ExperimentTableBlock;

public final class ExperimentTableRenderer implements BlockEntityRenderer<ExperimentTableBlockEntity> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("experiment_table");
    public ExperimentTableRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public AABB getRenderBoundingBox(ExperimentTableBlockEntity table) { return new AABB(table.getBlockPos()).inflate(.15); }
    @Override public void render(ExperimentTableBlockEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (table.getLevel() == null) return;
        pose.pushPose(); pose.translate(.5, 0, .5);
        float angle = switch (table.getBlockState().getValue(ExperimentTableBlock.FACING)) {
            case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0;
        };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        model.render(pose, buffers, light, "idle", (table.getLevel().getGameTime() % 80 + partial) / 20);
        pose.popPose();
    }
}
