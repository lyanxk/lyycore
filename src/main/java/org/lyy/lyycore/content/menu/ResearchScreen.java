package org.lyy.lyycore.content.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.research.ResearchDefinition;

import java.util.ArrayList;
import java.util.List;

public final class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
    private static final int INK = 0xFF302D35, MUTED_INK = 0xFF68616A, GOLD = 0xFFB69A55, PAPER = 0xFFF9F7F1;
    private static final int PAGE_SIZE = 18, COLUMNS = 6, DETAIL_HEIGHT = 111;
    private int page, selected = -1, scroll, materialRow;
    private List<Integer> visibleEntries = List.of();
    private boolean confirming;
    private PaperButton researchButton, confirmButton;

    public ResearchScreen(ResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 222;
    }

    private static MutableComponent text(String key, Object... args) {
        return translated("gui.lyycore.research." + key, args);
    }

    private static MutableComponent translated(String key, Object... args) {
        return Component.translatable(key, args);
    }

    @Override protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        refreshVisibleEntries();
        clearWidgets();
        researchButton = confirmButton = null;
        if (confirming) {
            confirmButton = button(67, 186, 76, 20, text("yes"), () -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
                confirming = false;
                rebuildButtons();
            });
            button(157, 186, 76, 20, text("no"), this::closeConfirmation);
        } else if (selected >= 0) {
            button(15, 13, 50, 19, text("back"), () -> { selected = -1; scroll = 0; rebuildButtons(); });
            if (!menu.isMemory() || research().production().isPresent()) {
                researchButton = button(92, 190, 116, 21, text(menu.isMemory() ? "transcribe" : "research"), () -> {
                    if (menu.isMemory()) {
                        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    } else {
                        confirming = true;
                        materialRow = 0;
                        if (minecraft.gameMode != null)
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchMenu.previewButton(selected));
                        rebuildButtons();
                    }
                });
            }
        } else {
            for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < visibleEntries.size(); i++) {
                final int index = visibleEntries.get(page * PAGE_SIZE + i);
                var entry = menu.entries().get(index).value();
                PaperButton icon = button(34 + i % COLUMNS * 39, 49 + i / COLUMNS * 43, 37, 37,
                        translated(entry.title()), () -> { selected = index; scroll = 0; rebuildButtons(); });
                icon.icon = entry.icon();
                icon.entry = index;
            }
            button(24, 190, 48, 20, Component.literal("<"), () -> { page--; rebuildButtons(); }).active = page > 0;
            button(228, 190, 48, 20, Component.literal(">"), () -> { page++; rebuildButtons(); }).active = page + 1 < pages();
        }
        refreshStatus();
    }

    private PaperButton button(int x, int y, int width, int height, Component label, Runnable action) {
        return addRenderableWidget(new PaperButton(leftPos + x, topPos + y, width, height, label, action));
    }

    private boolean refreshVisibleEntries() {
        // Keep the menu's original indices for status sync and server button requests.
        List<Integer> visible = new ArrayList<>();
        for (int i = 0; i < menu.entries().size(); i++) {
            if (menu.isMemory() || menu.status(i) != ResearchMenu.COMPLETED) visible.add(i);
        }
        if (visibleEntries.equals(visible)) return false;
        visibleEntries = visible;
        page = Mth.clamp(page, 0, pages() - 1);
        if (selected >= 0 && !visibleEntries.contains(selected)) {
            selected = -1;
            confirming = false;
            scroll = materialRow = 0;
        }
        return true;
    }

    private int pages() { return Math.max(1, (visibleEntries.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    private ResearchDefinition research() { return menu.entries().get(selected).value(); }

    private void refreshStatus() {
        if (selected < 0) return;
        int status = menu.status(selected);
        if (researchButton != null && !menu.isMemory()) {
            researchButton.active = status != ResearchMenu.COMPLETED;
            researchButton.setMessage(text(status == ResearchMenu.COMPLETED ? "completed" : "research"));
        }
        if (confirmButton != null) confirmButton.active = status == ResearchMenu.READY;
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (refreshVisibleEntries()) rebuildButtons();
        else refreshStatus();
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        panel(g, leftPos, topPos, imageWidth, imageHeight, PAPER);
        // Inset rules and diamond ornaments echo the supplied white-and-gold table.
        g.fill(leftPos + 13, topPos + 37, leftPos + 287, topPos + 38, GOLD);
        g.fill(leftPos + 13, topPos + 180, leftPos + 287, topPos + 181, GOLD);
        if (selected < 0) {
            drawCenteredText(g, title, leftPos + 150, topPos + 18, INK);
            if (visibleEntries.isEmpty()) {
                drawCenteredText(g, text(menu.isMemory() ? "memory_empty" : "empty"), leftPos + 150, topPos + 97, MUTED_INK);
            }
            drawCenteredText(g, text("page", page + 1, pages()), leftPos + 150, topPos + 196, INK);
        } else if (!confirming) {
            renderDetails(g);
        }
        if (confirming) renderConfirmation(g);
    }

    private void renderDetails(GuiGraphics g) {
        ResearchDefinition research = research();
        g.renderItem(research.icon(), leftPos + 267, topPos + 14);
        var title = translated(research.title());
        float scale = Math.min(1F, 188F / Math.max(1, font.width(title)));
        g.pose().pushPose();
        g.pose().translate(leftPos + 165, topPos + 18, 0);
        g.pose().scale(scale, scale, 1);
        int titleX = -font.width(title) / 2;
        g.drawString(font, title, titleX, 0, research.rarity().color, false);
        g.pose().popPose();

        List<net.minecraft.util.FormattedCharSequence> summary = font.split(
                translated(research.summary()), 248);
        List<net.minecraft.util.FormattedCharSequence> body = font.split(translated(research.description()), 248);
        int contentHeight = summary.size() * 12 + 12 + body.size() * 12;
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - DETAIL_HEIGHT));
        g.enableScissor(leftPos + 22, topPos + 51, leftPos + 279, topPos + 51 + DETAIL_HEIGHT);
        int y = topPos + 51 - scroll;
        for (var line : summary) { g.drawString(font, line, leftPos + 24, y, MUTED_INK, false); y += 12; }
        y += 12;
        for (var line : body) { g.drawString(font, line, leftPos + 24, y, INK, false); y += 12; }
        g.disableScissor();
        if (contentHeight > DETAIL_HEIGHT) {
            g.fill(leftPos + 281, topPos + 51, leftPos + 283, topPos + 162, 0xFFE8DFC8);
            int thumb = Math.max(10, DETAIL_HEIGHT * DETAIL_HEIGHT / contentHeight);
            int offset = scroll * (DETAIL_HEIGHT - thumb) / (contentHeight - DETAIL_HEIGHT);
            g.fill(leftPos + 281, topPos + 51 + offset, leftPos + 283, topPos + 51 + offset + thumb, GOLD);
        }
    }

    private void renderConfirmation(GuiGraphics g) {
        g.fill(leftPos + 4, topPos + 4, leftPos + 296, topPos + 218, 0xB03A3540);
        panel(g, leftPos + 10, topPos + 10, 280, 202, PAPER);
        drawCenteredText(g, text("confirm"), leftPos + 150, topPos + 25, INK);
        g.fill(leftPos + 23, topPos + 43, leftPos + 277, topPos + 44, GOLD);
        g.drawString(font, text("materials"), leftPos + 28, topPos + 52, INK, false);
        List<ItemStack> costs = research().materials();
        if (costs.isEmpty()) g.drawString(font, text("none"), leftPos + 28, topPos + 77, MUTED_INK, false);
        for (int i = materialRow * 8; i < Math.min(costs.size(), (materialRow + 3) * 8); i++) {
            int cell = i - materialRow * 8;
            int x = leftPos + 28 + cell % 8 * 30, y = topPos + 67 + cell / 8 * 22;
            g.fill(x - 2, y - 2, x + 20, y + 19, 0xFFEDE7DA);
            g.renderItem(costs.get(i), x, y);
            g.renderItemDecorations(font, costs.get(i), x, y);
        }
        if (costs.size() > 24) g.drawString(font, text("scroll"), leftPos + 170, topPos + 52, MUTED_INK, false);
        Component experience = research().experiencePoints() > 0
                ? text("experience_points", research().experiencePoints()) : text("experience", research().experienceLevels());
        g.drawString(font, experience, leftPos + 28, topPos + 132, INK, false);
        if (menu.status(selected) == ResearchMenu.MISSING_COST)
            g.drawString(font, text("missing"), leftPos + 160, topPos + 132, 0xFFBD3030, false);
        if (research().dangerous()) {
            int y = topPos + 150;
            for (var line : font.split(text("danger"), 246)) {
                g.drawString(font, line, leftPos + 28, y, 0xFFBD3030, false);
                y += 11;
            }
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (confirming) {
            var costs = research().materials();
            for (int i = materialRow * 8; i < Math.min(costs.size(), (materialRow + 3) * 8); i++) {
                int cell = i - materialRow * 8;
                if (isHovering(26 + cell % 8 * 30, 65 + cell / 8 * 22, 22, 21, mouseX, mouseY))
                    g.renderTooltip(font, costs.get(i), mouseX, mouseY);
            }
        } else if (selected < 0) {
            for (var child : children()) {
                if (child instanceof PaperButton button && button.entry >= 0 && button.isHovered()) {
                    var entry = menu.entries().get(button.entry).value();
                    var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
                    lines.add(translated(entry.title()).withColor(entry.rarity().color).getVisualOrderText());
                    lines.addAll(font.split(translated(entry.summary()).withStyle(ChatFormatting.GRAY), 220));
                    if (menu.status(button.entry) == ResearchMenu.COMPLETED)
                        lines.add(text("completed").withStyle(ChatFormatting.GOLD).getVisualOrderText());
                    g.renderTooltip(font, lines, mouseX, mouseY);
                }
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    private void drawCenteredText(GuiGraphics g, Component text, int x, int y, int color) {
        // The vanilla centered helper always adds a shadow, which muddies dark text on paper.
        g.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }

    private void closeConfirmation() {
        confirming = false;
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchMenu.CLOSE_PREVIEW);
        rebuildButtons();
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (selected < 0) return super.mouseScrolled(x, y, horizontal, vertical);
        if (confirming) materialRow = Mth.clamp(materialRow - (int) Math.signum(vertical), 0,
                Math.max(0, (research().materials().size() + 7) / 8 - 3));
        else scroll = Math.max(0, scroll - (int) (vertical * 18));
        return true;
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256 && (confirming || selected >= 0)) {
            if (confirming) closeConfirmation();
            else { selected = -1; rebuildButtons(); }
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    static void panel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x60000000);
        g.fill(x, y, x + w, y + h, GOLD);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFCF2);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, GOLD);
        g.fill(x + 5, y + 5, x + w - 5, y + h - 5, fill);
        for (int dx : new int[]{2, w - 5}) for (int dy : new int[]{2, h - 5})
            g.fill(x + dx, y + dy, x + dx + 3, y + dy + 3, GOLD);
    }

    private final class PaperButton extends AbstractButton {
        private final Runnable action;
        private ItemStack icon = ItemStack.EMPTY;
        private int entry = -1;

        private PaperButton(int x, int y, int w, int h, Component label, Runnable action) {
            super(x, y, w, h, label);
            this.action = action;
        }
        @Override public void onPress() { action.run(); }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int border = active ? GOLD : 0xFFCFC8B9;
            g.fill(getX(), getY(), getX() + width, getY() + height, border);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                    isHoveredOrFocused() && active ? 0xFFF1E5C6 : PAPER);
            if (icon.isEmpty()) {
                int x = getX() + (width - font.width(getMessage())) / 2;
                g.drawString(font, getMessage(), x, getY() + (height - 8) / 2, active ? INK : 0xFFAAA397, false);
            } else {
                g.renderItem(icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
                if (menu.status(entry) == ResearchMenu.COMPLETED)
                    g.fill(getX() + width - 7, getY() + 3, getX() + width - 3, getY() + 7, GOLD);
            }
        }
    }
}
