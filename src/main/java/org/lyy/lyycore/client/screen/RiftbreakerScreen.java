package org.lyy.lyycore.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.ToolMining;
import org.lyy.lyycore.content.item.RiftbreakerItem;
import org.lyy.lyycore.content.menu.RiftbreakerMenu;

import java.util.function.BooleanSupplier;

public final class RiftbreakerScreen extends AbstractContainerScreen<RiftbreakerMenu> {
    private static final int INK = 0xFF35465D, MUTED = 0xFF73849B, BLUE = 0xFF8EC7E8, TRACK = 0xFFE0E9F1;
    private static final Style TEXT_STYLE = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);
    private EfficiencySlider efficiency;
    private AbstractButton enchantment;

    public RiftbreakerScreen(RiftbreakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title.copy().withStyle(TEXT_STYLE));
        imageWidth = 276;
        imageHeight = 238;
    }
    private static Component text(String key) { return Component.translatable(key).withStyle(TEXT_STYLE); }

    @Override protected void init() {
        super.init();
        efficiency = addRenderableWidget(new EfficiencySlider());
        for (var pattern : ToolMining.Pattern.values()) {
            int mode = pattern.ordinal();
            addChoice(leftPos + 20 + mode * 80, topPos + 128, 76, RiftbreakerItem.modeName(pattern).copy().withStyle(TEXT_STYLE),
                    () -> menu.mode() == pattern, RiftbreakerMenu.MODE_BUTTON + mode);
        }
        for (int radius = 1; radius <= 5; radius++) {
            int value = radius, side = radius * 2 + 1;
            addChoice(leftPos + 20 + (radius - 1) * 48, topPos + 174, 44,
                    Component.literal(side + "×" + side).withStyle(TEXT_STYLE),
                    () -> menu.mode() == ToolMining.Pattern.AREA && menu.radius() == value, RiftbreakerMenu.RADIUS_BUTTON + value);
        }
        enchantment = addChoice(leftPos + 20, topPos + 208, 236, enchantmentLabel(), () -> false, RiftbreakerMenu.TOGGLE_ENCHANTMENT);
    }

    private AbstractButton addChoice(int x, int y, int width, Component label, BooleanSupplier selected, int button) {
        return addRenderableWidget(new AbstractButton(x, y, width, 22, label) {
            @Override public void onPress() { send(button); }
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                ResearchScreen.panel(g, getX(), getY(), width, height,
                        selected.getAsBoolean() ? 0xFFD6ECFA : isHoveredOrFocused() ? 0xFFEDF5FD : 0xFFF0F3F8);
                g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + 7, INK, false);
            }
            @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
                defaultButtonNarrationText(output);
            }
        });
    }
    private Component enchantmentLabel() { return text("gui.lyycore.reaper." + (menu.silkTouch() ? "silk_touch" : "fortune")); }
    private void send(int button) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (!efficiency.isFocused()) efficiency.sync(menu.efficiency());
        enchantment.setMessage(enchantmentLabel());
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        SpacetimeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 13, topPos + 47, leftPos + 263, topPos + 48, BLUE);
        g.renderItem(menu.tool(), leftPos + 21, topPos + 18);
        g.drawString(font, title, leftPos + 48, topPos + 14, INK, false);
        g.drawString(font, RiftbreakerItem.modeName(menu.mode()).copy().withStyle(TEXT_STYLE), leftPos + 48, topPos + 30, MUTED, false);
        g.drawString(font, text("gui.lyycore.reaper.efficiency"), leftPos + 20, topPos + 60, INK, false);
        g.drawString(font, text("gui.lyycore.riftbreaker.mode"), leftPos + 20, topPos + 114, INK, false);
        int side = menu.radius() * 2 + 1;
        g.drawString(font, Component.translatable("gui.lyycore.riftbreaker.area", side, side).withStyle(TEXT_STYLE),
                leftPos + 20, topPos + 160, INK, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDragging() && getFocused() instanceof EfficiencySlider slider)
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private final class EfficiencySlider extends AbstractSliderButton {
        private int lastSent;
        private boolean dragging;
        private EfficiencySlider() {
            super(leftPos + 20, topPos + 75, 236, 18, Component.empty(),
                    (menu.efficiency() - RiftbreakerItem.MIN_EFFICIENCY) / 120.0);
            lastSent = menu.efficiency();
            updateMessage();
        }
        private int setting() { return Mth.clamp((int) Math.round(8 + value * 120), 8, 128); }
        private void sync(int setting) { value = (setting - 8) / 120.0; lastSent = setting; updateMessage(); }
        @Override protected void updateMessage() {
            setMessage(Component.translatable("gui.lyycore.reaper.value", text("gui.lyycore.reaper.efficiency"), setting()).withStyle(TEXT_STYLE));
        }
        @Override protected void applyValue() {
            if (!dragging && setting() != lastSent) { send(setting()); lastSent = setting(); }
        }
        @Override public void onClick(double x, double y) { dragging = true; super.onClick(x, y); }
        @Override public void onRelease(double x, double y) { dragging = false; applyValue(); super.onRelease(x, y); }
        @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY(), thumb = x + 3 + (int) Math.round(value * (width - 8));
            g.fill(x, y + 7, x + width, y + 12, TRACK);
            g.fill(x, y + 7, thumb, y + 12, BLUE);
            g.fill(thumb - 3, y + 3, thumb + 4, y + 16, INK);
            g.fill(thumb - 2, y + 4, thumb + 3, y + 15, 0xFFF9FCFF);
            var number = Component.literal(Integer.toString(setting())).withStyle(TEXT_STYLE);
            g.drawString(font, number, x + width - font.width(number), y - 15, INK, false);
            g.drawString(font, Component.literal("8").withStyle(TEXT_STYLE), x, y + 20, MUTED, false);
            g.drawString(font, Component.literal("128").withStyle(TEXT_STYLE), x + width - font.width("128"), y + 20, MUTED, false);
        }
    }
}
