package org.lyy.lyycore.client.screen;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.logistics.LogisticsChannels;
import org.lyy.lyycore.content.menu.LogisticsMenu;
import org.lyy.lyycore.network.LogisticsNetwork;
import org.lyy.lyycore.network.LogisticsNetwork.Action;

/** The matrix edits its five channels; all three ports share the paged binding view. */
public final class LogisticsScreen extends AbstractContainerScreen<LogisticsMenu> {
    private static final int PAPER = 0xFFFFFBFD, PINK = 0xFFE9A2C7, INK = 0xFF593F50;
    private static final int MUTED = 0xFF97818E, LINE = 0xFFEED7E3, HOVER = 0xFFFBE9F2;
    private final List<ChannelButton> rows = new ArrayList<>(), deletes = new ArrayList<>();
    private EditBox name;
    private Button previous, next, unbind, accept, cancel, visibility;
    private UUID editing;
    private boolean deleting, publicChannel;
    public LogisticsScreen(LogisticsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 248; imageHeight = 278;
        inventoryLabelX = 44; inventoryLabelY = 182;
    }
    private static Component text(String key, Object... arguments) { return Component.translatable("gui.lyycore.logistics." + key, arguments); }
    private ChannelButton button(Component title, int x, int y, int width, Button.OnPress action) {
        return addRenderableWidget(new ChannelButton(title, x, y, width, action));
    }
    private final class ChannelButton extends Button {
        private LogisticsNetwork.Entry channel;
        private boolean bound;
        private ChannelButton(Component title, int x, int y, int width, OnPress action) {
            super(leftPos + x, topPos + y, width, 18, title, action, DEFAULT_NARRATION);
        }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY();
            boolean highlighted = active && isHoveredOrFocused();
            g.fill(x, y, x + width, y + height, bound || highlighted ? PINK : LINE);
            g.fill(x + 1, y + 1, x + width - 1, y + height - 1, bound || highlighted ? HOVER : PAPER);
            if (channel == null) {
                ForgeGui.centeredText(g, font, getMessage(), x + width / 2, y + 5, active ? INK : MUTED);
                return;
            }
            if (bound) g.fill(x + 1, y + 2, x + 3, y + height - 2, PINK);
            var badge = text(channel.publicChannel() ? "public_short" : "private_short");
            int badgeWidth = font.width(badge) + 10;
            int badgeX = x + width - badgeWidth - 5;
            g.fill(badgeX, y + 3, x + width - 5, y + height - 3, channel.publicChannel() ? 0xFFF1E2F2 : 0xFFF6E8EE);
            ForgeGui.centeredText(g, font, badge, badgeX + badgeWidth / 2, y + 5, INK);
            g.drawString(font, font.plainSubstrByWidth(channel.name(), width - badgeWidth - 20), x + 7, y + 5, INK, false);
        }
    }
    private static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, PINK);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, PAPER);
        for (int dx : new int[]{4, width - 7}) for (int dy : new int[]{4, height - 7})
            g.fill(x + dx, y + dy, x + dx + 3, y + dy + 3, LINE);
    }
    @Override protected void init() {
        super.init(); rows.clear(); deletes.clear();
        for (int i = 0; i < LogisticsMenu.PAGE_SIZE; i++) {
            final int row = i;
            rows.add(button(Component.empty(), 16, 28 + i * 22, menu.matrix() ? 190 : 216, b -> select(row)));
            if (menu.matrix()) deletes.add(button(Component.literal("×"), 211, 28 + i * 22, 21, b -> confirmDelete(row)));
        }
        if (!menu.matrix()) {
            previous = button(Component.literal("<"), 16, 140, 22, b -> send(Action.PAGE, LogisticsNetwork.NONE, "", menu.snapshot().page() - 1));
            next = button(Component.literal(">"), 210, 140, 22, b -> send(Action.PAGE, LogisticsNetwork.NONE, "", menu.snapshot().page() + 1));
            unbind = button(text("unbind"), 168, 160, 64, b -> send(Action.BIND, LogisticsNetwork.NONE, "", 0));
        }
        name = addRenderableWidget(new EditBox(font, leftPos + 35, topPos + 72, 178, 12, text("name")));
        name.setMaxLength(LogisticsChannels.NAME_LIMIT);
        name.setBordered(false);
        name.setTextColor(INK);
        name.setTextColorUneditable(MUTED);
        name.setTextShadow(false);
        accept = button(text("save"), 51, 117, 68, b -> submit());
        cancel = button(Component.translatable("gui.cancel"), 129, 117, 68, b -> closeDialog());
        visibility = button(text("private"), 29, 91, 190, b -> { publicChannel = !publicChannel; refresh(); });
        editing = null; refresh();
    }
    private void send(Action action, UUID channel, String value, int page) {
        PacketDistributor.sendToServer(new LogisticsNetwork.Request(menu.containerId, action, channel, value, page, publicChannel));
    }
    private void select(int row) {
        var entries = menu.snapshot().entries();
        var entry = row < entries.size() ? entries.get(row) : null;
        if (!menu.matrix()) { if (entry != null) send(Action.BIND, entry.id(), "", 0); return; }
        if (entry != null && !entry.editable() || entry == null && !menu.snapshot().canCreate()) return;
        publicChannel = entry != null && entry.publicChannel();
        deleting = false; editing = entry == null ? LogisticsNetwork.NONE : entry.id();
        name.setValue(entry == null ? "" : entry.name()); refresh(); setFocused(name); name.setFocused(true);
    }
    private void confirmDelete(int row) {
        var entries = menu.snapshot().entries();
        if (row >= entries.size() || !entries.get(row).editable()) return;
        editing = entries.get(row).id(); deleting = true; refresh(); setFocused(accept);
    }
    private void submit() {
        if (editing == null || !deleting && name.getValue().isBlank()) return;
        send(deleting ? Action.DELETE : editing.equals(LogisticsNetwork.NONE) ? Action.CREATE : Action.RENAME, editing, deleting ? "" : name.getValue(), 0);
        closeDialog();
    }
    private void closeDialog() { editing = null; deleting = false; name.setFocused(false); setFocused(null); refresh(); }
    private void refresh() {
        var snapshot = menu.snapshot();
        for (int i = 0; i < rows.size(); i++) {
            var entry = i < snapshot.entries().size() ? snapshot.entries().get(i) : null;
            boolean create = menu.matrix() && snapshot.canCreate() && i == snapshot.entries().size();
            var row = rows.get(i);
            row.visible = editing == null;
            row.active = entry == null ? create : !menu.matrix() || entry.editable();
            row.channel = entry;
            row.bound = entry != null && entry.id().equals(snapshot.bound());
            row.setMessage(entry == null ? (create ? text("create") : text("empty_slot")) : Component.literal(entry.name()));
            if (entry != null) row.setTooltip(Tooltip.create(Component.literal(entry.name() + "\n" + text(entry.publicChannel() ? "public" : "private").getString() + "\n" + entry.source().dimension().location()
                    + "\n" + entry.source().pos().toShortString())));
            else row.setTooltip(null);
            if (menu.matrix()) { deletes.get(i).visible = editing == null && entry != null; deletes.get(i).active = entry != null && entry.editable(); }
        }
        if (!menu.matrix()) {
            previous.active = snapshot.page() > 0; next.active = snapshot.page() + 1 < snapshot.pages();
            unbind.active = snapshot.hasBinding();
        }
        name.visible = visibility.visible = editing != null && !deleting;
        visibility.setMessage(text(publicChannel ? "public" : "private"));
        accept.visible = cancel.visible = editing != null;
        accept.active = deleting || !name.getValue().isBlank();
        accept.setMessage(deleting ? text("delete") : text("save"));
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 12, topPos + 21, leftPos + 236, topPos + 22, PINK);
        g.fill(leftPos + 12, topPos + 180, leftPos + 236, topPos + 181, LINE);
        for (var slot : menu.slots) {
            int sx = leftPos + slot.x, sy = topPos + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFFE9B4CB);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFFF8EDF2);
        }
        if (editing != null) {
            panel(g, leftPos + 20, topPos + 38, 208, 109);
            var message = deleting ? text("confirm_delete") : text("name");
            ForgeGui.centeredText(g, font, message, leftPos + 124, topPos + 49, INK);
            if (!deleting) {
                g.fill(leftPos + 29, topPos + 66, leftPos + 219, topPos + 86, name.isFocused() ? PINK : LINE);
                g.fill(leftPos + 30, topPos + 67, leftPos + 218, topPos + 85, PAPER);
            }
            if (deleting) ForgeGui.centeredText(g, font, text("delete_warning"), leftPos + 124, topPos + 75, INK);
        } else if (menu.matrix()) {
            ForgeGui.centeredText(g, font, text("limit", LogisticsChannels.CHANNEL_LIMIT), leftPos + 124, topPos + 147, INK);
        } else {
            var snapshot = menu.snapshot();
            ForgeGui.centeredText(g, font, (snapshot.page() + 1) + " / " + snapshot.pages(), leftPos + 124, topPos + 144, INK);
            var bound = snapshot.boundName().isEmpty() ? text(snapshot.hasBinding() ? "private_bound" : "unbound") : Component.literal(snapshot.boundName());
            g.drawString(font, font.plainSubstrByWidth(bound.getString(), 67), leftPos + 16, topPos + 164, INK, false);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        ForgeGui.centeredText(g, font, title, imageWidth / 2, 8, INK);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) { super.render(g, x, y, partial); if (editing == null) renderTooltip(g, x, y); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (editing == null) return super.mouseClicked(x, y, button);
        if (name.visible && name.mouseClicked(x, y, button)) { setFocused(name); return true; }
        if (visibility.visible && visibility.mouseClicked(x, y, button)) return true;
        if (accept.mouseClicked(x, y, button) || cancel.mouseClicked(x, y, button)) return true;
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (editing != null) {
            if (key == 256) { closeDialog(); return true; }
            if (key == 257 || key == 335) { submit(); return true; }
            return name.visible && name.keyPressed(key, scan, modifiers);
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
