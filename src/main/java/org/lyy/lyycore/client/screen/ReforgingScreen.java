package org.lyy.lyycore.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.content.equipment.*;

public final class ReforgingScreen extends EquipmentScreen<ReforgingMenu> {
    private Button reforge;
    private final List<Button> rows = new ArrayList<>();
    public ReforgingScreen(ReforgingMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 252, 160); }
    @Override protected void init() {
        super.init(); rows.clear();
        reforge = button(17, 115, 66, text("reforge"), -1);
        for (int i = 0; i < 4; i++) rows.add(button(99, 37 + i * 24, 132, Component.empty(), i));
    }
    @Override protected void containerTick() {
        super.containerTick(); refresh();
    }
    private void refresh() {
        var data = AccessoryData.read(menu.accessory());
        reforge.active = menu.canReforge();
        for (int i = 0; i < rows.size(); i++) {
            var button = rows.get(i); button.visible = i < data.traits().size();
            if (button.visible) button.setMessage(text((menu.locks() & 1 << i) != 0 ? "locked" : "unlocked", AccessoryItem.statName(data.traits().get(i))));
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        ForgeGui.centeredText(g, font, text("item"), 50, 30, ForgeGui.TEXT);
        ForgeGui.centeredText(g, font, text("material"), 50, 70, ForgeGui.TEXT);
        ForgeGui.centeredText(g, font, text("cost", menu.cost()), 50, 103, ForgeGui.TEXT);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        refresh(); super.render(g, mx, my, partial);
        if (reforge.isHoveredOrFocused()) g.renderTooltip(font, font.split(text("reforge_hint"), 220), mx, my);
    }
}
