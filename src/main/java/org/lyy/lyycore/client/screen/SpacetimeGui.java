package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Shared spacetime panel and diagram strokes, including JEI. */
public final class SpacetimeGui {
    public static final int TEXT = 0xFF435770, PINK = 0xFFE79DC7, BLUE = 0xFF85C9E4;
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("lyycore:textures/gui/spacetime_background.png");
    private SpacetimeGui() { }

    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        // Keep ornaments at 1/8 scale; only the undecorated middle strips grow.
        // The upper slice includes the side crystals as well as the corner flourishes.
        int[] sourceX = {0, 480, 968, 1448}, sourceY = {0, 560, 606, 1086};
        int[] targetX = {x, x + 60, x + width - 60, x + width};
        int[] targetY = {y, y + 70, y + height - 60, y + height};
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            g.blit(BACKGROUND, targetX[column], targetY[row],
                    targetX[column + 1] - targetX[column], targetY[row + 1] - targetY[row],
                    sourceX[column], sourceY[row], sourceX[column + 1] - sourceX[column],
                    sourceY[row + 1] - sourceY[row], 1448, 1086);
        }
    }

    public static void slot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 17, y + 17, color);
        g.fill(x, y, x + 16, y + 16, 0xFFF7FBFF);
        g.fill(x + 1, y + 1, x + 15, y + 15, 0xFFEAF2FA);
    }

    public static void line(GuiGraphics g, float ax, float ay, float bx, float by, int color, float width) {
        float length = (float) Math.hypot(bx - ax, by - ay);
        if (length == 0) return;
        float dx = -(by - ay) / length * width / 2, dy = (bx - ax) / length * width / 2;
        var vertices = g.bufferSource().getBuffer(RenderType.gui());
        var pose = g.pose().last().pose();
        vertices.addVertex(pose, ax + dx, ay + dy, 0).setColor(color);
        vertices.addVertex(pose, bx + dx, by + dy, 0).setColor(color);
        vertices.addVertex(pose, bx - dx, by - dy, 0).setColor(color);
        vertices.addVertex(pose, ax - dx, ay - dy, 0).setColor(color);
    }

    public static void ring(GuiGraphics g, int x, int y, int[][] positions, int start, int count, int color, float width) {
        for (int i = 0; i < count; i++) {
            var a = positions[start + i]; var b = positions[start + (i + 1) % count];
            line(g, x + a[0] + 8, y + a[1] + 8, x + b[0] + 8, y + b[1] + 8, color, width);
        }
    }
}
