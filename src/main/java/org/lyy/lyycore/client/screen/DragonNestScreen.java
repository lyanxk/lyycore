package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.DragonNestMenu;

public final class DragonNestScreen extends AbstractContainerScreen<DragonNestMenu> {
    public DragonNestScreen(DragonNestMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth = 176; imageHeight = 190; }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        PureSmeltingScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            g.fill(leftPos+slot.x-1, topPos+slot.y-1, leftPos+slot.x+17, topPos+slot.y+17, 0xFFE9B4CB);
            g.fill(leftPos+slot.x, topPos+slot.y, leftPos+slot.x+16, topPos+slot.y+16, 0xFFF8EDF2);
        }
        g.fill(leftPos+54, topPos+71, leftPos+122, topPos+74, 0xFFF5DEEA);
        g.fill(leftPos+54, topPos+71, leftPos+54+68*menu.progress()/6000, topPos+74, 0xFFE698BC);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawCenteredString(font, title, 88, 11, 0x593F50);
        g.drawCenteredString(font, Component.translatable("gui.lyycore.dragon." + new String[]{"patrol", "breath", "stowed"}[menu.mode()]), 88, 81, 0x593F50);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) { super.render(g, x, y, partial); renderTooltip(g, x, y); }
}
