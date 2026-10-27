package org.lyy.lyycore.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import org.lyy.lyycore.network.PowerChannelNetwork;

/** Shared static block snapshot preview for the coordinate and logistics tools. */
final class SpacetimeBlockPreview {
    static void render(GuiGraphics g, int left, int top, int width, int height, PowerChannelNetwork.Preview preview) {
        // Only baked block models are rendered: no level replacement, ticking, entities or BE renderers.
        g.flush();
        g.enableScissor(left, top, left + width, top + height);
        var pose = g.pose(); pose.pushPose();
        try {
            Lighting.setupFor3DItems();
            pose.translate(left + width / 2.0, top + height / 2.0, 150);
            pose.scale(7, -7, 7); pose.mulPose(Axis.XP.rotationDegrees(25)); pose.mulPose(Axis.YP.rotationDegrees(135));
            for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
                var state = Block.stateById(preview.states().get(y * 25 + z * 5 + x));
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                pose.pushPose(); pose.translate(x - 2.5, y - 2.5, z - 2.5);
                net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, g.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            g.flush();
            // The target is the central block. Keep its marker visible over surrounding models.
            pose.pushPose();
            pose.last().pose().translateLocal(0, 0, 100);
            LevelRenderer.renderLineBox(pose, g.bufferSource().getBuffer(RenderType.lines()),
                    -.503, -.503, -.503, .503, .503, .503, .25f, .7f, 1, 1);
            g.flush();
            pose.popPose();
        } finally {
            pose.popPose(); Lighting.setupFor3DItems(); g.disableScissor();
        }
    }
    private SpacetimeBlockPreview() { }
}
