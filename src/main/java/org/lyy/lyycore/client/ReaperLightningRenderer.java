package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class ReaperLightningRenderer {
    private static final int LIFETIME = 8;
    private record Flash(long born, List<Vec3> vertices) { }
    private static final List<Flash> FLASHES = new ArrayList<>();
    private static ClientLevel level;

    private ReaperLightningRenderer() { }

    private static void refreshLevel() {
        if (level != Minecraft.getInstance().level) {
            FLASHES.clear();
            level = Minecraft.getInstance().level;
        }
    }

    public static void add(List<Vec3> points) {
        refreshLevel();
        if (level == null) return;
        var vertices = new ArrayList<Vec3>();
        var random = RandomSource.create();
        vertices.add(points.getFirst());
        for (int link = 1; link < points.size(); link++) {
            Vec3 start = points.get(link - 1), end = points.get(link);
            int segments = Math.max(2, (int) Math.ceil(start.distanceTo(end) * 5));
            for (int i = 1; i < segments; i++) {
                vertices.add(start.lerp(end, (double) i / segments).add(
                        (random.nextDouble() - 0.5) * 0.22, (random.nextDouble() - 0.5) * 0.22, (random.nextDouble() - 0.5) * 0.22));
            }
            vertices.add(end);
        }
        if (FLASHES.size() >= 64) FLASHES.removeFirst();
        FLASHES.add(new Flash(level.getGameTime(), vertices));
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        refreshLevel();
        if (level != null) FLASHES.removeIf(flash -> level.getGameTime() - flash.born >= LIFETIME);
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FLASHES.isEmpty() || level == null) return;
        var pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderType.lightning());
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Flash flash : FLASHES) {
            float fade = Math.max(0, 1 - (level.getGameTime() - flash.born + partial) / LIFETIME);
            for (int i = 1; i < flash.vertices.size(); i++) {
                Vec3 a = flash.vertices.get(i - 1), b = flash.vertices.get(i);
                beam(vertices, pose, a, b, 0.055F, 230, 145, 255, (int) (110 * fade));
                beam(vertices, pose, a, b, 0.016F, 255, 245, 255, (int) (255 * fade));
            }
        }
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }

    private static void beam(VertexConsumer vertices, PoseStack pose, Vec3 a, Vec3 b, float width, int r, int g, int bColor, int alpha) {
        Vec3 direction = b.subtract(a).normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(width);
        Vec3 up = direction.cross(side).normalize().scale(width);
        for (Vec3 offset : new Vec3[]{side, up}) {
            for (Vec3 point : new Vec3[]{a.add(offset), b.add(offset), b.subtract(offset), a.subtract(offset)}) {
                vertices.addVertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z).setColor(r, g, bColor, alpha);
            }
        }
    }
}
