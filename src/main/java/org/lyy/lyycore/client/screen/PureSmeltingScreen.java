package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.PureSmeltingMenu;

public final class PureSmeltingScreen extends AbstractContainerScreen<PureSmeltingMenu> {
    private boolean initializedSpeed;
    private SpeedSlider slider;
    public PureSmeltingScreen(PureSmeltingMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth = 224; imageHeight = 130; }
    @Override protected void init() {
        super.init();
        initializedSpeed = false;
        slider = addRenderableWidget(new SpeedSlider());
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (!initializedSpeed && menu.speed() > 0) { slider.restore(menu.speed()); initializedSpeed = true; }
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        // Container screens handle slot dragging themselves instead of forwarding it to widgets.
        if (button == 0 && isDragging() && getFocused() == slider)
            return slider.mouseDragged(x, y, button, dx, dy);
        return super.mouseDragged(x, y, button, dx, dy);
    }
    private final class SpeedSlider extends AbstractSliderButton {
        SpeedSlider() { super(leftPos + 16, topPos + 39, 192, 20, Component.literal("1×"), 0); }
        void restore(int speed) { value = (speed-1)/99.0; updateMessage(); }
        @Override protected void updateMessage() { setMessage(Component.literal((1 + Math.round(value*99)) + "×")); }
        @Override protected void applyValue() {
            initializedSpeed = true;
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1 + (int)Math.round(value*99));
        }
    }
    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x+w, y+h, 0xFFF3B5CF); g.fill(x+2, y+2, x+w-2, y+h-2, 0xFFFFFBFD);
        g.fill(x+10, y+27, x+w-10, y+28, 0xFFF3B5CF);
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) { panel(g, leftPos, topPos, imageWidth, imageHeight); }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawCenteredString(font, title, imageWidth/2, 11, 0x593F50);
        g.drawString(font, Component.translatable("gui.lyycore.smelting.power", String.format(java.util.Locale.ROOT, "%,d", 10_000L*menu.speed()*menu.speed())), 16, 73, 0x593F50, false);
        g.drawString(font, Component.translatable("gui.lyycore.smelting.energy", String.format(java.util.Locale.ROOT, "%,d", menu.energy())), 16, 93, 0x593F50, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) { super.render(g, x, y, partial); renderTooltip(g, x, y); }
}
