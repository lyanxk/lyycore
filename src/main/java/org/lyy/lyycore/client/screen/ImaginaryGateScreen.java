package org.lyy.lyycore.client.screen;

import org.lyy.lyycore.content.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class ImaginaryGateScreen extends AbstractContainerScreen<ImaginaryGateMenu> {
    public ImaginaryGateScreen(ImaginaryGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 92;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 57, topPos + 24, leftPos + 119, topPos + 68, 0xFFB98BA9);
        g.fill(leftPos + 60, topPos + 27, leftPos + 116, topPos + 65, 0xFFFAF2FA);
        for (Slot slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == 0);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component status = Component.translatable("screen.lyycore.gate.status." + menu.status());
        g.drawString(font, status, (imageWidth - font.width(status)) / 2, 76, ForgeGui.TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
