package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;

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
        return new AABB(machine.getBlockPos()).inflate(1, 0, 1).expandTowards(0, height - 1, 0);
    }
    @Override public void render(T machine, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (machine.getLevel() == null) return;
        float seconds = (machine.getLevel().getGameTime() % 480 + partial) / 20;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        mesh.render(pose, buffers, light, clip, seconds);
        pose.popPose();
    }
}
