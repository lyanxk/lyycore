package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity.Page;
import org.lyy.lyycore.content.menu.SpacetimeFactoryMenu;

/** Viewing a page never pauses the other jobs or changes their energy priority. */
public final class SpacetimeFactoryScreen extends AbstractContainerScreen<SpacetimeFactoryMenu> {
    private Button previous, next;
    private static final int ENERGY_X = 16, ENERGY_Y = 196, ENERGY_WIDTH = 72, ENERGY_HEIGHT = 6;

    public SpacetimeFactoryScreen(SpacetimeFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = SpacetimeFactoryMenu.WIDTH; imageHeight = SpacetimeFactoryMenu.HEIGHT;
    }

    @Override protected void init() {
        super.init();
        previous = addRenderableWidget(new PageButton(10, "‹", -1));
        next = addRenderableWidget(new PageButton(imageWidth - 28, "›", 1));
        updateControls();
    }

    private void updateControls() {
        previous.active = menu.pageIndex() > 0;
        next.active = menu.pageIndex() < Page.values().length - 1;
    }

    @Override protected void containerTick() {
        super.containerTick(); updateControls();
    }

    private void changePage(int delta) {
        int index = menu.pageIndex() + delta;
        if (index < 0 || index >= Page.values().length) return;
        menu.selectPage(index); updateControls();
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
    }

    public static Component pageName(Page page) {
        String key = switch (page) {
            case SPACETIME -> "spacetime"; case CONDENSING -> "condensing";
            case CRAFTING -> "imaginary_crafting"; case ALLOYING -> "imaginary_alloying";
        };
        return Component.translatable("gui.lyycore.spacetime.page." + key);
    }

