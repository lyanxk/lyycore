package org.lyy.lyycore.content.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.CondensingFrameBlockEntity;

import java.util.ArrayList;

public final class ResourceGatheringScreen extends AbstractContainerScreen<ResourceGatheringMenu> {
    private static final int COLUMNS = 8, PAGE_SIZE = 24;
    private boolean selectorOpen;
    private int page;

    public ResourceGatheringScreen(ResourceGatheringMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = CondensingFrameMenu.WIDTH;
        imageHeight = CondensingFrameMenu.HEIGHT;
        inventoryLabelY = 92;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.lyycore.gathering.select"), button -> {
            selectorOpen = !selectorOpen;
            page = Math.max(0, menu.selectedIndex()) / PAGE_SIZE;
        }).bounds(leftPos + 119, topPos + 37, 48, 18).build());
    }
    public boolean isSelectorOpen() { return selectorOpen; }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 8, topPos + 19, leftPos + 168, topPos + 20, ForgeGui.TRACK);
        ForgeGui.frame(g, leftPos + 64, topPos + 26, menu.kind().supportsWater() && menu.isWaterlogged());
        g.fill(leftPos + 64, topPos + 27, leftPos + 69, topPos + 31, menu.kind().color());
        g.fill(leftPos + 107, topPos + 27, leftPos + 112, topPos + 31, menu.kind().color());
        for (Slot slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == 0);
        if (!menu.getSlot(0).hasItem()) {
            ItemStack selected = menu.selectedResult();
            if (!selected.isEmpty()) {
                g.renderFakeItem(selected, leftPos + 80, topPos + 39);
                g.fill(leftPos + 80, topPos + 39, leftPos + 96, topPos + 55, 0x70F5EDF3);
            }
        }
        int filled = ForgeGui.scaled(menu.getEnergyIE(), CondensingFrameBlockEntity.CAPACITY, 160);
        g.fill(leftPos + 8, topPos + 81, leftPos + 168, topPos + 86, ForgeGui.TRACK);
        if (filled > 0) g.fillGradient(leftPos + 8, topPos + 81, leftPos + 8 + filled, topPos + 86, 0xFFF2EAF2, menu.kind().color());
    }
    @Override protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, String count) {
        super.renderSlotContents(g, stack, slot, slot.index == 0 ? "" : count);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 160), 8, 6, ForgeGui.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, 92, ForgeGui.TEXT, false);
        String count = Integer.toString(menu.getOutputCount());
        g.drawString(font, count, (176 - font.width(count)) / 2, 58, 0xFF756783, false);
        String energy = String.format(java.util.Locale.ROOT, "%,d IE", menu.getEnergyIE());
        g.drawString(font, energy, (176 - font.width(energy)) / 2, 70, ForgeGui.TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (selectorOpen) { renderSelector(g, mouseX, mouseY); return; }
        if (isHovering(8, 68, 160, 19, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("tooltip.lyycore.imaginary_energy", menu.getEnergyIE(), CondensingFrameBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (isHovering(77, 35, 24, 31, mouseX, mouseY)) {
            var lines = new ArrayList<Component>();
            if (menu.getSlot(0).hasItem()) lines.add(menu.getSlot(0).getItem().getHoverName());
            else if (!menu.selectedResult().isEmpty()) lines.add(menu.selectedResult().getHoverName());
            else lines.add(Component.translatable("screen.lyycore.gathering.choose"));
            lines.add(Component.translatable("screen.lyycore.gathering.output", menu.getOutputCount(), 1024));
            if (menu.kind().supportsWater()) lines.add(Component.translatable(menu.isWaterlogged() ? "screen.lyycore.gathering.wet" : "screen.lyycore.condensing.dry"));
            if (!menu.getSlot(0).getItem().isEmpty() && !ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(), menu.selectedResult()))
                lines.add(Component.translatable("screen.lyycore.gathering.waiting"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (isHovering(8, 5, 160, 12, mouseX, mouseY)) {
            g.renderTooltip(font, title, mouseX, mouseY);
        } else renderTooltip(g, mouseX, mouseY);
    }
    private int pages() { return Math.max(1, (menu.recipes().size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    private void renderSelector(GuiGraphics g, int mouseX, int mouseY) {
        page = Math.min(page, pages() - 1);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        int x = leftPos + 8, y = topPos + 22;
        ForgeGui.panel(g, x, y, 160, 104);
        g.drawString(font, Component.translatable("screen.lyycore.gathering.choose"), x + 8, y + 7, ForgeGui.TEXT, false);
        g.drawString(font, "×", x + 145, y + 7, ForgeGui.TEXT, false);
        var recipes = menu.recipes();
        ItemStack hovered = ItemStack.EMPTY;
        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            int index = page * PAGE_SIZE + cell;
            if (index >= recipes.size()) break;
            int sx = x + 8 + cell % COLUMNS * 18, sy = y + 23 + cell / COLUMNS * 18;
            ForgeGui.slot(g, sx, sy, index == menu.selectedIndex());
            ItemStack result = recipes.get(index).value().result();
            g.renderFakeItem(result, sx, sy);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                g.fill(sx, sy, sx + 16, sy + 16, 0x40FFFFFF);
                hovered = result;
            }
        }
        g.drawString(font, "<", x + 8, y + 87, ForgeGui.TEXT, false);
        String position = (page + 1) + " / " + pages();
        g.drawString(font, position, x + (160 - font.width(position)) / 2, y + 87, ForgeGui.TEXT, false);
        g.drawString(font, ">", x + 145, y + 87, ForgeGui.TEXT, false);
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!selectorOpen) return super.mouseClicked(mouseX, mouseY, button);
        double x = mouseX - leftPos - 8, y = mouseY - topPos - 22;
        if (x < 0 || x >= 160 || y < 0 || y >= 104 || (x >= 140 && y < 20)) { selectorOpen = false; return true; }
        if (button != 0) return true;
        if (y >= 82) {
            if (x < 24) page = Math.max(0, page - 1);
            if (x >= 136) page = Math.min(pages() - 1, page + 1);
        } else if (x >= 8 && x < 152 && y >= 23 && y < 77) {
            int index = page * PAGE_SIZE + (int) ((y - 23) / 18) * COLUMNS + (int) ((x - 8) / 18);
            if (index < menu.recipes().size() && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                selectorOpen = false;
            }
        }
        return true;
    }
    @Override public boolean mouseReleased(double x, double y, int button) { return selectorOpen || super.mouseReleased(x, y, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return selectorOpen || super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (selectorOpen) { if (key == 256) selectorOpen = false; return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
