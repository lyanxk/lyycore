package org.lyy.lyycore.client.screen;

import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.menu.DragonControlMenu;

public final class DragonControlScreen extends AbstractContainerScreen<DragonControlMenu> {
    private final ArrayList<Button> modes = new ArrayList<>();
    public DragonControlScreen(DragonControlMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 216; imageHeight = 130; }
    @Override protected void init() {
        super.init(); modes.clear();
        String[] names = {"patrol", "breath", "stowed"};
        for (int i = 0; i < 3; i++) {
            int mode = i;
            modes.add(addRenderableWidget(Button.builder(Component.translatable("gui.lyycore.dragon."+names[i]), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode))
                    .bounds(leftPos+12+i*65, topPos+49, 62, 22).build()));
        }
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        PureSmeltingScreen.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < modes.size(); i++) modes.get(i).active = menu.mode() >= 0 && menu.mode() != i;
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawCenteredString(font, title, imageWidth/2, 14, 0x593F50);
        g.drawWordWrap(font, Component.translatable(menu.mode() == 2 ? "gui.lyycore.dragon.full_power" : "gui.lyycore.dragon.quarter_power"), 12, 87, imageWidth-24, 0x76586B);
    }
}
