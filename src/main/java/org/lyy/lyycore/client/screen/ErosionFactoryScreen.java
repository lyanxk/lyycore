package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.ErosionFactoryMenu;
import org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity;

public final class ErosionFactoryScreen extends AbstractContainerScreen<ErosionFactoryMenu> {
    public ErosionFactoryScreen(ErosionFactoryMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageHeight = 180; inventoryLabelY = 84; }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == 0);
        g.drawString(font, Component.translatable("gui.lyycore.erosion.template"), leftPos + 12, topPos + 24, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("gui.lyycore.erosion.material"), leftPos + 65, topPos + 24, ForgeGui.TEXT, false);
        ForgeGui.arrow(g, leftPos + 105, topPos + 42, ForgeGui.ACCENT);
        g.drawString(font, String.format(java.util.Locale.ROOT, "%,d IE", menu.energy()), leftPos + 8, topPos + 64, ForgeGui.TEXT, false);
        g.fill(leftPos + 8, topPos + 77, leftPos + 168, topPos + 81, ForgeGui.TRACK);
        g.fill(leftPos + 8, topPos + 77, leftPos + 8 + ForgeGui.scaled(menu.energy(), ErosionFactoryBlockEntity.CAPACITY, 160), topPos + 81, ForgeGui.ACCENT);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial); renderTooltip(g, x, y);
        if (isHovering(12, 22, 90, 12, x, y)) g.renderComponentTooltip(font, java.util.List.of(
                Component.translatable("gui.lyycore.erosion.template_hint"), Component.translatable("gui.lyycore.erosion.cost")), x, y);
    }
}
