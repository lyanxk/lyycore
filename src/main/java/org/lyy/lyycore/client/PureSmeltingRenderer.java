package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.*;
import org.lyy.lyycore.content.blocks.LargeStructureBlock;
import org.lyy.lyycore.content.blockEntities.PureSmeltingPlantBlockEntity;

public final class PureSmeltingRenderer implements BlockEntityRenderer<PureSmeltingPlantBlockEntity> {
    private final AnimatedMeshModel model = new AnimatedMeshModel("pure_smelting_plant");
    public PureSmeltingRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public AABB getRenderBoundingBox(PureSmeltingPlantBlockEntity be) { return new AABB(be.getBlockPos()).inflate(2).expandTowards(0, 4, 0); }
    @Override public void render(PureSmeltingPlantBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!be.active() || be.getLevel() == null) return;
        var mc = Minecraft.getInstance();
        pose.pushPose(); pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-be.getBlockState().getValue(LargeStructureBlock.FACING).toYRot()));
        model.render(pose, buffers, light, "working", be.working() ? (be.getLevel().getGameTime()%80+partial)/20 : 0);
        pose.popPose();
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;
        var state = be.getLevel().getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof org.lyy.lyycore.content.blocks.PureSmeltingPlantBlock)
                || !LargeStructureBlock.center(hit.getBlockPos(), state).equals(be.getBlockPos())) return;
        pose.pushPose(); pose.translate(0.5, 3.6, 0.5); pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation()); pose.scale(0.02F, -0.02F, 0.02F);
        for (int slot = 0; slot < 10; slot++) {
            var stack = be.automation.getStackInSlot(slot); if (stack.isEmpty()) continue;
            float x = (slot%5-2)*24, y = slot/5*26;
            pose.pushPose(); pose.translate(x, y, 0); pose.scale(16, -16, 16);
            mc.getItemRenderer().renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, overlay, pose, buffers, be.getLevel(), slot);
            pose.popPose();
            mc.font.drawInBatch(String.valueOf(stack.getCount()), x+3, y+5, 0xFFFFFFFF, true, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }
}
