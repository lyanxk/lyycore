package org.lyy.lyycore.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.CelestialOrderMenu;

public final class CelestialOrderScreen extends AbstractContainerScreen<CelestialOrderMenu> {
    private static final Style FONT = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);
    private final List<AbstractButton> timeButtons = new ArrayList<>();
    public CelestialOrderScreen(CelestialOrderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title.copy().withStyle(FONT));
        imageWidth = 276; imageHeight = 224;
    }
    private static Component text(String key) { return Component.translatable("gui.lyycore.celestial_order." + key).withStyle(FONT); }
    @Override protected void init() {
        super.init(); timeButtons.clear();
        for (int i = 0; i < 4; i++) {
            int hours = new int[]{-12, -6, 6, 12}[i];
            timeButtons.add(choice(20 + i * 60, 67, 56,
                    Component.literal((hours > 0 ? "+" : "") + hours + " h").withStyle(FONT), i, () -> false));
        }
        for (int i = 0; i < 3; i++) {
            choice(20 + i * 80, 121, 76, text(new String[]{"clear", "rain", "thunder"}[i]), 20 + i, () -> false);
            int count = new int[]{1, 10, 100}[i];
            choice(20 + i * 80, 175, 76, Component.literal("×" + count), 10 + i, () -> menu.iterations() == count);
        }
    }
    private AbstractButton choice(int x, int y, int width, Component label, int action, BooleanSupplier selected) {
        return addRenderableWidget(new AbstractButton(leftPos + x, topPos + y, width, 22, label) {
            @Override public void onPress() {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            }
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                ResearchScreen.panel(g, getX(), getY(), width, height,
                        selected.getAsBoolean() ? 0xFFD6ECFA : isHoveredOrFocused() ? 0xFFEDF5FD : 0xFFF0F3F8);
                g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + 7,
                        active ? SpacetimeGui.TEXT : 0xFFA5AFBC, false);
            }
            @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        });
    }
    @Override protected void containerTick() {
        super.containerTick(); timeButtons.forEach(button -> button.active = !menu.shifting());
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        SpacetimeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.renderItem(menu.tool(), leftPos + 20, topPos + 15);
        g.drawString(font, title, leftPos + 44, topPos + 20, SpacetimeGui.TEXT, false);
        g.fill(leftPos + 20, topPos + 40, leftPos + 256, topPos + 41, SpacetimeGui.BLUE);
        g.drawString(font, text("time"), leftPos + 20, topPos + 53, SpacetimeGui.TEXT, false);
        g.drawString(font, text("weather"), leftPos + 20, topPos + 107, SpacetimeGui.TEXT, false);
        g.drawString(font, text("random_ticks"), leftPos + 20, topPos + 161, SpacetimeGui.TEXT, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }
}
