package org.lyy.lyycore.client.screen;

import java.util.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.menu.SpacetimeCoordinatesMenu;
import org.lyy.lyycore.network.PowerChannelNetwork;
import org.lyy.lyycore.network.PowerChannelNetwork.Action;

/** Channel lists are server filtered; the selected cube is a cached static model snapshot. */
public final class SpacetimeCoordinatesScreen extends AbstractContainerScreen<SpacetimeCoordinatesMenu> {
    private static final int PAGE_SIZE = 5;
    private final List<SpacetimeButton> channels = new ArrayList<>(), targets = new ArrayList<>(), deletes = new ArrayList<>();
    private SpacetimeButton channelPrevious, channelNext, targetPrevious, targetNext, copy;
    private int channelPage, targetPage;
    private boolean copying;
    private GlobalPos selected;
    private UUID lastBinding = PowerChannelNetwork.NONE;
    public SpacetimeCoordinatesScreen(SpacetimeCoordinatesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 378; imageHeight = 270;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.lyycore.spacetime_coordinates." + key, args); }
    private SpacetimeButton button(int x, int y, int width, Component label, net.minecraft.client.gui.components.Button.OnPress action) {
        return addRenderableWidget(new SpacetimeButton(leftPos + x, topPos + y, width, label, action));
    }
    @Override protected void init() {
        super.init(); channels.clear(); targets.clear(); deletes.clear();
        copy = button(132, 28, 24, Component.literal("+"), b -> { copying = !copying; channelPage = 0; refresh(); });
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int row = i;
            channels.add(button(18, 49 + i * 21, 138, Component.empty(), b -> {
                int index = channelPage * PAGE_SIZE + row;
                var choices = channelChoices();
                if (index < choices.size()) {
                    send(copying ? Action.COPY : Action.BIND, choices.get(index).id(), -1);
                    copying = false; channelPage = 0; refresh();
                }
            }));
            targets.add(button(174, 49 + i * 21, 156, Component.empty(), b -> select(targetPage * PAGE_SIZE + row)));
            deletes.add(button(333, 49 + i * 21, 24, Component.literal("×"), b -> send(Action.REMOVE, menu.snapshot().bound(), targetPage * PAGE_SIZE + row)));
        }
        channelPrevious = button(18, 158, 24, Component.literal("<"), b -> { channelPage--; refresh(); });
        channelNext = button(132, 158, 24, Component.literal(">"), b -> { channelPage++; refresh(); });
        targetPrevious = button(174, 158, 24, Component.literal("<"), b -> { targetPage--; refresh(); });
        targetNext = button(333, 158, 24, Component.literal(">"), b -> { targetPage++; refresh(); });
        refresh();
    }
    private void send(Action action, UUID channel, int target) {
        var address = target >= 0 && target < menu.snapshot().targets().size() ? menu.snapshot().targets().get(target).address() : null;
        PacketDistributor.sendToServer(new PowerChannelNetwork.Request(menu.containerId, action, channel, "", false, address));
    }
    private List<PowerChannelNetwork.Channel> channelChoices() {
        return copying ? menu.snapshot().channels().stream().filter(channel -> !channel.id().equals(menu.snapshot().bound())).toList()
                : menu.snapshot().channels();
    }
    private void select(int index) {
        if (index < 0 || index >= menu.snapshot().targets().size()) return;
        selected = menu.snapshot().targets().get(index).address();
        if (menu.preview(selected) == null) send(Action.PREVIEW, menu.snapshot().bound(), index);
        refresh();
    }
    private void refresh() {
        var snapshot = menu.snapshot();
        if (!lastBinding.equals(snapshot.bound())) { selected = null; targetPage = 0; copying = false; lastBinding = snapshot.bound(); }
        if (selected != null && snapshot.targets().stream().noneMatch(t -> t.address().equals(selected))) selected = null;
        var choices = channelChoices();
        channelPage = Math.clamp(channelPage, 0, Math.max(0, (choices.size() - 1) / PAGE_SIZE));
        targetPage = Math.clamp(targetPage, 0, Math.max(0, (snapshot.targets().size() - 1) / PAGE_SIZE));
        for (int row = 0; row < PAGE_SIZE; row++) {
            int c = channelPage * PAGE_SIZE + row, t = targetPage * PAGE_SIZE + row;
            var channel = c < choices.size() ? choices.get(c) : null;
            channels.get(row).visible = channel != null;
            if (channel != null) {
                channels.get(row).selected = channel.id().equals(snapshot.bound());
                channels.get(row).setMessage(Component.literal(font.plainSubstrByWidth(channel.name(), 126)));
                channels.get(row).setTooltip(Tooltip.create(copying ? text("copy_hint", channel.name())
                        : Component.literal(channel.name()).append("\n").append(text(channel.publicChannel() ? "public" : "private"))));
            }
            var target = t < snapshot.targets().size() ? snapshot.targets().get(t) : null;
            targets.get(row).visible = deletes.get(row).visible = target != null;
            if (target != null) {
                targets.get(row).selected = target.address().equals(selected);
                targets.get(row).setMessage(Component.literal("    " + font.plainSubstrByWidth(targetName(target).getString(), 117)));
                targets.get(row).setTooltip(Tooltip.create(targetName(target).copy().append("\n" + target.address().dimension().location() + "\n" + target.address().pos().toShortString())));
                deletes.get(row).setTooltip(Tooltip.create(text("remove")));
            }
        }
        channelPrevious.active = channelPage > 0; channelNext.active = (channelPage + 1) * PAGE_SIZE < choices.size();
        targetPrevious.active = targetPage > 0; targetNext.active = (targetPage + 1) * PAGE_SIZE < snapshot.targets().size();
        copy.active = !snapshot.bound().equals(PowerChannelNetwork.NONE) && snapshot.channels().size() > 1;
        copy.selected = copying; copy.setMessage(Component.literal(copying ? "×" : "+"));
        copy.setTooltip(Tooltip.create(copying ? Component.translatable("gui.cancel") : text("copy")));
    }
    private static Component targetName(PowerChannelNetwork.Target target) {
        return target.state() < 0 ? text("unloaded") : Block.stateById(target.state()).getBlock().getName();
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        SpacetimeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 164, topPos + 31, leftPos + 165, topPos + 253, 0xFFCCDCEB);
        g.drawString(font, text(copying ? "copy_destination" : "channels"), leftPos + 18, topPos + 34, SpacetimeGui.TEXT, false);
        g.drawString(font, text(menu.snapshot().bound().equals(PowerChannelNetwork.NONE) ? "pending_targets" : "targets", menu.snapshot().targets().size()), leftPos + 174, topPos + 34, SpacetimeGui.TEXT, false);
        ForgeGui.centeredText(g, font, (channelPage + 1) + "/" + Math.max(1, (channelChoices().size() + 4) / 5), leftPos + 87, topPos + 163, SpacetimeGui.TEXT);
        ForgeGui.centeredText(g, font, (targetPage + 1) + "/" + Math.max(1, (menu.snapshot().targets().size() + 4) / 5), leftPos + 265, topPos + 163, SpacetimeGui.TEXT);
        var bound = menu.snapshot().selected();
        g.drawString(font, font.plainSubstrByWidth(bound == null ? text("pending").getString() : bound.name(), 138), leftPos + 18, topPos + 205, SpacetimeGui.TEXT, false);
        if (selected != null) {
            g.drawString(font, text("position"), leftPos + 18, topPos + 219, SpacetimeGui.TEXT, false);
            g.drawString(font, selected.pos().toShortString(), leftPos + 18, topPos + 233, SpacetimeGui.TEXT, false);
            g.drawString(font, font.plainSubstrByWidth(selected.dimension().location().toString(), 138), leftPos + 18, topPos + 247, 0xFF8392A6, false);
        }
        g.fill(leftPos + 174, topPos + 183, leftPos + 357, topPos + 257, 0xCFE9F2FA);
        g.drawString(font, text("preview"), leftPos + 179, topPos + 188, SpacetimeGui.TEXT, false);
        renderPreview(g);
    }
    private void renderPreview(GuiGraphics g) {
        var preview = selected == null ? null : menu.preview(selected);
        if (preview == null || !preview.loaded() || preview.states().size() != 125) {
            ForgeGui.centeredText(g, font, text(selected == null ? "select_target" : preview == null ? "loading" : "unloaded"), leftPos + 265, topPos + 224, SpacetimeGui.TEXT);
            return;
        }
        // Only baked block models are rendered: no level replacement, ticking, entities or BE renderers.
        g.flush();
        g.enableScissor(leftPos + 175, topPos + 201, leftPos + 356, topPos + 256);
        var pose = g.pose(); pose.pushPose();
        try {
            Lighting.setupFor3DItems();
            pose.translate(leftPos + 265, topPos + 230, 150);
            pose.scale(7, -7, 7); pose.mulPose(Axis.XP.rotationDegrees(25)); pose.mulPose(Axis.YP.rotationDegrees(135));
            for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
                var state = Block.stateById(preview.states().get(y * 25 + z * 5 + x));
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                pose.pushPose(); pose.translate(x - 2.5, y - 2.5, z - 2.5);
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, g.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            g.flush();
        } finally {
            pose.popPose(); Lighting.setupFor3DItems(); g.disableScissor();
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { ForgeGui.centeredText(g, font, title, imageWidth / 2, 13, SpacetimeGui.TEXT); }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        // Icons are drawn after the row widgets so their backgrounds cannot cover them.
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = targetPage * PAGE_SIZE + row;
            if (index >= menu.snapshot().targets().size()) break;
            var target = menu.snapshot().targets().get(index);
            var item = target.state() < 0 ? Items.COMPASS : Block.stateById(target.state()).getBlock().asItem();
            if (item != Items.AIR) g.renderItem(new ItemStack(item), leftPos + 177, topPos + 50 + row * 21);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
