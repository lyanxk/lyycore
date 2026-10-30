package org.lyy.lyycore.client.finality;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;

import org.lyy.lyycore.client.finality.FinalityGui.StyledButton;
import org.lyy.lyycore.content.finality.*;

public final class FinalitySettingsScreen extends AbstractContainerScreen<FinalitySettingsMenu> {
    private int page;
    private String[] options = {};
    private SpeedSlider speedSlider;
    private EquipmentSlot displayedSlot;

    public FinalitySettingsScreen(FinalitySettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 182;
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        displayedSlot = menu.slot;
        speedSlider = null;
        if (menu.item.getItem() instanceof FinalityArmorItem armor) {
            options =
                    switch (armor.getEquipmentSlot()) {
                        case HEAD -> new String[] {"exhaustion", "healing", "debuff_decay", "night_vision"};
                        case CHEST -> new String[] {"cancel_damage", "flight", "prevent_teleport"};
                        case LEGS -> new String[] {"retaliation"};
                        default -> new String[] {"speed", "step"};
                    };
            for (int i = 0; i < options.length; i++) {
                final int option = i;
                addRenderableWidget(
                        new StyledButton(
                                leftPos + 18,
                                topPos + 38 + i * 26,
                                220,
                                label(options[i], menu.enabled(i)),
                                b -> send(option)));
            }
            if (armor.getEquipmentSlot() == EquipmentSlot.FEET)
                speedSlider = addRenderableWidget(new SpeedSlider());
            for (int i = 0; i < FinalitySettingsMenu.ARMOR_SLOTS.size(); i++)
                addRenderableWidget(new EquipmentButton(i));
        } else {
            for (int i = 0; i < 4; i++) {
                final int row = i;
                addRenderableWidget(
                        new StyledButton(
                                leftPos + 18,
                                topPos + 34 + i * 25,
                                220,
                                Component.empty(),
                                b -> {
                                    int index = page * 4 + row;
                                    if (index < menu.channels.size()) send(index);
                                }));
            }
            addRenderableWidget(
                    new StyledButton(
                            leftPos + 18,
                            topPos + 144,
                            24,
                            Component.literal("<"),
                            b -> page = Math.max(0, page - 1)));
            addRenderableWidget(
                    new StyledButton(
                            leftPos + 84,
                            topPos + 144,
                            88,
                            Component.translatable("gui.lyycore.finality.unbind"),
                            b -> send(-1)));
            addRenderableWidget(
                    new StyledButton(
                            leftPos + 214,
                            topPos + 144,
                            24,
                            Component.literal(">"),
                            b ->
                                    page =
                                            Math.min(
                                                    Math.max(0, (menu.channels.size() - 1) / 4),
                                                    page + 1)));
        }
        updateButtons();
    }

    private Component label(String key, boolean on) {
        return Component.translatable("gui.lyycore.finality." + key)
                .append(": ")
                .append(Component.translatable(on ? "options.on" : "options.off"));
    }

    private void updateButtons() {
        if (menu.item.getItem() instanceof FinalityArmorItem) {
            for (int i = 0; i < options.length; i++) {
                var button = (StyledButton) children().get(i);
                button.setMessage(label(options[i], menu.enabled(i)));
                button.selected = menu.enabled(i);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                var button = (StyledButton) children().get(i);
                int index = page * 4 + i;
                button.active = index < menu.channels.size();
                button.selected = button.active && index == menu.selectedChannel();
                button.setMessage(
                        button.active
                                ? Component.literal(
                                        font.plainSubstrByWidth(
                                                menu.channels.get(index).name(), 200))
                                : Component.literal("—"));
            }
            ((StyledButton) children().get(4)).active = page > 0;
            ((StyledButton) children().get(6)).active = (page + 1) * 4 < menu.channels.size();
        }
    }

    @Override
    protected void containerTick() {
        if (displayedSlot != menu.slot) {
            setDragging(false);
            rebuildWidgets();
        }
        updateButtons();
        if (speedSlider != null && !speedSlider.isFocused()) speedSlider.sync();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        FinalityGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
        if (menu.item.getItem() instanceof FinalityArmorItem) {
            var name = menu.item.getHoverName();
            g.drawString(
                    font, name, (imageWidth - font.width(name)) / 2, 18, FinalityGui.TEXT, false);
        }
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && isDragging() && getFocused() instanceof SpeedSlider slider)
            return slider.mouseDragged(x, y, button, dx, dy);
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && speedSlider != null && speedSlider.dragging) {
            setDragging(false);
            return speedSlider.mouseReleased(x, y, button);
        }
        return super.mouseReleased(x, y, button);
    }

    private final class EquipmentButton extends StyledButton {
        private final EquipmentSlot slot;

        private EquipmentButton(int index) {
            super(leftPos - 26, topPos + 38 + index * 26, 22, Component.empty(),
                    b -> send(FinalitySettingsMenu.SELECT_ARMOR + index));
            slot = FinalitySettingsMenu.ARMOR_SLOTS.get(index);
            active = FinalitySettingsMenu.canConfigure(minecraft.player, slot);
            selected = menu.slot == slot;
            if (active) setTooltip(Tooltip.create(minecraft.player.getItemBySlot(slot).getHoverName()));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            active = FinalitySettingsMenu.canConfigure(minecraft.player, slot);
            super.renderWidget(g, mouseX, mouseY, partial);
            if (active) g.renderItem(minecraft.player.getItemBySlot(slot), getX() + 3, getY() + 2);
        }
    }

    private final class SpeedSlider extends AbstractSliderButton {
        private boolean dragging;
        private int lastSent;

        private SpeedSlider() {
            super(leftPos + 18, topPos + 108, 220, 20, Component.empty(), 0);
            sync();
        }

        private int setting() {
            return (int) Math.round(100 + value * 300);
        }

        private void sync() {
            value = (menu.speed() - 1) / 3;
            lastSent = setting();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(
                    Component.literal(
                            String.format(java.util.Locale.ROOT, "%.2f×", setting() / 100.0)));
        }

        @Override
        protected void applyValue() {
            if (!dragging && setting() != lastSent) {
                send(setting());
                lastSent = setting();
            }
        }

        @Override
        public void onClick(double x, double y) {
            dragging = true;
            super.onClick(x, y);
        }

        @Override
        public void onRelease(double x, double y) {
            dragging = false;
            applyValue();
            super.onRelease(x, y);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY(), thumb = x + 4 + (int) Math.round(value * (width - 8));
            g.fill(x, y + 8, x + width, y + 12, 0xFFE8DFED);
            g.fill(x, y + 8, thumb, y + 12, FinalityGui.PURPLE);
            g.fill(
                    thumb - 4,
                    y + 3,
                    thumb + 4,
                    y + 17,
                    isHoveredOrFocused() ? FinalityGui.GOLD : FinalityGui.PURPLE);
            g.fill(thumb - 3, y + 4, thumb + 3, y + 16, FinalityGui.PAPER);
            g.drawString(
                    font,
                    getMessage(),
                    x + (width - font.width(getMessage())) / 2,
                    y - 12,
                    FinalityGui.TEXT,
                    false);
            g.drawString(font, "1×", x, y + 24, FinalityGui.MUTED, false);
            g.drawString(
                    font, "4×", x + width - font.width("4×"), y + 24, FinalityGui.MUTED, false);
        }
    }
}
