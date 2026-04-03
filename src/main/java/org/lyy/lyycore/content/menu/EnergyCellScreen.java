package org.lyy.lyycore.content.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.LyyCore;

public class EnergyCellScreen extends AbstractContainerScreen<EnergyCellMenu> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "textures/gui/energy_cell.png");

    public EnergyCellScreen(EnergyCellMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        g.blit(TEX, x, y, 0, 0, this.imageWidth, this.imageHeight, 176, 166);
        int e = menu.getEnergy();
        int c = menu.getCapacity();
        g.drawString(this.font, "IE: " + e + " / " + c, leftPos + 8, topPos + 58, 0x66CCFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
