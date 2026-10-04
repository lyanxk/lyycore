package org.lyy.lyycore.content.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.item.ImaginaryReaperItem;

public final class ImaginaryReaperScreen extends AbstractContainerScreen<ImaginaryReaperMenu> {
    private static final int INK = 0xFF423646, MUTED = 0xFF817483, GOLD = 0xFFB69A55;
    private static final int PAPER = 0xFFF9F7F1, PINK = 0xFFE5A6CD, TRACK = 0xFFECE1E8;
    private static final Style TEXT_STYLE = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);
    private SettingSlider efficiency, damage;
    private AbstractButton enchantment;

    public ImaginaryReaperScreen(ImaginaryReaperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title.copy().withStyle(TEXT_STYLE));
        imageWidth = 276;
        imageHeight = 204;
    }

    private static Component text(String key) {
        return Component.translatable("gui.lyycore.reaper." + key).withStyle(TEXT_STYLE);
    }

    @Override protected void init() {
        super.init();
        efficiency = addRenderableWidget(new SettingSlider(topPos + 75, ImaginaryReaperItem.MIN_EFFICIENCY,
                ImaginaryReaperItem.MAX_EFFICIENCY, menu.efficiency(), ImaginaryReaperMenu.EFFICIENCY_BUTTON, "efficiency"));
        damage = addRenderableWidget(new SettingSlider(topPos + 127, ImaginaryReaperItem.MIN_DAMAGE,
                ImaginaryReaperItem.MAX_DAMAGE, menu.damage(), ImaginaryReaperMenu.DAMAGE_BUTTON, "damage"));
        enchantment = addRenderableWidget(new AbstractButton(leftPos + 20, topPos + 167, 236, 24, enchantmentLabel()) {
            @Override public void onPress() { send(ImaginaryReaperMenu.TOGGLE_ENCHANTMENT); }
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                ResearchScreen.panel(g, getX(), getY(), width, height, isHoveredOrFocused() ? 0xFFF8E5F0 : 0xFFF3EDF1);
                g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + 8, INK, false);
            }
            @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
                defaultButtonNarrationText(output);
            }
        });
    }

    private Component enchantmentLabel() { return text(menu.silkTouch() ? "silk_touch" : "fortune"); }
    private void send(int button) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (!efficiency.isFocused()) efficiency.sync(menu.efficiency());
        if (!damage.isFocused()) damage.sync(menu.damage());
        enchantment.setMessage(enchantmentLabel());
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ResearchScreen.panel(g, leftPos, topPos, imageWidth, imageHeight, PAPER);
        g.fill(leftPos + 13, topPos + 47, leftPos + 263, topPos + 48, GOLD);
        g.renderItem(menu.tool(), leftPos + 21, topPos + 18);
        g.drawString(font, title, leftPos + 48, topPos + 14, INK, false);
        var mode = ImaginaryReaperItem.modeName(menu.tool()).copy().withStyle(TEXT_STYLE);
        g.drawString(font, mode, leftPos + 48, topPos + 30, MUTED, false);
        g.drawString(font, text("efficiency"), leftPos + 20, topPos + 60, INK, false);
        g.drawString(font, text("damage"), leftPos + 20, topPos + 112, INK, false);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Container screens handle inventory dragging themselves; forward slider drags explicitly.
        if (button == 0 && isDragging() && getFocused() instanceof SettingSlider slider)
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private final class SettingSlider extends AbstractSliderButton {
        private final int min, max, button;
        private final String label;
        private int lastSent;
        private boolean dragging;

        private SettingSlider(int y, int min, int max, int initial, int button, String label) {
            super(leftPos + 20, y, 236, 18, Component.empty(), (double) (initial - min) / (max - min));
            this.min = min;
            this.max = max;
            this.button = button;
            this.label = label;
            lastSent = initial;
            updateMessage();
        }

        private int setting() { return Mth.clamp((int) Math.round(min + value * (max - min)), min, max); }
        private void sync(int setting) {
            value = (double) (setting - min) / (max - min);
            lastSent = setting;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.translatable("gui.lyycore.reaper.value", text(label), setting()).withStyle(TEXT_STYLE));
        }

        @Override protected void applyValue() {
            if (dragging) return;
            int setting = setting();
            if (setting != lastSent) {
                send(button + setting);
                lastSent = setting;
            }
        }

        @Override public void onClick(double mouseX, double mouseY) {
            dragging = true;
            super.onClick(mouseX, mouseY);
        }

        @Override public void onRelease(double mouseX, double mouseY) {
            dragging = false;
            applyValue();
            super.onRelease(mouseX, mouseY);
        }

        @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY();
            int thumb = x + 3 + (int) Math.round(value * (width - 8));
            g.fill(x, y + 7, x + width, y + 12, TRACK);
            g.fill(x, y + 7, thumb, y + 12, PINK);
            g.fill(thumb - 3, y + 3, thumb + 4, y + 16, isHoveredOrFocused() ? GOLD : INK);
            g.fill(thumb - 2, y + 4, thumb + 3, y + 15, 0xFFFFF9FC);
            Component number = Component.literal(Integer.toString(setting())).withStyle(TEXT_STYLE);
            g.drawString(font, number, x + width - font.width(number), y - 15, INK, false);
            g.drawString(font, Component.literal(Integer.toString(min)).withStyle(TEXT_STYLE), x, y + 20, MUTED, false);
            Component upper = Component.literal(Integer.toString(max)).withStyle(TEXT_STYLE);
            g.drawString(font, upper, x + width - font.width(upper), y + 20, MUTED, false);
        }
    }
}
