package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.blocks.SummoningBeaconBlock.Controller;

public final class SummoningBeaconRenderer implements BlockEntityRenderer<Controller> {
    @Override public void render(Controller beacon, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (beacon.getLevel() == null) return;
        pose.pushPose();
        pose.translate(0, 58.0 / 16, 0);
        BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partial, 1,
                beacon.getLevel().getGameTime(), 0, BeaconRenderer.MAX_RENDER_Y, 0xFFF5A6D1, .2F, .25F);
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(Controller beacon) { return true; }
    @Override public int getViewDistance() { return 256; }
    @Override public boolean shouldRender(Controller beacon, Vec3 camera) {
        return Vec3.atCenterOf(beacon.getBlockPos()).multiply(1, 0, 1).closerThan(camera.multiply(1, 0, 1), getViewDistance());
    }
    @Override public AABB getRenderBoundingBox(Controller beacon) {
        var pos = beacon.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 4 + BeaconRenderer.MAX_RENDER_Y, pos.getZ() + 1);
    }
}
