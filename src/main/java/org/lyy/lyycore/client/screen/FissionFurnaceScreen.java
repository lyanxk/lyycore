package org.lyy.lyycore.client.screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.FissionFurnaceMenu;
public final class FissionFurnaceScreen extends AbstractContainerScreen<FissionFurnaceMenu> {
    public FissionFurnaceScreen(FissionFurnaceMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth = 176; imageHeight = 180; }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        PureSmeltingScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            g.fill(leftPos+slot.x-1, topPos+slot.y-1, leftPos+slot.x+17, topPos+slot.y+17, 0xFFE9B4CB);
            g.fill(leftPos+slot.x, topPos+slot.y, leftPos+slot.x+16, topPos+slot.y+16, 0xFFF8EDF2);
        }
        ForgeGui.arrow(g, leftPos+70, topPos+43, 0xFFE698BC);
        g.fill(leftPos+16, topPos+72, leftPos+160, topPos+76, 0xFFF5DEEA);
        g.fill(leftPos+16, topPos+72, leftPos+16+(int)(144L*menu.energy()/1_000_000_000), topPos+76, 0xFFE698BC);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font, title, (176-font.width(title))/2, 11, 0x593F50, false);
        var energy = Component.literal(String.format(java.util.Locale.ROOT, "%,d / 1 GIE", menu.energy()));
        g.drawString(font, energy, (176-font.width(energy))/2, 82, 0x765D6D, false);
        g.drawString(font, "×2", 72, 32, 0xB75A89, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) { super.render(g, x, y, partial); renderTooltip(g, x, y); }
}
