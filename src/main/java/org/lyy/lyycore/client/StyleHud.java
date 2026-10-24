package org.lyy.lyycore.client;

import org.lyy.lyycore.content.skills.BasicSkills;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.content.skills.SkillSettings;
import org.lyy.lyycore.content.skills.StyleSystem;

import java.util.List;

/** Four crystal letter glyphs; style selection is owned and synchronized by StyleSystem. */
public final class StyleHud {
    private static final ResourceLocation LETTERS = ResourceLocation.parse("lyycore:textures/gui/style_letters.png");
    private record Glyph(StyleSystem.Style style, int u, int v, int x, int y) { }
    private static final Glyph TECHNIQUE = new Glyph(StyleSystem.Style.TECHNIQUE, 0, 1, 0, 25);
    private static final List<Glyph> GLYPHS = List.of(
            new Glyph(StyleSystem.Style.BUILDING, 0, 0, -25, 0),
            new Glyph(StyleSystem.Style.MOBILITY, 1, 0, 0, -25),
            TECHNIQUE,
            new Glyph(StyleSystem.Style.OFFENSE, 1, 1, 25, 0));
    private static StyleSystem.Style selected, previous;
    private static double changedAt, shownAt;
    private static float shownFrom;
    private static SkillSettings.HudMode displayMode;
    private static net.minecraft.client.player.LocalPlayer observedPlayer;

    private StyleHud() { }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player != observedPlayer) {
            observedPlayer = mc.player;
            selected = previous = null;
            displayMode = null;
        }
        if (mc.player == null || !SkillSystem.enabled(mc.player)) {
            selected = previous = null;
            displayMode = null;
            return;
        }
        var style = StyleSystem.current(mc.player);
        var mode = SkillSettings.hudMode(mc.player);
        double now = (double) mc.player.tickCount + delta.getGameTimeDeltaPartialTick(true);
        if (style != selected || mode != displayMode) {
            shownFrom = selected == null || mode != displayMode ? 0 : visibility(now);
            shownAt = now;
            displayMode = mode;
        }
        if (style != selected) {
            previous = selected;
            selected = style;
            changedAt = now;
        }
        float opacity = visibility(now);
        if (opacity < .02F || mc.options.hideGui || mc.player.isSpectator() || mc.screen != null) return;
        float blend = previous == null ? 1 : (float) Mth.clamp((now - changedAt) / 4, 0, 1);
        blend = blend * blend * (3 - 2 * blend);
        int centerX = 52, centerY = 52;

        if (style == StyleSystem.Style.TECHNIQUE && BasicSkills.available(mc.player)) {
            int guard = org.lyy.lyycore.content.skills.GuardSkill.value(mc.player);
            int guardX = centerX + TECHNIQUE.x + 1, guardY = centerY + TECHNIQUE.y - 2;
            drawGuardArc(graphics, guardX, guardY, 100, faded(0x88524C62, opacity));
            if (guard > 0) drawGuardArc(graphics, guardX, guardY, guard, faded(0xFFFFD4EE, opacity));
            graphics.drawCenteredString(mc.font, Integer.toString(guard), guardX, guardY + 26, faded(0xFFFFE5F4, opacity));
        }
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (Glyph glyph : GLYPHS) {
            float emphasis = glyph.style == selected ? blend : glyph.style == previous ? 1 - blend : 0;
            int size = Math.round(Mth.lerp(emphasis, 32, 42));
            RenderSystem.setShaderColor(Mth.lerp(emphasis, 0.72F, 1), Mth.lerp(emphasis, 0.73F, 1),
                    Mth.lerp(emphasis, 0.80F, 1), Mth.lerp(emphasis, 0.8F, 1) * opacity);
            // Treat the atlas as a 2x2 UV grid, independent of the source image resolution.
            graphics.blit(LETTERS, centerX + glyph.x - size / 2, centerY + glyph.y - size / 2, size, size,
                    glyph.u, glyph.v, 1, 1, 2, 2);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static float visibility(double now) {
        if (displayMode == SkillSettings.HudMode.ON) return 1;
        if (displayMode != SkillSettings.HudMode.ON_SWITCH) return 0;
        float age = (float) (now - shownAt);
        // Fade in for 0.2 s, hold for 1.5 s, then fade out for 0.5 s.
        return Mth.lerp(smooth(age / 4), shownFrom, 1) * (1 - smooth((age - 34) / 10));
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static int faded(int color, float opacity) {
        return color & 0x00FFFFFF | Math.round((color >>> 24) * opacity) << 24;
    }

    private static void drawGuardArc(GuiGraphics graphics, int centerX, int centerY, int value, int color) {
        float radius = 20, halfWidth = .65F;
        // Fade each edge across one screen pixel, independent of the GUI scale.
        float feather = (float)(1 / Minecraft.getInstance().getWindow().getGuiScale());
        int transparent = color & 0x00FFFFFF;
        drawGuardBand(graphics, centerX, centerY, value, radius - halfWidth - feather, radius - halfWidth, transparent, color);
        drawGuardBand(graphics, centerX, centerY, value, radius - halfWidth, radius + halfWidth, color, color);
        drawGuardBand(graphics, centerX, centerY, value, radius + halfWidth, radius + halfWidth + feather, color, transparent);
    }

    private static void drawGuardBand(GuiGraphics graphics, int centerX, int centerY, int value,
                                      float innerRadius, float outerRadius, int innerColor, int outerColor) {
        var vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        var pose = graphics.pose().last().pose();
        double sweep = Math.PI * 2 * value / 100;
        int segments = (int)Math.ceil(128 * value / 100.0);
        // Adjacent quads share floating-point edges instead of stamping rounded pixel squares.
        for (int i = 0; i < segments; i++) {
            double start = sweep * i / segments - Math.PI / 2;
            double end = sweep * (i + 1) / segments - Math.PI / 2;
            float startX = (float)Math.cos(start), startY = (float)Math.sin(start);
            float endX = (float)Math.cos(end), endY = (float)Math.sin(end);
            vertices.addVertex(pose, centerX + startX * outerRadius, centerY + startY * outerRadius, 0).setColor(outerColor);
            vertices.addVertex(pose, centerX + startX * innerRadius, centerY + startY * innerRadius, 0).setColor(innerColor);
            vertices.addVertex(pose, centerX + endX * innerRadius, centerY + endY * innerRadius, 0).setColor(innerColor);
            vertices.addVertex(pose, centerX + endX * outerRadius, centerY + endY * outerRadius, 0).setColor(outerColor);
        }
    }
}
