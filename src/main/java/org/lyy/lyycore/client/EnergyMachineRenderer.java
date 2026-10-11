package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.mojang.math.Axis;

/** Only moving bones are drawn here; stationary surfaces use the chunk mesh. */
public final class EnergyMachineRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final AnimatedMeshModel mesh;
    private final String clip;
    public EnergyMachineRenderer(String model, String clip) {
        this.mesh = new AnimatedMeshModel(model);
        this.clip = clip;
    }
    @Override public AABB getRenderBoundingBox(T machine) {
        int height = ((SquareMachineBlock) machine.getBlockState().getBlock()).height();
        return new AABB(machine.getBlockPos()).inflate(((SquareMachineBlock)machine.getBlockState().getBlock()).width() / 2, 0, ((SquareMachineBlock)machine.getBlockState().getBlock()).width() / 2).expandTowards(0, height - 1, 0);
    }
    @Override public void render(T machine, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (machine.getLevel() == null) return;
        float seconds = (float)((machine.getLevel().getGameTime() / 20.0) % mesh.clipLength(clip)) + partial / 20;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        if (machine.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            pose.mulPose(Axis.YP.rotationDegrees(180 - machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        mesh.render(pose, buffers, light, clip, seconds);
        pose.popPose();
    }
}
