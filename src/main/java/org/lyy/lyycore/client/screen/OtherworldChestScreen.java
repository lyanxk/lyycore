package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.menu.OtherworldChestMenu;
import org.lyy.lyycore.content.blockEntities.OtherworldChestBlockEntity;

public final class OtherworldChestScreen extends AbstractContainerScreen<OtherworldChestMenu> {
    private boolean selecting;
    private int page;
    public OtherworldChestScreen(OtherworldChestMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageHeight = 190; inventoryLabelY = 94; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.lyycore.gathering.select"), b -> { selecting = !selecting; page = Math.max(0, menu.selectedIndex()) / 24; })
                .bounds(leftPos + 118, topPos + 39, 48, 18).build());
    }
    private void send(int id) { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, false);
        ForgeGui.frame(g, leftPos + 64, topPos + 29, false);
        ForgeGui.slot(g, leftPos + 80, topPos + 39, true);
        g.renderFakeItem(menu.selected(), leftPos + 80, topPos + 39);
        String balance = String.format(java.util.Locale.ROOT, "%,d IE", menu.energy());
        g.drawString(font, balance, leftPos + (176 - font.width(balance)) / 2, topPos + 73, ForgeGui.TEXT, false);
        g.fill(leftPos + 8, topPos + 85, leftPos + 168, topPos + 89, ForgeGui.TRACK);
        g.fill(leftPos + 8, topPos + 85, leftPos + 8 + ForgeGui.scaled(menu.energy(), OtherworldChestBlockEntity.CAPACITY, 160), topPos + 89, ForgeGui.ACCENT);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial);
        if (!selecting) {
            renderTooltip(g, x, y);
            if (isHovering(78, 37, 20, 20, x, y)) g.renderComponentTooltip(font, java.util.List.of(
                    menu.selected().isEmpty() ? Component.translatable("screen.lyycore.gathering.choose") : menu.selected().getHoverName(),
                    Component.translatable("gui.lyycore.otherworld.take"), Component.translatable("gui.lyycore.otherworld.cost")), x, y);
            return;
        }
        page = Math.min(page, pages() - 1);
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        int sx = leftPos + 8, sy = topPos + 22;
        ForgeGui.panel(g, sx, sy, 160, 100);
        g.drawString(font, Component.translatable("screen.lyycore.gathering.choose"), sx + 7, sy + 7, ForgeGui.TEXT, false);
        g.drawString(font, "×", sx + 145, sy + 7, ForgeGui.TEXT, false);
        ItemStack hovered = ItemStack.EMPTY;
        for (int cell = 0; cell < 24 && page * 24 + cell < menu.products().size(); cell++) {
            int px = sx + 8 + cell % 8 * 18, py = sy + 23 + cell / 8 * 18;
            var item = menu.products().get(page * 24 + cell);
            ForgeGui.slot(g, px, py, page * 24 + cell == menu.selectedIndex()); g.renderFakeItem(item, px, py);
            if (x >= px && x < px + 16 && y >= py && y < py + 16) hovered = item;
        }
        g.drawString(font, "<", sx + 8, sy + 85, ForgeGui.TEXT, false);
        g.drawString(font, (page + 1) + " / " + pages(), sx + 65, sy + 85, ForgeGui.TEXT, false);
        g.drawString(font, ">", sx + 145, sy + 85, ForgeGui.TEXT, false);
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, x, y);
        g.pose().popPose();
    }
    private int pages() { return Math.max(1, (menu.products().size() + 23) / 24); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (selecting) {
            double px = x - leftPos - 8, py = y - topPos - 22;
            if (px < 0 || px >= 160 || py < 0 || py >= 100 || px >= 140 && py < 20) selecting = false;
            else if (button == 0 && py >= 80) { if (px < 24) page = Math.max(0, page - 1); if (px >= 136) page = Math.min(pages() - 1, page + 1); }
            else if (button == 0 && px >= 8 && px < 152 && py >= 23 && py < 77) {
                int index = page * 24 + (int)((py - 23) / 18) * 8 + (int)((px - 8) / 18);
                if (index < menu.products().size()) { send(index); selecting = false; }
            }
            return true;
        }
        if (button == 0 && isHovering(78, 37, 20, 20, x, y)) { send(hasShiftDown() ? OtherworldChestMenu.TAKE_STACK : OtherworldChestMenu.TAKE_ONE); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { return selecting || super.mouseReleased(x, y, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return selecting || super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (selecting) { if (key == 256) selecting = false; return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
