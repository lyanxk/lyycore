package org.lyy.lyycore.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/** Opt-in offscreen check of the actual Minecraft shader/mesh path. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class GrabRenderCheck {
    private static final boolean RUN = Boolean.getBoolean("lyycore.grabRenderCheck");
    private static boolean hidden, done;
    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        if (RUN && !hidden) { GLFW.glfwHideWindow(Minecraft.getInstance().getWindow().getWindow()); hidden = true; }
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        var mc = Minecraft.getInstance();
        if (!RUN || done || mc.getOverlay() != null || !(mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) return;
        done = true;
        var output = Path.of("../../rope-phases.png");
        var target = new TextureTarget(2000, 1000, true, Minecraft.ON_OSX);
        target.setClearColor(.045F, .055F, .085F, 1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().ortho(0, 2000, 0, 1000, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack(); view.pushMatrix().identity(); RenderSystem.applyModelViewMatrix();
        try {
            var model = new PinkEnergyRopeModel();
            String[] clips = {"grab_extend", "grab_wrap", "grab_wrap", "grab_pull", "bound_idle", "release", "grab_extend", "grab_wrap"};
            float[] times = {.24F, .15F, .38F, .3F, .3F, .12F, .24F, .15F};
            for (int i = 0; i < 8; i++) {
                var pose = new PoseStack(); pose.translate(290 + i % 4 * 500, i < 4 ? 760 : 270, 0);
                pose.scale(75, 75, 75); pose.mulPose(Axis.XP.rotationDegrees(20)); pose.mulPose(Axis.YP.rotationDegrees(-40));
                Vec3 caster = i < 3 ? new Vec3(-22, 24, -65) : new Vec3(-9, 19, -28);
                model.render(pose, mc.renderBuffers().bufferSource(), clips[i], times[i], caster, i >= 6 ? .35F : 1);
                mc.renderBuffers().bufferSource().endBatch();
            }
            if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("Rope render OpenGL error");
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(output); }
            Files.writeString(Path.of("../../rope-render-result.txt"), "PASS: six rope stages loaded and rendered through Minecraft\n");
        } finally {
            view.popMatrix(); RenderSystem.applyModelViewMatrix(); RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers(); mc.getMainRenderTarget().bindWrite(true); mc.stop();
        }
    }
}
