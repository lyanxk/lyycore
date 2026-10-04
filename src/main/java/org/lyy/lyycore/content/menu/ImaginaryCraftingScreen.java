package org.lyy.lyycore.content.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ImaginaryCraftingScreen extends AbstractContainerScreen<ImaginaryCraftingMenu> {
    // JEI uses the connector between the center and right ingredient slots.
    public static final int RECIPE_X = 99, RECIPE_Y = 65, RECIPE_W = 22, RECIPE_H = 10;
    private float animationProgress;

    public ImaginaryCraftingScreen(ImaginaryCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ImaginaryCraftingMenu.WIDTH;
        imageHeight = ImaginaryCraftingMenu.HEIGHT;
        inventoryLabelY = ImaginaryCraftingMenu.INVENTORY_LABEL_Y;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        animationProgress = menu.progress() == 0 ? 0
                : Math.clamp((menu.progress() + partial) / menu.duration(), 0, 1);
        ImaginaryCraftingGui.diagram(g, leftPos, topPos, animationProgress);
        if (menu.condensing()) {
            g.fill(leftPos + 155, topPos + 20, leftPos + 163, topPos + 120, ForgeGui.TRACK);
            int height = Math.round(100F * menu.storedEnergy() / 100000);
            g.fillGradient(leftPos + 156, topPos + 120 - height, leftPos + 162, topPos + 120, 0xFFFFD9ED, ForgeGui.ACCENT);
        }
        for (int index = 9; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            ForgeGui.slot(g, leftPos + slot.x, topPos + slot.y, false);
        }
        g.fill(leftPos + 8, topPos + 128, leftPos + 168, topPos + 132, ForgeGui.TRACK);
        int filled = Math.round(160 * animationProgress);
        if (filled > 0) {
            g.fillGradient(leftPos + 8, topPos + 128, leftPos + 8 + filled, topPos + 132,
                    0xFFFFD9ED, ForgeGui.ACCENT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 160), titleLabelX, titleLabelY, ForgeGui.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ForgeGui.TEXT, false);
        Component status = menu.progress() == 0
                ? Component.translatable("screen.lyycore.imaginary_crafting.ready")
                : Component.translatable("screen.lyycore.imaginary_crafting.progress",
                        menu.progress() * 100 / menu.duration());
        g.drawString(font, status, (imageWidth - font.width(status)) / 2, 135, ForgeGui.TEXT, false);
    }

    @Override
    protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, String countString) {
        if (menu.progress() > 0 && slot.index > 0 && slot.index < 9) {
            int[] center = ImaginaryCraftingMenu.POSITIONS[0];
            int x = Math.round(slot.x + (center[0] - slot.x) * animationProgress);
            int y = Math.round(slot.y + (center[1] - slot.y) * animationProgress);
            g.renderItem(stack, x, y);
        } else {
            super.renderSlotContents(g, stack, slot, countString);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (menu.condensing() && isHovering(153, 18, 12, 104, mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("screen.lyycore.condensing.energy", menu.storedEnergy()),
                    Component.translatable("screen.lyycore.condensing.cost", menu.energyCost())), mouseX, mouseY);
        } else if (isHovering(8, 128, 160, 18, mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("screen.lyycore.imaginary_crafting.time", menu.duration() / 20),
                    Component.translatable("screen.lyycore.imaginary_crafting.instructions"),
                    Component.translatable("screen.lyycore.imaginary_crafting.returns_to_center")), mouseX, mouseY);
        } else if (menu.progress() == 0 || hoveredSlot == null || hoveredSlot.index >= 9) {
            // Ingredients move away from their slots during crafting; do not show stale item tooltips.
            renderTooltip(g, mouseX, mouseY);
        }
    }
}
