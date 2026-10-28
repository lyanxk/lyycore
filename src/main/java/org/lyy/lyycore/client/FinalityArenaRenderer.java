package org.lyy.lyycore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lyy.lyycore.LyyCore;

/** One full-screen triangle, with a world-space floor and sky evaluated in the fragment shader. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class FinalityArenaRenderer {
    private static final VertexFormat FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).build();
    private static ShaderInstance shader;
    private static VertexBuffer triangle;
    private static final RenderType TYPE = RenderType.create("lyycore_finality_arena", FORMAT,
            VertexFormat.Mode.TRIANGLES, 256, false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private FinalityArenaRenderer() {}

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws IOException {
        if (triangle != null) {
            triangle.close();
            triangle = null;
        }
        event.registerShader(new ShaderInstance(event.getResourceProvider(), FinalityArenaEffects.ID, FORMAT),
                loaded -> shader = loaded);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(FinalityArenaEffects.ID)
                || shader == null || ShaderPackCompat.shadowPass()) return;
        // Iris replaces unknown core shaders during its world passes. Use the existing
        // post-composite path, testing against world/hand depth, when a pack is active.
        var stage = ShaderPackCompat.active() ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_SKY;
        if (event.getStage() != stage) return;
        if (triangle == null) triangle = uploadTriangle();

        var camera = event.getCamera().getPosition();
        var viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        var previousShader = RenderSystem.getShader();
        TYPE.setupRenderState();
        try {
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f(viewProjection).invert());
            shader.safeGetUniform("ViewProjection").set(viewProjection);
            shader.safeGetUniform("CameraPosition").set((float) camera.x, (float) camera.y, (float) camera.z);
            shader.safeGetUniform("FloorHeight").set(FinalityArenaEffects.FLOOR_HEIGHT);
            // Reduce in double precision before conversion, including in old worlds.
            double seconds = ((double) minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20.0;
            shader.safeGetUniform("SceneTime").set((float) (seconds % 4096.0));
            triangle.bind();
            shader.apply();
            triangle.draw();
        } finally {
            shader.clear();
            VertexBuffer.unbind();
            TYPE.clearRenderState();
            RenderSystem.setShader(() -> previousShader);
        }
    }

    private static VertexBuffer uploadTriangle() {
        try (var memory = new ByteBufferBuilder(256)) {
            var builder = new BufferBuilder(memory, VertexFormat.Mode.TRIANGLES, FORMAT);
            builder.addVertex(-1, -1, 0);
            builder.addVertex(3, -1, 0);
            builder.addVertex(-1, 3, 0);
            var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(builder.buildOrThrow());
            VertexBuffer.unbind();
            return buffer;
        }
    }
}
