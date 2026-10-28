package org.lyy.lyycore.client.finality;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class FinalityGui {
    public static final int TEXT = 0xFF665568,
            GOLD = 0xFFD3B98A,
            PURPLE = 0xFFBBA4C9,
            PINK = 0xFFE6ADCF,
            PAPER = 0xFFFFF9FA,
            MUTED = 0xFFAA9FAC;
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.parse("lyycore:textures/gui/finality_background.png");
    private static final int[] SOURCE_X = {0, 288, 620, 828, 1160, 1448};
    private static final int[] SOURCE_Y = {0, 256, 416, 640, 830, 1086};

    private FinalityGui() {}

    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        // Preserve ornaments at 1/8 scale; extend only the undecorated strips.
        int[] tx = {x, x + 36, x + width / 2 - 13, x + width / 2 + 13, x + width - 36, x + width};
        int[] ty = {
            y, y + 32, y + height / 2 - 14, y + height / 2 + 14, y + height - 32, y + height
        };
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int row = 0; row < 5; row++)
            for (int col = 0; col < 5; col++)
                g.blit(
                        BACKGROUND,
                        tx[col],
                        ty[row],
                        tx[col + 1] - tx[col],
                        ty[row + 1] - ty[row],
                        SOURCE_X[col],
                        SOURCE_Y[row],
                        SOURCE_X[col + 1] - SOURCE_X[col],
                        SOURCE_Y[row + 1] - SOURCE_Y[row],
                        1448,
                        1086);
        g.flush();
        RenderSystem.disableBlend();
    }

    public static void slot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 17, y + 17, color);
        g.fill(x, y, x + 16, y + 16, PAPER);
        g.fill(x + 1, y + 1, x + 15, y + 15, 0xFFF1EAF3);
    }

    public static class StyledButton extends Button {
        public boolean selected;

        public StyledButton(int x, int y, int width, Component text, OnPress action) {
            super(x, y, width, 20, text, action, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY();
            int border =
                    !active ? 0xFFDCD3DC : selected ? PINK : isHoveredOrFocused() ? GOLD : PURPLE;
            g.fill(x, y, x + width, y + height, border);
            g.fill(
                    x + 1,
                    y + 1,
                    x + width - 1,
                    y + height - 1,
                    active && (selected || isHoveredOrFocused()) ? 0xFFF6EAF3 : PAPER);
            var font = Minecraft.getInstance().font;
            g.drawString(
                    font,
                    getMessage(),
                    x + (width - font.width(getMessage())) / 2,
                    y + (height - font.lineHeight) / 2,
                    active ? TEXT : MUTED,
                    false);
        }
    }
}
