package org.lyy.lyycore.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Small controls shared by the two spacetime power screens. */
final class SpacetimeButton extends Button {
    boolean selected;
    SpacetimeButton(int x, int y, int width, Component label, OnPress action) {
        super(x, y, width, 18, label, action, DEFAULT_NARRATION);
    }
    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int border = selected ? SpacetimeGui.PINK : isHoveredOrFocused() && active ? SpacetimeGui.BLUE : 0xFFBDCDDF;
        g.fill(getX(), getY(), getX() + width, getY() + height, border);
        g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, selected ? 0xFFFFEDF8 : 0xFFF3F8FF);
        var font = Minecraft.getInstance().font;
        ForgeGui.centeredText(g, font, getMessage(), getX() + width / 2, getY() + 5, active ? SpacetimeGui.TEXT : 0xFF9DAABA);
    }
}
