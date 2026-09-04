package org.lyy.lyycore.content.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.lyycore.LyyCore;

public class IAFScreen extends AbstractContainerScreen<IAFMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "textures/gui/imaginary_alloy_forge.png");

    private static final int ENERGY_U = 176, ENERGY_V = 16, ENERGY_W = 12, ENERGY_H = 48;
    private static final int ENERGY_X = 10, ENERGY_Y = 20;
    private static final int PROGRESS_X = 91, PROGRESS_Y = 47, PROGRESS_W = 29, PROGRESS_H = 11;

    public IAFScreen(IAFMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 10000;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        super.render(gg, mouseX, mouseY, partialTicks);
        this.renderTooltip(gg, mouseX, mouseY);

        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            int e = menu.getEnergy(), max = Math.max(menu.getMaxEnergy(), 1);
            gg.renderTooltip(this.font, Component.translatable("tooltip." + LyyCore.MODID + ".energy",
                    e, max), mouseX, mouseY);
        }
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.translatable("tooltip." + LyyCore.MODID + ".progress",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gg.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 188, 166);

        int e = menu.getEnergy();
        int emax = Math.max(menu.getMaxEnergy(), 1);
        int h = (int) Math.round((e / (double) emax) * ENERGY_H);
        if (h > 0) {
            gg.blit(BG,
                    this.leftPos + ENERGY_X,
                    this.topPos + ENERGY_Y + (ENERGY_H - h),
                    ENERGY_U,
                    ENERGY_V + (ENERGY_H - h),
                    ENERGY_W,
                    h,
                    188,
                    166);
        }

        int maxProgress = menu.getMaxProgress();
        int progressWidth = maxProgress <= 0 ? 0
                : (int) Math.round(menu.getProgress() / (double) maxProgress * (PROGRESS_W - 2));
        gg.fill(this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y,
                this.leftPos + PROGRESS_X + PROGRESS_W, this.topPos + PROGRESS_Y + PROGRESS_H, 0xFF32172D);
        gg.fill(this.leftPos + PROGRESS_X + 1, this.topPos + PROGRESS_Y + 1,
                this.leftPos + PROGRESS_X + 1 + progressWidth,
                this.topPos + PROGRESS_Y + PROGRESS_H - 1, 0xFFF0A1CE);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }
}
