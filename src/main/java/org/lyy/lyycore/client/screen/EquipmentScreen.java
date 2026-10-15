package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shared rose-and-white equipment workbench layout, using normal container slots. */
abstract class EquipmentScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected EquipmentScreen(M menu, Inventory inventory, Component title, int height, int inventoryY) {
        super(menu, inventory, title); imageWidth = 248; imageHeight = height;
        inventoryLabelX = 44; inventoryLabelY = inventoryY - 12;
    }
    protected static Component text(String key, Object... args) { return Component.translatable("gui.lyycore.accessory." + key, args); }
    protected Button button(int x, int y, int width, Component label, int action) {
        return addRenderableWidget(new Button(leftPos + x, topPos + y, width, 20, label,
                b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }, narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                int border = active ? ForgeGui.ACCENT : ForgeGui.TRACK;
                g.fill(getX(), getY(), getX() + width, getY() + height, border);
                g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, isHoveredOrFocused() && active ? 0xFFFCE3F0 : 0xFFFFFAFC);
                ForgeGui.centeredText(g, font, font.plainSubstrByWidth(getMessage().getString(), width - 8), getX() + width / 2,
                        getY() + (height - font.lineHeight) / 2, active ? ForgeGui.TEXT : 0xFF9C949C);
            }
        });
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 12, topPos + 27, leftPos + imageWidth - 12, topPos + 28, ForgeGui.ACCENT);
        g.fill(leftPos + 12, topPos + inventoryLabelY - 6, leftPos + imageWidth - 12, topPos + inventoryLabelY - 5, ForgeGui.ACCENT);
        for (var slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        ForgeGui.centeredText(g, font, title, imageWidth / 2, 11, ForgeGui.TEXT);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ForgeGui.TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) { super.render(g, mx, my, partial); renderTooltip(g, mx, my); }
}
