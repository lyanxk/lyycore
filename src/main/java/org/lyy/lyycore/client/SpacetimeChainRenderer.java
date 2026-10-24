package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lyy.lyycore.network.SpacetimeChainPayload;

import java.util.List;

/** A lightly sagging blue tether follows both interpolated endpoints, just like a leash. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class SpacetimeChainRenderer {
    private static ClientLevel level;
    private static List<SpacetimeChainPayload.Link> links = List.of();
    private SpacetimeChainRenderer() { }
    private static void refresh() {
        if (level != Minecraft.getInstance().level) { level = Minecraft.getInstance().level; links = List.of(); }
    }
    public static void update(SpacetimeChainPayload payload) {
        refresh();
        if (level != null && level.dimension().location().equals(payload.dimension())) links = List.copyOf(payload.links());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        refresh();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || level == null || links.isEmpty()) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (var link : links) {
            if (!(level.getEntity(link.player()) instanceof Player player)
                    || !(level.getEntity(link.target()) instanceof LivingEntity target) || !target.isAlive()) continue;
            Vec3 start = player.getPosition(partial).add(0, player.getBbHeight() * .6, 0);
            Vec3 end = target.getPosition(partial).add(0, target.getBbHeight() * .6, 0);
            double sag = Math.min(1.5, start.distanceTo(end) * .06);
            for (int segment = 0; segment < 24; segment++) {
                double a = segment / 24.0, b = (segment + 1) / 24.0;
                Vec3 from = start.lerp(end, a).add(0, -4 * sag * a * (1 - a), 0);
                Vec3 to = start.lerp(end, b).add(0, -4 * sag * b * (1 - b), 0);
                Vec3 side = to.subtract(from).cross(camera.subtract(from)).normalize().scale(.018);
                for (Vec3 point : new Vec3[]{from.add(side), to.add(side), to.subtract(side), from.subtract(side)})
                    vertices.addVertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z).setColor(76, 180, 255, 235);
            }
        }
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }
}
