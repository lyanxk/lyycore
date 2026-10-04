package org.lyy.lyycore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.content.skills.StyleSystem;

import java.util.List;

/** Four crystal letter glyphs; style selection is owned and synchronized by StyleSystem. */
public final class StyleHud {
    private static final ResourceLocation LETTERS = ResourceLocation.parse("lyycore:textures/gui/style_letters.png");
    private record Glyph(StyleSystem.Style style, int u, int v, int x, int y) { }
    private static final List<Glyph> GLYPHS = List.of(
            new Glyph(StyleSystem.Style.BUILDING, 0, 0, -25, 0),
            new Glyph(StyleSystem.Style.MOBILITY, 1, 0, 0, -25),
            new Glyph(StyleSystem.Style.TECHNIQUE, 0, 1, 0, 25),
            new Glyph(StyleSystem.Style.OFFENSE, 1, 1, 25, 0));
    private static StyleSystem.Style selected, previous;
    private static float changedAt;
    private static net.minecraft.client.player.LocalPlayer observedPlayer;

    private StyleHud() { }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player != observedPlayer) {
            observedPlayer = mc.player;
            selected = previous = null;
        }
        if (mc.player == null || !SkillSystem.unlocked(mc.player)) {
            selected = previous = null;
            return;
        }
        if (mc.options.hideGui || mc.player.isSpectator() || mc.screen != null) return;
        var style = StyleSystem.current(mc.player);
        float now = mc.player.tickCount + delta.getGameTimeDeltaPartialTick(true);
        if (style != selected) {
            previous = selected;
            selected = style;
            changedAt = now;
        }
        float blend = previous == null ? 1 : Mth.clamp((now - changedAt) / 4, 0, 1);
        blend = blend * blend * (3 - 2 * blend);
        int centerX = 52, centerY = graphics.guiHeight() - 76;

        // A small crystal connects the four glyphs while keeping the world visible.
        for (int row = -4; row <= 4; row++) {
            int halfWidth = 4 - Math.abs(row);
            graphics.fill(centerX - halfWidth, centerY + row, centerX + halfWidth + 1, centerY + row + 1, 0xC0EBA4CD);
        }
        graphics.fill(centerX - 1, centerY - 2, centerX + 1, centerY + 1, 0xFFFFF2FC);
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (Glyph glyph : GLYPHS) {
            float emphasis = glyph.style == selected ? blend : glyph.style == previous ? 1 - blend : 0;
            int size = Math.round(Mth.lerp(emphasis, 32, 42));
            RenderSystem.setShaderColor(Mth.lerp(emphasis, 0.72F, 1), Mth.lerp(emphasis, 0.73F, 1),
                    Mth.lerp(emphasis, 0.80F, 1), Mth.lerp(emphasis, 0.8F, 1));
            // Treat the atlas as a 2x2 UV grid, independent of the source image resolution.
            graphics.blit(LETTERS, centerX + glyph.x - size / 2, centerY + glyph.y - size / 2, size, size,
                    glyph.u, glyph.v, 1, 1, 2, 2);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
