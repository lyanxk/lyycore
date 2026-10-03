package org.lyy.lyycore.content.menu;

import net.minecraft.client.gui.GuiGraphics;

/** Shared silver-and-rose drawing primitives for machine screens and JEI. */
public final class ForgeGui {
    public static final int TEXT = 0xFF50546A;
    public static final int ACCENT = 0xFFE9A2C7;
    public static final int TRACK = 0xFFD9DCE8;

    private ForgeGui() { }

    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF8B91A9);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE9DBE8);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFFF5EDF3);
        g.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFFF9FCFF);
        g.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFFC4CBDC);
    }

    /** x and y are the item coordinates, not the surrounding border. */
    public static void slot(GuiGraphics g, int x, int y, boolean catalyst) {
        g.fill(x - 1, y - 1, x + 17, y + 17, catalyst ? 0xFFC084AE : 0xFF929AAF);
        g.fill(x, y, x + 17, y + 17, 0xFFF9FCFF);
        g.fill(x, y, x + 16, y + 16, catalyst ? 0xFFECD2E3 : 0xFFDCE0EB);
    }

    public static void arrow(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y + 4, x + 17, y + 8, color);
        for (int i = 0; i < 6; i++) {
            g.fill(x + 17 + i, y + i, x + 18 + i, y + 12 - i, color);
        }
    }

    /** Pixel art framework shared with JEI: white rails, rose joints and water. */
    public static void frame(GuiGraphics g, int x, int y, boolean wet) {
        g.fill(x, y + 3, x + 48, y + 34, 0xFFDED2E5);
        g.fill(x + 2, y + 5, x + 46, y + 32, 0xFFFAF6FC);
        if (wet) {
            g.fill(x + 3, y + 22, x + 45, y + 31, 0xFFCEE8F4);
            g.fill(x + 3, y + 22, x + 45, y + 23, 0xFF99CADF);
        }
        g.fill(x, y + 3, x + 4, y + 34, 0xFFC1B9D1);
        g.fill(x + 44, y + 3, x + 48, y + 34, 0xFFC1B9D1);
        g.fill(x + 1, y + 3, x + 3, y + 32, 0xFFFFFFFF);
        g.fill(x + 45, y + 3, x + 47, y + 32, 0xFFFFFFFF);
        for (int dx : new int[]{0, 43}) {
            g.fill(x + dx, y + 1, x + dx + 5, y + 5, ForgeGui.ACCENT);
            g.fill(x + dx, y + 32, x + dx + 5, y + 36, ForgeGui.ACCENT);
        }
        g.fill(x + 7, y + 17, x + 12, y + 18, 0xFFD3B1D5);
        g.fill(x + 9, y + 15, x + 10, y + 20, 0xFFD3B1D5);
        g.fill(x + 35, y + 13, x + 40, y + 14, 0xFFD3B1D5);
        g.fill(x + 37, y + 11, x + 38, y + 16, 0xFFD3B1D5);
    }

    public static int scaled(int value, int maximum, int size) {
        if (maximum <= 0) return 0;
        return (int) ((long) Math.clamp(value, 0, maximum) * size / maximum);
    }
}
