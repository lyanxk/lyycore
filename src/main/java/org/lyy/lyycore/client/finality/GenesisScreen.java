package org.lyy.lyycore.client.finality;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lyy.lyycore.content.finality.*;

public final class GenesisScreen extends AbstractContainerScreen<GenesisMenu> {

    public GenesisScreen(GenesisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 204;
        inventoryLabelY = 108;
    }

    public void acceptGhost(ItemStack stack) {
        PacketDistributor.sendToServer(
                new FinalityNetwork.Ghost(menu.containerId, stack.copyWithCount(1)));
    }

    public net.minecraft.client.renderer.Rect2i ghostArea() {
        return new net.minecraft.client.renderer.Rect2i(leftPos + 133, topPos + 37, 18, 18);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        FinalityGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots)
            FinalityGui.slot(
                    g,
                    leftPos + slot.x,
                    topPos + slot.y,
                    slot.index == 1 ? FinalityGui.PINK : FinalityGui.PURPLE);
        int x0 = leftPos + 18, y0 = topPos + 85, width = 140;
        g.fill(x0 - 1, y0 - 1, x0 + width + 1, y0 + 7, FinalityGui.GOLD);
        g.fill(x0, y0, x0 + width, y0 + 6, 0xFFE8DFED);
        int filled = (int) (width * (long) menu.energy() / GenesisBlockEntity.CAPACITY);
        g.fill(x0, y0, x0 + filled, y0 + 6, FinalityGui.PURPLE);
        g.fill(x0, y0, x0 + filled, y0 + 2, FinalityGui.PINK);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font, playerInventoryTitle, 8, 108, FinalityGui.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial);
        renderTooltip(g, x, y);
    }
}
