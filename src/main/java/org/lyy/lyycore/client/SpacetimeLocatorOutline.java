package org.lyy.lyycore.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Short-lived corner markers; never iterates the blocks inside a selected area. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class SpacetimeLocatorOutline {
    private static ClientLevel level;
    private static final Map<BlockPos, Long> POINTS = new LinkedHashMap<>();
    private static void refresh() {
        if (level != Minecraft.getInstance().level) { level = Minecraft.getInstance().level; POINTS.clear(); }
    }
    public static void show(GlobalPos position) {
        refresh();
        if (level != null && level.dimension().equals(position.dimension())) POINTS.put(position.pos(), level.getGameTime() + 20);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        refresh();
        if (level == null || POINTS.isEmpty()) return;
        POINTS.values().removeIf(until -> until <= level.getGameTime());
        var pose = event.getPoseStack(); var camera = event.getCamera().getPosition();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        for (var point : POINTS.keySet()) LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(point).inflate(.003), .25f, .7f, 1, 1);
        pose.popPose(); buffers.endBatch(RenderType.lines());
    }
    private SpacetimeLocatorOutline() { }
}
