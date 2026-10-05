package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;
import org.lyy.lyycore.content.blockEntities.AlloyCauldronBlockEntity;

/** Shows either the current materials or the finished potion while pointing at the pot. */
public final class AlloyCauldronRenderer implements BlockEntityRenderer<AlloyCauldronBlockEntity> {
    public AlloyCauldronRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public void render(AlloyCauldronBlockEntity pot, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pot.getBlockPos())) return;
        var finished = pot.finishedPotion();
        var displayed = finished.isEmpty()
                ? pot.ingredients().entrySet().stream().map(entry -> new ItemStack(entry.getKey(), entry.getValue())).toList()
                : java.util.List.of(finished);
        if (displayed.isEmpty()) return;
        pose.pushPose();
        pose.translate(.5, 1.5, .5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        int index = 0, size = displayed.size();
        for (var stack : displayed) {
            int column = index % 5, row = index / 5, columns = Math.min(5, size - row * 5);
            pose.pushPose();
            pose.translate((column - (columns - 1) / 2.0) * .36, (1 - row) * .4, 0);
            pose.scale(.3F, .3F, .3F);
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, light, overlay, pose, buffers, pot.getLevel(), index);
            pose.translate(0, -.57, .55);
            pose.scale(.055F, -.055F, .055F);
            String count = Integer.toString(stack.getCount());
            mc.font.drawInBatch(count, -mc.font.width(count) / 2F, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x883C304A, light);
            pose.popPose(); index++;
        }
        pose.popPose();
    }
}
