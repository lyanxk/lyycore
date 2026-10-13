package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.CaptureCageMenu;

public final class CaptureCageScreen extends AbstractContainerScreen<CaptureCageMenu> {
    public CaptureCageScreen(CaptureCageMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth = 176; imageHeight = 208; }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        PureSmeltingScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            g.fill(leftPos+slot.x-1, topPos+slot.y-1, leftPos+slot.x+17, topPos+slot.y+17, 0xFFE9B4CB);
            g.fill(leftPos+slot.x, topPos+slot.y, leftPos+slot.x+16, topPos+slot.y+16, 0xFFF8EDF2);
        }
        g.fill(leftPos+8, topPos+94, leftPos+168, topPos+98, 0xFFF5DEEA);
        g.fill(leftPos+8, topPos+94, leftPos+8+(int)(160L*menu.energy()/Integer.MAX_VALUE), topPos+98, 0xFFE698BC);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font, title, (176-font.width(title))/2, 8, 0x593F50, false);
        g.drawString(font, font.plainSubstrByWidth(menu.captive.getString(), 160), 8, 18, 0x765D6D, false);
        String energy = String.format(java.util.Locale.ROOT, "%,d IE", menu.energy());
        g.drawString(font, energy, (176-font.width(energy))/2, 103, 0x765D6D, false);
        g.drawString(font, playerInventoryTitle, 8, 115, 0x593F50, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g,x,y,partial); renderTooltip(g,x,y);
        if (isHovering(8,94,160,4,x,y)) g.renderTooltip(font, Component.literal(String.format(java.util.Locale.ROOT, "%,d / %,d IE", menu.energy(), Integer.MAX_VALUE)),x,y);
    }
}
