package org.lyy.lyycore.client.screen;

import org.lyy.lyycore.content.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.blockEntities.AdvancedImaginaryGateBlockEntity;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public final class AdvancedImaginaryGateScreen extends AbstractContainerScreen<AdvancedImaginaryGateMenu> {
    private static final int ROWS = 6;
    private static final Style TEXT_STYLE = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);
    private final Button[] rows = new Button[ROWS];
    private List<Integer> matches = List.of();
    private int page;
    private EditBox search;
    private Button previous, next, action;

    public AdvancedImaginaryGateScreen(AdvancedImaginaryGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title.copy().withStyle(TEXT_STYLE));
        imageWidth = 356;
        imageHeight = 202;
        inventoryLabelY = 92;
    }
    private static Component text(String key, Object... args) { return Component.translatable("screen.lyycore.advanced_gate." + key, args).withStyle(TEXT_STYLE); }
    private String biomeName(ResourceLocation id) {
        String key = "biome." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        return I18n.exists(key) ? I18n.get(key) : id.toString();
    }
    @Override protected void init() {
        super.init();
        search = addRenderableWidget(new EditBox(font, leftPos + 185, topPos + 23, 160, 16, text("search")));
        search.setMaxLength(128);
        search.setHint(text("search"));
        search.setResponder(this::filter);
        for (int row = 0; row < ROWS; row++) {
            final int offset = row;
            rows[row] = addRenderableWidget(Button.builder(Component.empty(), b -> send(matches.get(page * ROWS + offset)))
                    .bounds(leftPos + 184, topPos + 43 + row * 17, 164, 16).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; updateRows(); })
                .bounds(leftPos + 184, topPos + 149, 20, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; updateRows(); })
                .bounds(leftPos + 328, topPos + 149, 20, 16).build());
        action = addRenderableWidget(Button.builder(text("start"), b -> send(menu.running() ? AdvancedImaginaryGateMenu.PAUSE : AdvancedImaginaryGateMenu.START))
                .bounds(leftPos + 184, topPos + 176, 164, 18).build());
        filter("");
    }
    private void send(int button) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button); }
    private void filter(String input) {
        String query = input.strip().toLowerCase(Locale.ROOT);
        matches = IntStream.range(0, menu.biomes().size()).filter(i -> {
            ResourceLocation biome = menu.biomes().get(i);
            return biome.toString().contains(query) || biomeName(biome).toLowerCase(Locale.ROOT).contains(query);
        }).boxed().toList();
        page = 0;
        updateRows();
    }
    private void updateRows() {
        for (int row = 0; row < ROWS; row++) {
            int index = page * ROWS + row;
            rows[row].visible = index < matches.size();
            if (!rows[row].visible) continue;
            int biome = matches.get(index);
            String label = (menu.selected() == biome ? "● " : "") + biomeName(menu.biomes().get(biome));
            rows[row].setMessage(Component.literal(label).withStyle(TEXT_STYLE));
            rows[row].active = !menu.running();
        }
        previous.active = page > 0;
        next.active = (page + 1) * ROWS < matches.size();
        action.setMessage(text(menu.running() ? "pause" : "start"));
    }
    @Override protected void containerTick() { super.containerTick(); updateRows(); }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 177, topPos + 7, leftPos + 178, topPos + 195, 0xFFB98BA9);
        g.fill(leftPos + 57, topPos + 25, leftPos + 119, topPos + 69, 0xFFB98BA9);
        g.fill(leftPos + 60, topPos + 28, leftPos + 116, topPos + 66, 0xFFFAF2FA);
        for (var slot : menu.slots) ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, slot.index == 0);
        g.fill(leftPos + 185, topPos + 168, leftPos + 347, topPos + 171, 0xFFD9CDCF);
        int progress = 162 * menu.completedColumns() / AdvancedImaginaryGateBlockEntity.totalColumns();
        g.fill(leftPos + 185, topPos + 168, leftPos + 185 + progress, topPos + 171, 0xFFD787B5);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, ForgeGui.TEXT, false);
        g.drawString(font, playerInventoryTitle.copy().withStyle(TEXT_STYLE), 8, inventoryLabelY, ForgeGui.TEXT, false);
        g.drawString(font, text("terrain"), 184, 7, ForgeGui.TEXT, false);
        // The advanced gate intentionally does not reveal its offering requirements.
        if (menu.status() != 0) {
            Component status = Component.translatable("screen.lyycore.gate.status." + menu.status()).withStyle(TEXT_STYLE);
            int y = 73;
            for (var line : font.split(status, 160)) { g.drawString(font, line, 8, y, ForgeGui.TEXT, false); y += 9; }
        }
        Component progress = menu.waitingForChunk() ? text("waiting")
                : text("progress", menu.completedColumns(), AdvancedImaginaryGateBlockEntity.totalColumns());
        g.drawString(font, progress, 266 - font.width(progress) / 2, 153, ForgeGui.TEXT, false);
        Component range = text("range");
        float scale = Math.min(1, 161F / font.width(range));
        g.pose().pushPose();
        g.pose().translate(8, 188, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, range, 0, 0, ForgeGui.TEXT, false);
        g.pose().popPose();
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        for (int row = 0; row < ROWS; row++) if (rows[row].visible && rows[row].isHovered())
            g.renderTooltip(font, Component.literal(menu.biomes().get(matches.get(page * ROWS + row)).toString()), mouseX, mouseY);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (search.isFocused() && key != 256) return search.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= leftPos + 184 && x < leftPos + 348 && y >= topPos + 43 && y < topPos + 145) {
            page = Math.clamp(page - (int) Math.signum(vertical), 0, Math.max(0, (matches.size() - 1) / ROWS));
            updateRows();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
}
