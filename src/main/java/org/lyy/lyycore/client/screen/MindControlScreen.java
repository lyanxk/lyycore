package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.MindControlMenu;

public final class MindControlScreen extends AbstractContainerScreen<MindControlMenu> {
    private static final String[] COMMANDS = {"execute", "gather", "control", "wander", "attack"};
    public MindControlScreen(MindControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 236; imageHeight = 164;
    }
    @Override protected void init() {
        super.init();
        for (int id = 0; id < COMMANDS.length; id++) {
            final int command = id;
            int x = id < 3 ? 14 + id * 70 : 49 + (id - 3) * 70;
            int y = id < 3 ? 66 : 96;
            addRenderableWidget(Button.builder(Component.translatable("screen.lyycore.control." + COMMANDS[id]), b -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, command);
            }).bounds(leftPos + x, topPos + y, 68, 22).build());
        }
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 12, topPos + 29, leftPos + imageWidth - 12, topPos + 30, ForgeGui.ACCENT);
        g.fill(leftPos + 14, topPos + 125, leftPos + 222, topPos + 126, ForgeGui.TRACK);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 13, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.control.count", menu.controlledCount()), 15, 42, ForgeGui.TEXT, false);
        String mode = switch (menu.mode()) { case 1 -> "attack"; case 2 -> "gather"; default -> "wander"; };
        g.drawString(font, Component.translatable("screen.lyycore.control." + mode), 160, 42, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.control.passive_scan"), 15, 132, ForgeGui.TEXT, false);
        g.drawString(font, Component.translatable("screen.lyycore.control.active_scan"), 15, 145, ForgeGui.TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) { super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY); }
}
