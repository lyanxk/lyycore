package org.lyy.lyycore.client.screen;

import org.lyy.lyycore.content.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.CondensingFrameBlockEntity;

public class CrystalCondensingScreen extends AbstractContainerScreen<CrystalCondensingMenu> {
    // Keep JEI's click target on the left rail, outside the interactive output slot.
    public static final int RECIPE_X = 61, RECIPE_Y = 26, RECIPE_W = 15, RECIPE_H = 32;

    public CrystalCondensingScreen(CrystalCondensingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = CondensingFrameMenu.WIDTH;
        imageHeight = CondensingFrameMenu.HEIGHT;
        inventoryLabelY = 92;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 8, topPos + 19, leftPos + 168, topPos + 20, ForgeGui.TRACK);
        ForgeGui.frame(g, leftPos + 64, topPos + 26, menu.isWaterlogged());
        for (Slot slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == 0);
        int filled = ForgeGui.scaled(menu.getEnergyIE(), CondensingFrameBlockEntity.CAPACITY, 160);
        g.fill(leftPos + 8, topPos + 81, leftPos + 168, topPos + 86, ForgeGui.TRACK);
        if (filled > 0) g.fillGradient(leftPos + 8, topPos + 81, leftPos + 8 + filled, topPos + 86, 0xFFFFD9ED, ForgeGui.ACCENT);
    }
    @Override protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, String countString) {
        super.renderSlotContents(g, stack, slot, slot.index == 0 ? "" : countString);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 160), 8, 6, ForgeGui.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, 92, ForgeGui.TEXT, false);
        String amount = String.format(java.util.Locale.ROOT, "%,d IE", menu.getEnergyIE());
        g.drawString(font, amount, (imageWidth - font.width(amount)) / 2, 70, ForgeGui.TEXT, false);
        String count = Integer.toString(menu.getOutputCount());
        g.drawString(font, count, (imageWidth - font.width(count)) / 2, 58, 0xFF956083, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (isHovering(8, 68, 160, 19, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("tooltip.lyycore.imaginary_energy", menu.getEnergyIE(), CondensingFrameBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (isHovering(77, 35, 24, 31, mouseX, mouseY)) {
            // Keep output details beside the slot; the left rail belongs to JEI.
            var lines = new java.util.ArrayList<Component>();
            if (menu.getSlot(0).hasItem()) lines.add(menu.getSlot(0).getItem().getHoverName());
            lines.add(Component.translatable("screen.lyycore.condensing.output", menu.getOutputCount(), CondensingFrameBlockEntity.OUTPUT_CAPACITY));
            lines.add(Component.translatable(menu.isMiniatureFactory() ? "screen.lyycore.factory.production" : menu.isWaterlogged() ? "screen.lyycore.condensing.wet" : "screen.lyycore.condensing.dry"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (isHovering(8, 5, 160, 12, mouseX, mouseY)) {
            g.renderTooltip(font, title, mouseX, mouseY);
        } else renderTooltip(g, mouseX, mouseY);
    }
}
