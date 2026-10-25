package org.lyy.lyycore.client.screen;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.energy.PowerChannels;
import org.lyy.lyycore.content.menu.SpacetimeMatrixMenu;
import org.lyy.lyycore.network.PowerChannelNetwork;

public final class SpacetimeMatrixScreen extends AbstractContainerScreen<SpacetimeMatrixMenu> {
    private EditBox name;
    private SpacetimeButton visibility, save;
    private UUID editing;
    private boolean publicChannel;
    public SpacetimeMatrixScreen(SpacetimeMatrixMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = SpacetimeMatrixMenu.WIDTH; imageHeight = SpacetimeMatrixMenu.HEIGHT;
        inventoryLabelX = 65; inventoryLabelY = 142;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.lyycore.spacetime_matrix." + key, args); }
    @Override protected void init() {
        super.init();
        name = addRenderableWidget(new EditBox(font, leftPos + 135, topPos + 45, 122, 14, text("channel_name")));
        name.setMaxLength(PowerChannels.NAME_LIMIT); name.setTextColor(SpacetimeGui.TEXT); name.setTextShadow(false);
        visibility = addRenderableWidget(new SpacetimeButton(leftPos + 132, topPos + 65, 77, text("private"), button -> {
            publicChannel = !publicChannel; refresh();
        }));
        save = addRenderableWidget(new SpacetimeButton(leftPos + 213, topPos + 65, 48, text("save"), button -> {
            if (editing != null) PacketDistributor.sendToServer(new PowerChannelNetwork.Request(menu.containerId,
                    PowerChannelNetwork.Action.UPDATE, editing, name.getValue(), publicChannel, null));
            name.setFocused(false); setFocused(null);
        }));
        editing = null; refresh();
    }
    private void refresh() {
        var channel = menu.snapshot().selected();
        if (channel != null && !channel.id().equals(editing)) {
            editing = channel.id(); name.setValue(channel.name()); publicChannel = channel.publicChannel();
        }
        boolean editable = channel != null && channel.editable();
        name.setEditable(editable); visibility.active = editable; save.active = editable && !name.getValue().isBlank();
        visibility.setMessage(text(publicChannel ? "public" : "private"));
        visibility.setTooltip(Tooltip.create(text(editable ? "public_hint" : "owner_only")));
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        SpacetimeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 124, topPos + 28, leftPos + 125, topPos + 131, 0xFFCEDDEE);
        for (var slot : menu.slots) SpacetimeGui.slot(g, leftPos + slot.x, topPos + slot.y,
                slot.index < 4 || slot.index == 10 ? SpacetimeGui.PINK : SpacetimeGui.BLUE);
        g.drawString(font, text("armor"), leftPos + 16, topPos + 30, SpacetimeGui.TEXT, false);
        g.drawString(font, text("charge"), leftPos + 51, topPos + 34, SpacetimeGui.TEXT, false);
        g.drawString(font, text("channel_name"), leftPos + 132, topPos + 30, SpacetimeGui.TEXT, false);
        var snapshot = menu.snapshot();
        g.drawString(font, text("power", snapshot.power(), snapshot.stage()), leftPos + 132, topPos + 91, SpacetimeGui.TEXT, false);
        long p = snapshot.power();
        g.drawString(font, text("rate", String.format(java.util.Locale.ROOT, "%,d", p * p * 10_000_000L)), leftPos + 20, topPos + 128, SpacetimeGui.TEXT, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ForgeGui.centeredText(g, font, title, imageWidth / 2, 12, SpacetimeGui.TEXT);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SpacetimeGui.TEXT, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial); renderTooltip(g, mouseX, mouseY);
        if (mouseX >= leftPos + 132 && mouseX < leftPos + 268 && mouseY >= topPos + 88 && mouseY < topPos + 103) {
            long p = menu.snapshot().power();
            g.renderTooltip(font, text("fe_rate", String.format(java.util.Locale.ROOT, "%,d", p * p * 21_474_836L)), mouseX, mouseY);
        }
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        // Text input must not trigger the inventory-close key while naming a channel.
        if (name.isFocused() && key != 256) return name.keyPressed(key, scan, modifiers) || name.canConsumeInput();
        return super.keyPressed(key, scan, modifiers);
    }
}
