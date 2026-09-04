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
        g.drawString(this.font, "IE: " + compact(e) + " / " + compact(c), leftPos + 8, topPos + 58, 0xF0A1CE, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
        if (isHovering(10, 56, 156, 15, mouseX, mouseY)) {
            g.renderTooltip(this.font, Component.literal("IE: " + menu.getEnergy() + " / " + menu.getCapacity()),
                    mouseX, mouseY);
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
