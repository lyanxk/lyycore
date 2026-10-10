package org.lyy.lyycore.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.lyy.lyycore.content.blockEntities.SummoningPedestalBlockEntity;
public final class SummoningPedestalRenderer<T extends SummoningPedestalBlockEntity> implements BlockEntityRenderer<T> {
    @Override public void render(T pedestal, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var stack = pedestal.items.getStackInSlot(0); if (stack.isEmpty() || pedestal.getLevel() == null) return;
        pose.pushPose(); pose.translate(.5, 1.25, .5); pose.scale(.45f, .45f, .45f);
        pose.mulPose(Axis.YP.rotationDegrees((pedestal.getLevel().getGameTime() + partial) * 2));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
