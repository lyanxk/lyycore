package org.lyy.lyycore.client.screen;

import org.lyy.lyycore.content.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class EnergyCellScreen extends AbstractContainerScreen<EnergyCellMenu> {
    private static final int BAR_X = 8, BAR_Y = 79, BAR_WIDTH = 160, BAR_HEIGHT = 8;

    public EnergyCellScreen(EnergyCellMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = EnergyCellMenu.WIDTH;
        imageHeight = EnergyCellMenu.HEIGHT;
        inventoryLabelY = EnergyCellMenu.INVENTORY_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 8, topPos + 19, leftPos + imageWidth - 8, topPos + 20, ForgeGui.TRACK);
        for (Slot slot : menu.slots) {
            ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == 0);
        }
        int x = leftPos + BAR_X, y = topPos + BAR_Y;
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, ForgeGui.TRACK);
        int filled = ForgeGui.scaled(menu.getEnergy(), menu.getCapacity(), BAR_WIDTH);
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + BAR_HEIGHT, 0xFFF6C9E4, ForgeGui.ACCENT);
            g.fill(x, y, x + filled, y + 1, 0xFFE4F7FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, ForgeGui.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.energy_cell.charging"), 8, 27, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.energy_cell.compatible"), 42, 42, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.energy_cell.automatic"), 42, 54, 0xFF7C8095, false);
        g.drawString(font, Component.translatable("screen.lyycore.energy_cell.stored"), 8, 68, ForgeGui.TEXT, false);
        String amount = compact(menu.getEnergy()) + " / " + compact(menu.getCapacity()) + " IE";
        g.drawString(font, amount, imageWidth - 8 - font.width(amount), 68, ForgeGui.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(BAR_X, 67, BAR_WIDTH, 20, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("tooltip.lyycore.imaginary_energy",
                    menu.getEnergy(), menu.getCapacity()), mouseX, mouseY);
        } else if (hoveredSlot != null && hoveredSlot.index == 0 && !hoveredSlot.hasItem()) {
            g.renderTooltip(font, Component.translatable("tooltip.lyycore.energy_cell.charge_slot"), mouseX, mouseY);
        }
    }

    private static String compact(int value) {
        if (value >= 1_000_000_000) return compact(value, 1_000_000_000, "G");
        if (value >= 1_000_000) return compact(value, 1_000_000, "M");
        if (value >= 1_000) return compact(value, 1_000, "k");
        return Integer.toString(value);
    }

    private static String compact(int value, int unit, String suffix) {
        long hundredths = (long) value * 100 / unit;
        long decimals = hundredths % 100;
        return hundredths / 100 + "." + (decimals < 10 ? "0" : "") + decimals + suffix;
    }
}