    public int recipeX() { return menu.page() == Page.SPACETIME ? 117 : menu.page() == Page.ALLOYING ? 138 : 159; }
    public int recipeY() { return menu.page() == Page.SPACETIME ? 121 : menu.page() == Page.ALLOYING ? 105 : 103; }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        Page page = menu.page();
        boolean spacetime = page == Page.SPACETIME;
        SpacetimeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int[][] p = SpacetimeFactoryMenu.positions(page);
        float charge = menu.capacity() == 0 ? 0 : (float) menu.storedEnergy() / menu.capacity();
        int energyColor = Mth.lerpInt(charge, 65, 255) << 24 | SpacetimeGui.PINK & 0xFFFFFF;
        float phase = minecraft.level == null ? 0 : (minecraft.level.getGameTime() % 40 + partial) / 40;
        int pulse = menu.running(page) ? Math.round(135 + 120 * (1 + (float)Math.cos(phase * Math.PI * 2)) / 2) : 100;
        int workingColor = pulse << 24 | SpacetimeGui.BLUE & 0xFFFFFF;
        if (page == Page.SPACETIME) {
            SpacetimeGui.diagram(g, leftPos + p[16][0] + 8, topPos + p[16][1] + 8, energyColor, workingColor);
        } else if (page == Page.CONDENSING || page == Page.CRAFTING) {
            ForgeGui.panel(g, leftPos + 39, topPos + 51, 178, 116);
            float progress = Math.clamp((menu.progress(page) + (menu.running(page) ? partial : 0)) / menu.duration(page), 0, 1);
            ImaginaryCraftingGui.diagram(g, leftPos + SpacetimeFactoryMenu.CRAFTING_X,
                    topPos + SpacetimeFactoryMenu.CRAFTING_Y, progress);
            ForgeGui.arrow(g, leftPos + recipeX(), topPos + recipeY(), ForgeGui.ACCENT);
        } else {
            ForgeGui.panel(g, leftPos + 40, topPos + 65, 176, 80);
            ForgeGui.arrow(g, leftPos + recipeX(), topPos + recipeY(), ForgeGui.ACCENT);
        }
        for (int i = 0; i <= page.inputCount; i++) {
            if (!spacetime && (page == Page.ALLOYING || i == page.inputCount))
                ForgeGui.slot(g, leftPos + p[i][0], topPos + p[i][1], page == Page.ALLOYING ? i == 2 : i == page.inputCount);
        }
        for (int i = SpacetimeFactoryMenu.MACHINE_SLOTS; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            SpacetimeGui.slot(g, leftPos + slot.x, topPos + slot.y, 0xFFB0C3DA);
        }
        int[] output = p[page.inputCount];
        int width = Math.round(16F * menu.progress(page) / menu.duration(page));
        g.fill(leftPos + output[0], topPos + output[1] + 18, leftPos + output[0] + 16, topPos + output[1] + 20, 0xFFDCE5F0);
        if (width > 0) g.fill(leftPos + output[0], topPos + output[1] + 18, leftPos + output[0] + width, topPos + output[1] + 20, spacetime ? SpacetimeGui.BLUE : ForgeGui.ACCENT);
        int x = leftPos + ENERGY_X, y = topPos + ENERGY_Y;
        g.fill(x - 1, y - 1, x + ENERGY_WIDTH + 1, y + ENERGY_HEIGHT + 1, spacetime ? SpacetimeGui.PINK : ForgeGui.ACCENT);
        g.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, ForgeGui.TRACK);
        g.fill(x, y, x + ForgeGui.scaled(menu.storedEnergy(), menu.capacity(), ENERGY_WIDTH), y + ENERGY_HEIGHT,
                spacetime ? SpacetimeGui.PINK : ForgeGui.ACCENT);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ForgeGui.centeredText(g, font, pageName(menu.page()), imageWidth / 2, 12, SpacetimeGui.TEXT);
        if (menu.page() == Page.ALLOYING) {
            g.drawString(font, Component.translatable("tooltip.lyycore.forge.input"), 48, 75, ForgeGui.TEXT, false);
            g.drawString(font, Component.translatable("tooltip.lyycore.forge.catalyst"), 92, 75, ForgeGui.TEXT, false);
            g.drawString(font, Component.translatable("tooltip.lyycore.forge.output"), 166, 75, ForgeGui.TEXT, false);
        } else if (menu.page() != Page.SPACETIME) {
            ForgeGui.centeredText(g, font, Component.translatable("tooltip.lyycore.forge.output"), 197, 85, ForgeGui.TEXT);
        }
        String energy = String.format(java.util.Locale.ROOT, "%,d IE", menu.storedEnergy());
        g.drawString(font, energy, ENERGY_X + ENERGY_WIDTH + 8, ENERGY_Y - 1, SpacetimeGui.TEXT, false);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial); renderTooltip(g, mouseX, mouseY);
        if (isHovering(ENERGY_X - 1, ENERGY_Y - 2, imageWidth - ENERGY_X * 2, ENERGY_HEIGHT + 4, mouseX, mouseY))
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("tooltip.lyycore.energy", menu.storedEnergy(), menu.capacity()),
                    Component.translatable(menu.energyPerTick(menu.page()) ? "screen.lyycore.condensing.rate" : "screen.lyycore.condensing.cost", menu.energyCost(menu.page()))), mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.index == SpacetimeFactoryMenu.outputSlot(menu.page()) && menu.outputCount(menu.page()) > 64)
            g.renderTooltip(font, Component.literal(String.format(java.util.Locale.ROOT, "×%,d", menu.outputCount(menu.page()))), mouseX, mouseY);
    }

    private final class PageButton extends Button {
        PageButton(int x, String text, int delta) { super(leftPos + x, topPos + 8, 18, 18, Component.literal(text), b -> changePage(delta), DEFAULT_NARRATION); }
        @Override protected void renderWidget(GuiGraphics g, int x, int y, float partial) {
            g.fill(getX(), getY(), getX() + width, getY() + height, isHoveredOrFocused() && active ? 0xFFDDECF8 : 0xFFEFF5FB);
            ForgeGui.centeredText(g, font, getMessage(), getX() + width / 2, getY() + 4,
                    active ? SpacetimeGui.TEXT : 0xFFAEB9C7);
        }
    }
}
