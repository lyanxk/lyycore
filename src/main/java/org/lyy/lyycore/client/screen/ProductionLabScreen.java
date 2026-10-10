package org.lyy.lyycore.client.screen;

import org.lyy.lyycore.content.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ProductionLabScreen extends AbstractContainerScreen<ProductionLabMenu> {
    public ProductionLabScreen(ProductionLabMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 196;
        inventoryLabelY = 100;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ResearchScreen.panel(g, leftPos, topPos, imageWidth, imageHeight, 0xFFF9F7F1);
        for (var slot : menu.slots) slot(g, slot.x, slot.y);
        float progress = menu.status() == 2 ? 1 : menu.progress();
        for (int step = 0; step < 96; step++) {
            double angle = step * Math.PI * 2 / 96 - Math.PI / 2;
            int x = leftPos + 88 + (int) Math.round(Math.cos(angle) * 26);
            int y = topPos + 53 + (int) Math.round(Math.sin(angle) * 26);
            g.fill(x - 1, y - 1, x + 2, y + 2, step < progress * 96 ? 0xFFB69A55 : 0xFFE4DDCD);
        }
        Component status = menu.status() == 1 ? Component.literal(Math.round(progress * 100) + "%")
                : Component.translatable("gui.lyycore.lab." + (menu.status() == 2 ? "complete" : menu.status() == 4 ? "experiment" : menu.status() == 3 ? "invalid" : "insert"));
        g.drawString(font, status, leftPos + (imageWidth - font.width(status)) / 2, topPos + 86, 0xFF68616A, false);
    }
    private void slot(GuiGraphics g, int x, int y) {
        x += leftPos; y += topPos;
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFB69A55);
        g.fill(x, y, x + 16, y + 16, 0xFFEDE7DA);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
