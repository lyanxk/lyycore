package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.lyy.lyycore.LyyCore;

/** Render a fixed twelve-edge outline without walking the model's detailed voxel shape. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class BlockSelectionOutline {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderHighlightEvent.Block event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var pos = event.getTarget().getBlockPos();
        var state = level.getBlockState(pos);
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(LyyCore.MODID)) return;

        event.setCanceled(true);
        if (state.isAir() || !level.getWorldBorder().isWithinBounds(pos)) return;
        var camera = event.getCamera().getPosition();
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;
        LevelRenderer.renderLineBox(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                x, y, z, x + 1, y + 1, z + 1, 0, 0, 0, 0.4F);
    }
}
