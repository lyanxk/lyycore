package org.lyy.lyycore.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity.Page;
import org.lyy.lyycore.content.menu.SpacetimeFactoryMenu;

/** Shared spacetime panel and recipe artwork, including JEI. */
public final class SpacetimeGui {
    public static final int TEXT = 0xFF435770, PINK = 0xFFE79DC7, BLUE = 0xFF85C9E4;
    private static final ResourceLocation BACKGROUND = ResourceLocation.parse("lyycore:textures/gui/spacetime_background.png");
    private static final ResourceLocation DIAGRAM = ResourceLocation.parse("lyycore:textures/gui/spacetime_diagram.png");
    private static final int[][] DIAGRAM_SLOTS = SpacetimeFactoryMenu.positions(Page.SPACETIME);
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

    public static int diagramSlotX(int slot) {
        return DIAGRAM_SLOTS[slot][0] - DIAGRAM_SLOTS[16][0] - 8;
    }

    public static int diagramSlotY(int slot) {
        return DIAGRAM_SLOTS[slot][1] - DIAGRAM_SLOTS[16][1] - 8;
    }

    /** Slot offsets and artwork share the same center in the machine and JEI. */
    public static void diagram(GuiGraphics g, int centerX, int centerY, int outerColor, int innerColor) {
        // Plain texture blits do not enable blending; preserve the artwork's soft alpha edges.
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(DIAGRAM, centerX - 86, centerY - 86, 172, 172, 0, 0, 1254, 1254, 1254, 1254);
        RenderSystem.disableBlend();
        for (int i = 0; i < DIAGRAM_SLOTS.length; i++) {
            slot(g, centerX + diagramSlotX(i), centerY + diagramSlotY(i),
                    i < 8 || i == 16 ? outerColor : innerColor);
        }
    }
}
