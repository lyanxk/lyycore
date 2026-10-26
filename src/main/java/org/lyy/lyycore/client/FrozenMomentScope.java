package org.lyy.lyycore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/** A single scene render with a circular mask and screen-pixel-width blue reticle. */
final class FrozenMomentScope {
    static void render(GuiGraphics g) {
        float x = g.guiWidth() / 2F, y = g.guiHeight() / 2F;
        float radius = Math.min(x, y) * 1.12F;
        float pixel = (float) (1 / Minecraft.getInstance().getWindow().getGuiScale());
        band(g, x, y, radius, Math.max(g.guiWidth(), g.guiHeight()) * 2F, 0xFF000000, 0xFF000000);
        band(g, x, y, radius - pixel, radius, 0x00000000, 0xFF000000);
        band(g, x, y, radius - 2 * pixel, radius - pixel, 0x804895BE, 0x804895BE);
        float halfWidth = pixel * .6F;
        rectangle(g, x - radius + 3 * pixel, y - halfWidth, x + radius - 3 * pixel, y + halfWidth, 0xE060C8FA);
        rectangle(g, x - halfWidth, y - radius + 3 * pixel, x + halfWidth, y + radius - 3 * pixel, 0xE060C8FA);
        g.flush();
    }
    private static void rectangle(GuiGraphics g, float left, float top, float right, float bottom, int color) {
        var vertices = g.bufferSource().getBuffer(RenderType.gui());
        var pose = g.pose().last().pose();
        vertices.addVertex(pose, left, bottom, 0).setColor(color);
        vertices.addVertex(pose, right, bottom, 0).setColor(color);
        vertices.addVertex(pose, right, top, 0).setColor(color);
        vertices.addVertex(pose, left, top, 0).setColor(color);
    }
    private static void band(GuiGraphics g, float x, float y, float inner, float outer, int inside, int outside) {
        var vertices = g.bufferSource().getBuffer(RenderType.gui());
        var pose = g.pose().last().pose();
        for (int i = 0; i < 256; i++) {
            double a = i * Math.PI / 128, b = (i + 1) * Math.PI / 128;
            float ax = (float) Math.cos(a), ay = (float) Math.sin(a), bx = (float) Math.cos(b), by = (float) Math.sin(b);
            vertices.addVertex(pose, x + ax * outer, y + ay * outer, 0).setColor(outside);
            vertices.addVertex(pose, x + ax * inner, y + ay * inner, 0).setColor(inside);
            vertices.addVertex(pose, x + bx * inner, y + by * inner, 0).setColor(inside);
            vertices.addVertex(pose, x + bx * outer, y + by * outer, 0).setColor(outside);
        }
    }
}
