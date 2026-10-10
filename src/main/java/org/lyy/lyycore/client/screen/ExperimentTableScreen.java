package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.menu.ExperimentTableMenu;
import org.lyy.lyycore.content.research.ExperimentDefinition;

/** Current and target graphs occupy the same board; crossings are only lines. */
public final class ExperimentTableScreen extends AbstractContainerScreen<ExperimentTableMenu> {
    private static final int BOARD_X = 15, BOARD_Y = 37, CELL = 20;
    private int selected = -1;
    private ItemStack observed = ItemStack.EMPTY;
    private Button reset;
    public ExperimentTableScreen(ExperimentTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 252; imageHeight = 237;
        inventoryLabelX = 45; inventoryLabelY = 143;
    }
    @Override protected void init() {
        super.init();
        reset = addRenderableWidget(new Button(leftPos + 176, topPos + 128, 65, 18,
                Component.translatable("gui.lyycore.experiment.reset"), b -> send(-1), message -> message.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                int border = active ? 0xFFB69A55 : 0xFFCEC7B8;
                g.fill(getX(), getY(), getX() + width, getY() + height, border);
                g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                        active && isHoveredOrFocused() ? 0xFFFFEDDB : 0xFFF9F7F1);
                g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                        getY() + (height - 8) / 2, active ? 0xFF81704C : 0xFFB1ACA2, false);
            }
        });
    }
    private void send(int action) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        selected = -1;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ItemStack report = menu.getSlot(1).getItem();
        if (!ItemStack.matches(report, observed)) { selected = -1; observed = report.copy(); }
        ResearchScreen.panel(g, leftPos, topPos, imageWidth, imageHeight, 0xFFF9F7F1);
        for (int i = 0; i < 3; i++) {
            int x = leftPos + 14 + i * 78, color = ExperimentDefinition.Element.values()[i].color;
            g.drawString(font, Component.translatable("gui.lyycore.experiment." + ExperimentDefinition.Element.values()[i].getSerializedName()), x, topPos + 17, color, false);
            String value = menu.reserve(i) + "/100";
            g.drawString(font, value, x + 67 - font.width(value), topPos + 17, 0xFF716B62, false);
            g.fill(x, topPos + 28, x + 67, topPos + 32, 0xFFE8E3D8);
            g.fill(x, topPos + 28, x + menu.reserve(i) * 67 / 100, topPos + 32, color);
        }
        int bx = leftPos + BOARD_X, by = topPos + BOARD_Y;
        g.fill(bx - 1, by - 1, bx + CELL * 5 + 1, by + CELL * 5 + 1, 0xFFCCBD97);
        for (int row = 0; row < 5; row++) for (int col = 0; col < 5; col++)
            g.fill(bx + col * CELL, by + row * CELL, bx + (col + 1) * CELL - 1, by + (row + 1) * CELL - 1, 0xFFFFFCF5);
        var experiment = menu.experiment();
        boolean complete = experiment != null && experiment.complete(menu.positions(experiment));
        reset.active = experiment != null;
        if (experiment != null) {
            graph(g, experiment, experiment.targetCells(), true);
            graph(g, experiment, menu.positions(experiment), false);
        }
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFCCBD97);
            g.fill(x, y, x + 16, y + 16, 0xFFF0EBE1);
        }
        label(g, "material", 43);
        label(g, "report", 82);
        if (complete) label(g, "complete", 117);
        else if (!report.isEmpty() && experiment == null) label(g, "unneeded", 117);
    }
    private void label(GuiGraphics g, String key, int y) {
        Component text = Component.translatable("gui.lyycore.experiment." + key);
        g.drawString(font, text, leftPos + 211 - font.width(text) / 2, topPos + y, 0xFF81704C, false);
    }
    private float x(int cell) { return leftPos + BOARD_X + (cell % 5 + .5F) * CELL; }
    private float y(int cell) { return topPos + BOARD_Y + (cell / 5 + .5F) * CELL; }
    private void graph(GuiGraphics g, ExperimentDefinition experiment, int[] cells, boolean target) {
        for (var link : experiment.links()) {
            float ax = x(cells[link.from()]), ay = y(cells[link.from()]);
            float bx = x(cells[link.to()]), by = y(cells[link.to()]);
            line(g, ax, ay, (ax + bx) / 2, (ay + by) / 2, target ? 3 : 1.5F, color(experiment, link.from(), target));
            line(g, (ax + bx) / 2, (ay + by) / 2, bx, by, target ? 3 : 1.5F, color(experiment, link.to(), target));
        }
        for (int i = 0; i < cells.length; i++) {
            int x = Math.round(x(cells[i])), y = Math.round(y(cells[i]));
            if (!target && selected == i) {
                g.fill(x - 8, y - 8, x + 8, y + 8, 0xFFB99B51);
                g.fill(x - 7, y - 7, x + 7, y + 7, 0xFFFFFCF5);
            }
            int radius = target ? 7 : 5;
            g.fill(x - radius, y - radius, x + radius, y + radius, color(experiment, i, target));
            if (!target) g.fill(x - 3, y - 3, x + 1, y - 1, 0x99FFFFFF);
        }
    }
    private static int color(ExperimentDefinition graph, int node, boolean target) {
        return graph.nodes().get(node).element().color & 0xFFFFFF | (target ? 0x38000000 : 0xFF000000);
    }
    private static void line(GuiGraphics g, float ax, float ay, float bx, float by, float width, int color) {
        float length = (float)Math.hypot(bx - ax, by - ay);
        if (length == 0) return;
        float dx = -(by - ay) / length * width / 2, dy = (bx - ax) / length * width / 2;
        var vertices = g.bufferSource().getBuffer(RenderType.gui());
        var pose = g.pose().last().pose();
        vertices.addVertex(pose, ax + dx, ay + dy, 0).setColor(color);
        vertices.addVertex(pose, bx + dx, by + dy, 0).setColor(color);
        vertices.addVertex(pose, bx - dx, by - dy, 0).setColor(color);
        vertices.addVertex(pose, ax - dx, ay - dy, 0).setColor(color);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int col = (int)Math.floor((mouseX - leftPos - BOARD_X) / CELL);
        int row = (int)Math.floor((mouseY - topPos - BOARD_Y) / CELL);
        var experiment = menu.experiment();
        if (button == 0 && col >= 0 && col < 5 && row >= 0 && row < 5 && experiment != null) {
            int[] positions = menu.positions(experiment);
            if (experiment.complete(positions)) return true;
            int cell = row * 5 + col;
            if (selected >= 0) send(selected * 25 + cell);
            else for (int i = 0; i < positions.length; i++) if (positions[i] == cell) { selected = i; break; }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial); renderTooltip(g, mouseX, mouseY);
    }
}
