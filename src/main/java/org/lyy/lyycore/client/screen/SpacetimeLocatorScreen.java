package org.lyy.lyycore.client.screen;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.menu.SpacetimeLocatorMenu;
import org.lyy.lyycore.network.SpacetimeLocatorNetwork;
import org.lyy.lyycore.network.SpacetimeLocatorNetwork.*;

public final class SpacetimeLocatorScreen extends AbstractContainerScreen<SpacetimeLocatorMenu> {
    private static final int PAGE_SIZE = 5;
    private final List<SpacetimeButton> channels = new ArrayList<>(), targets = new ArrayList<>(), deletes = new ArrayList<>();
    private SpacetimeButton channelPrevious, channelNext, targetPrevious, targetNext, input, output, mode;
    private int channelPage, targetPage;
    private Target selected;
    private Snapshot displayed;
    public SpacetimeLocatorScreen(SpacetimeLocatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 378; imageHeight = 270;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.lyycore.spacetime_locator." + key, args); }
    private SpacetimeButton button(int x, int y, int width, Component label, Button.OnPress action) {
        return addRenderableWidget(new SpacetimeButton(leftPos + x, topPos + y, width, label, action));
    }
    @Override protected void init() {
        super.init(); channels.clear(); targets.clear(); deletes.clear(); displayed = null;
        mode = button(287, 28, 70, Component.empty(), b -> send(menu.snapshot().area() ? Action.BLOCK : Action.AREA, -1));
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int row = i;
            channels.add(button(18, 49 + i * 21, 138, Component.empty(), b -> {
                int index = channelPage * PAGE_SIZE + row;
                if (index < menu.snapshot().channels().size()) send(Action.BIND, menu.snapshot().channels().get(index).id(), -1);
            }));
            targets.add(button(174, 49 + i * 21, 156, Component.empty(), b -> select(targetPage * PAGE_SIZE + row)));
            deletes.add(button(333, 49 + i * 21, 24, Component.literal("×"), b -> send(Action.REMOVE, targetPage * PAGE_SIZE + row)));
        }
        channelPrevious = button(18, 158, 24, Component.literal("<"), b -> { channelPage--; refresh(); });
        channelNext = button(132, 158, 24, Component.literal(">"), b -> { channelPage++; refresh(); });
        targetPrevious = button(174, 158, 24, Component.literal("<"), b -> { targetPage--; refresh(); });
        targetNext = button(333, 158, 24, Component.literal(">"), b -> { targetPage++; refresh(); });
        input = button(18, 183, 66, text("input"), b -> send(Action.INPUT, -1));
        output = button(90, 183, 66, text("output"), b -> send(Action.OUTPUT, -1));
        refresh();
    }
    private void send(Action action, int target) { send(action, menu.snapshot().bound(), target); }
    private void send(Action action, UUID channel, int target) {
        var s = menu.snapshot();
        PacketDistributor.sendToServer(new Request(menu.containerId, action, channel, s.output(), s.revision(), target));
    }
    private void select(int index) {
        if (index < 0 || index >= menu.snapshot().targets().size()) return;
        selected = menu.snapshot().targets().get(index);
        if (selected.previewPositions().stream().anyMatch(pos -> menu.preview(pos) == null)) send(Action.PREVIEW, index);
        refresh();
    }
    private void refresh() {
        var s = menu.snapshot();
        if (displayed != null && (!displayed.bound().equals(s.bound()) || displayed.output() != s.output())) { targetPage = 0; selected = null; }
        if (selected != null && !s.targets().contains(selected)) selected = null;
        displayed = s;
        channelPage = Math.clamp(channelPage, 0, Math.max(0, (s.channels().size() - 1) / PAGE_SIZE));
        targetPage = Math.clamp(targetPage, 0, Math.max(0, (s.targets().size() - 1) / PAGE_SIZE));
        for (int row = 0; row < PAGE_SIZE; row++) {
            int c = channelPage * PAGE_SIZE + row, t = targetPage * PAGE_SIZE + row;
            var channel = c < s.channels().size() ? s.channels().get(c) : null;
            channels.get(row).visible = channel != null;
            if (channel != null) {
                channels.get(row).selected = channel.id().equals(s.bound());
                channels.get(row).setMessage(Component.literal(font.plainSubstrByWidth(channel.name(), 126)));
                channels.get(row).setTooltip(Tooltip.create(Component.literal(channel.name()).append("\n")
                        .append(Component.translatable("gui.lyycore.spacetime_coordinates." + (channel.publicChannel() ? "public" : "private")))));
            }
            var target = t < s.targets().size() ? s.targets().get(t) : null;
            targets.get(row).visible = deletes.get(row).visible = target != null;
            if (target != null) {
                targets.get(row).selected = target.equals(selected);
                targets.get(row).setMessage(Component.literal("    " + font.plainSubstrByWidth(name(target).getString(), 117)));
                var tip = name(target).copy().append("\n" + target.first().dimension().location() + "\n" + target.first().pos().toShortString());
                if (target.area()) tip.append("\n" + target.second().toShortString());
                else if (target.side() != null) tip.append("\n").append(Component.translatable("gui.lyycore.spacetime_locator.side." + target.side().getName()));
                targets.get(row).setTooltip(Tooltip.create(tip));
                deletes.get(row).setTooltip(Tooltip.create(Component.translatable("gui.lyycore.spacetime_coordinates.remove")));
            }
        }
        channelPrevious.active = channelPage > 0; channelNext.active = (channelPage + 1) * PAGE_SIZE < s.channels().size();
        targetPrevious.active = targetPage > 0; targetNext.active = (targetPage + 1) * PAGE_SIZE < s.targets().size();
        input.selected = !s.output(); output.selected = s.output();
        mode.setMessage(text(s.area() ? "area_mode" : "block_mode"));
        mode.active = !s.output();
    }
    private static Component name(Target target) {
        return target.area() ? text("region") : target.state() < 0 ? Component.translatable("gui.lyycore.spacetime_coordinates.unloaded")
                : Block.stateById(target.state()).getBlock().getName();
    }
    @Override protected void containerTick() { super.containerTick(); if (displayed != menu.snapshot()) refresh(); }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        SpacetimeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 164, topPos + 31, leftPos + 165, topPos + 253, 0xFFCCDCEB);
        g.drawString(font, text("channels"), leftPos + 18, topPos + 34, SpacetimeGui.TEXT, false);
        g.drawString(font, text(menu.snapshot().output() ? "outputs" : "inputs"), leftPos + 174, topPos + 34, SpacetimeGui.TEXT, false);
        ForgeGui.centeredText(g, font, page(channelPage, menu.snapshot().channels().size()), leftPos + 87, topPos + 163, SpacetimeGui.TEXT);
        ForgeGui.centeredText(g, font, page(targetPage, menu.snapshot().targets().size()), leftPos + 265, topPos + 163, SpacetimeGui.TEXT);
        if (selected != null) {
            g.drawString(font, font.plainSubstrByWidth(selected.first().dimension().location().toString(), 138), leftPos + 18, topPos + 212, 0xFF8392A6, false);
            g.drawString(font, selected.first().pos().toShortString(), leftPos + 18, topPos + 228, SpacetimeGui.TEXT, false);
            if (selected.area()) g.drawString(font, selected.second().toShortString(), leftPos + 18, topPos + 244, SpacetimeGui.TEXT, false);
        }
        g.fill(leftPos + 174, topPos + 183, leftPos + 357, topPos + 257, 0xCFE9F2FA);
        g.drawString(font, Component.translatable("gui.lyycore.spacetime_coordinates.preview"), leftPos + 179, topPos + 188, SpacetimeGui.TEXT, false);
        if (selected != null && selected.area()) {
            renderPreview(g, selected.first(), leftPos + 175, 89);
            g.fill(leftPos + 265, topPos + 202, leftPos + 266, topPos + 255, 0xFFCCDCEB);
            renderPreview(g, GlobalPos.of(selected.first().dimension(), selected.second()), leftPos + 267, 89);
            g.drawString(font, "1", leftPos + 179, topPos + 202, SpacetimeGui.TEXT, false);
            g.drawString(font, "2", leftPos + 271, topPos + 202, SpacetimeGui.TEXT, false);
        } else renderPreview(g, selected == null ? null : selected.first(), leftPos + 175, 181);
    }
    private static String page(int page, int size) { return (page + 1) + "/" + Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE); }
    private void renderPreview(GuiGraphics g, GlobalPos address, int x, int width) {
        var preview = address == null ? null : menu.preview(address);
        if (preview != null && preview.loaded() && preview.states().size() == 125)
            SpacetimeBlockPreview.render(g, x, topPos + 201, width, 55, preview);
        else ForgeGui.centeredText(g, font, Component.translatable("gui.lyycore.spacetime_coordinates."
                + (address == null ? "select_target" : preview == null ? "loading" : "unloaded")), x + width / 2, topPos + 224, SpacetimeGui.TEXT);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { ForgeGui.centeredText(g, font, title, imageWidth / 2, 13, SpacetimeGui.TEXT); }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = targetPage * PAGE_SIZE + row;
            if (index >= menu.snapshot().targets().size()) break;
            var target = menu.snapshot().targets().get(index);
            var item = target.area() ? Items.HOPPER : target.state() < 0 ? Items.COMPASS : Block.stateById(target.state()).getBlock().asItem();
            if (item != Items.AIR) g.renderItem(new ItemStack(item), leftPos + 177, topPos + 50 + row * 21);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
