package org.lyy.lyycore.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.equipment.AccessoryMenu;

public final class AccessoryScreen extends EquipmentScreen<AccessoryMenu> {
    public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 220, 128); }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        for (int i = 0; i < 2; i++) ForgeGui.centeredText(g, font, text("kind." + i), 96 + i * 56, 37, ForgeGui.TEXT);
    }
}
