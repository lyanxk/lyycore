package org.lyy.lyycore.client.screen;

import org.lyy.lyycore.content.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class IAFScreen extends AbstractContainerScreen<IAFMenu> {
    private static final int ENERGY_X = 12, ENERGY_Y = 28, ENERGY_W = 10, ENERGY_H = 40;
    private static final int PROGRESS_X = 38, PROGRESS_Y = 77, PROGRESS_W = 110, PROGRESS_H = 10;
    // JEI owns the arrow tooltip. Progress has its own hover area below it.
    public static final int RECIPE_X = 96, RECIPE_Y = 42, RECIPE_W = 23, RECIPE_H = 12;

    public IAFScreen(IAFMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = IAFMenu.WIDTH;
        this.imageHeight = IAFMenu.HEIGHT;
        this.inventoryLabelY = IAFMenu.INVENTORY_Y - 12;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("tooltip.lyycore.energy",
                    menu.getEnergy(), menu.getMaxEnergy()), mouseX, mouseY);
        } else if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            Component tooltip = menu.getMaxProgress() <= 0
                    ? Component.translatable("screen.lyycore.forge.idle")
                    : Component.translatable("tooltip.lyycore.progress", menu.getProgress(), menu.getMaxProgress());
            g.renderTooltip(font, tooltip, mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 4) {
            String role = switch (hoveredSlot.index) {
                case IAFMenu.CATALYST -> "catalyst";
                case IAFMenu.OUTPUT -> "output";
                default -> "input";
            };
            g.renderTooltip(font, Component.translatable("tooltip.lyycore.forge." + role), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 8, topPos + 19, leftPos + imageWidth - 8, topPos + 20, ForgeGui.TRACK);
        for (Slot slot : menu.slots) {
            ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == IAFMenu.CATALYST);
        }
        ForgeGui.arrow(g, leftPos + RECIPE_X, topPos + RECIPE_Y, ForgeGui.ACCENT);

        int x = leftPos + ENERGY_X, y = topPos + ENERGY_Y;
        g.fill(x - 1, y - 1, x + ENERGY_W + 1, y + ENERGY_H + 1, ForgeGui.TEXT);
        g.fill(x, y, x + ENERGY_W, y + ENERGY_H, ForgeGui.TRACK);
        int filled = ForgeGui.scaled(menu.getEnergy(), menu.getMaxEnergy(), ENERGY_H);
        g.fill(x, y + ENERGY_H - filled, x + ENERGY_W, y + ENERGY_H, ForgeGui.ACCENT);

        x = leftPos + PROGRESS_X;
        y = topPos + PROGRESS_Y;
        g.fill(x, y, x + PROGRESS_W, y + PROGRESS_H, ForgeGui.TRACK);
        int progress = ForgeGui.scaled(menu.getProgress(), menu.getMaxProgress(), PROGRESS_W);
        g.fill(x, y, x + progress, y + PROGRESS_H, 0xFFE59ACB);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, ForgeGui.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ForgeGui.TEXT, false);
        Component status = menu.getMaxProgress() <= 0
                ? Component.translatable("screen.lyycore.forge.idle")
                : Component.literal(ForgeGui.scaled(menu.getProgress(), menu.getMaxProgress(), 100) + "%");
        g.drawString(font, status, PROGRESS_X + (PROGRESS_W - font.width(status)) / 2,
                PROGRESS_Y + 1, ForgeGui.TEXT, false);
    }
}
