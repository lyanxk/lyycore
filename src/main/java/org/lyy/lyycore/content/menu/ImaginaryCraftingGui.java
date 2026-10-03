package org.lyy.lyycore.content.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lyy.lyycore.LyyCore;

/** The same nine-slot diagram is used by the machine screen and JEI. */
public final class ImaginaryCraftingGui {
    private static final ResourceLocation DIAGRAM = ResourceLocation.fromNamespaceAndPath(
            LyyCore.MODID, "textures/gui/imaginary_crafting.png");
    private static final int TEXTURE_SIZE = 1254;

    private ImaginaryCraftingGui() { }

    /** Coordinates are relative to the menu origin, including when drawn in JEI. */
    public static void diagram(GuiGraphics g, int x, int y, float progress) {
        g.blit(DIAGRAM, x + 32, y + 14, 112, 112, 0, 0,
                TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        if (progress > 0) {
            int shade = Math.round(160 + 95 * progress);
            int color = 0xFFFF0000 | shade << 8 | 0xEB;
            for (int slot = 1; slot < ImaginaryCraftingMenu.POSITIONS.length; slot++) {
                int[] position = ImaginaryCraftingMenu.POSITIONS[slot];
                int startX = x + position[0] + 8;
                int startY = y + position[1] + 8;
                int endX = x + ImaginaryCraftingMenu.POSITIONS[0][0] + 8;
                int endY = y + ImaginaryCraftingMenu.POSITIONS[0][1] + 8;
                line(g, startX, startY, endX, endY, color);
                int pulseX = Math.round(startX + (endX - startX) * progress);
                int pulseY = Math.round(startY + (endY - startY) * progress);
                g.fill(pulseX - 1, pulseY - 1, pulseX + 2, pulseY + 2, 0xFFFFFFFF);
            }
        }
        for (int slot = 0; slot < ImaginaryCraftingMenu.POSITIONS.length; slot++) {
            int[] position = ImaginaryCraftingMenu.POSITIONS[slot];
            ForgeGui.slot(g, x + position[0], y + position[1], slot == 0);
        }
    }

    private static void line(GuiGraphics g, int x, int y, int endX, int endY, int color) {
        int steps = Math.max(Math.abs(endX - x), Math.abs(endY - y));
        for (int step = 0; step <= steps; step++) {
            int px = x + (endX - x) * step / steps;
            int py = y + (endY - y) * step / steps;
            g.fill(px, py, px + 1, py + 1, color);
        }
    }
}
