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
import org.lwjgl.opengl.GL11;

/** Procedural sky and floor; the floor uses world geometry for ordinary depth testing. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class FinalityArenaRenderer {
    private static final VertexFormat FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).build();
    private static ShaderInstance shader;
    private static VertexBuffer triangle;
    private static VertexBuffer floor;
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
        if (floor != null) {
            floor.close();
            floor = null;
        }
        event.registerShader(new ShaderInstance(event.getResourceProvider(), FinalityArenaEffects.ID, FORMAT),
                loaded -> shader = loaded);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(FinalityArenaEffects.ID)
                || shader == null || ShaderPackCompat.shadowPass()) return;
        // Draw before entities and translucent effects: they do not all write depth,
        // so a floor drawn after the world would paint over them.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (triangle == null) triangle = uploadMesh(-1, -1, 3, -1, -1, 3);
        if (floor == null) floor = uploadMesh(-1, -1, 1, -1, 1, 1, -1, -1, 1, 1, -1, 1);

        var camera = event.getCamera().getPosition();
        var viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        var previousShader = RenderSystem.getShader();
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        TYPE.setupRenderState();
        try {
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f(viewProjection).invert());
            shader.safeGetUniform("ViewProjection").set(viewProjection);
            shader.safeGetUniform("CameraPosition").set((float) camera.x, (float) camera.y, (float) camera.z);
            shader.safeGetUniform("FloorHeight").set(FinalityArenaEffects.FLOOR_HEIGHT);
            shader.safeGetUniform("FloorExtent").set(minecraft.gameRenderer.getDepthFar());
            // Reduce in double precision before conversion, including in old worlds.
            double seconds = ((double) minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20.0;
            shader.safeGetUniform("SceneTime").set((float) (seconds % 4096.0));
            RenderSystem.depthMask(false);
            draw(triangle, false);
            RenderSystem.depthMask(true);
            draw(floor, true);
        } finally {
            shader.clear();
            VertexBuffer.unbind();
            TYPE.clearRenderState();
            RenderSystem.depthMask(depthWrite);
            RenderSystem.depthFunc(depthFunction);
            if (depthTest) RenderSystem.enableDepthTest();
            RenderSystem.setShader(() -> previousShader);
        }
    }

    private static void draw(VertexBuffer buffer, boolean renderFloor) {
        shader.safeGetUniform("RenderFloor").set(renderFloor ? 1.0F : 0.0F);
        buffer.bind();
        shader.apply();
        buffer.draw();
    }

    private static VertexBuffer uploadMesh(float... positions) {
        try (var memory = new ByteBufferBuilder(256)) {
            var builder = new BufferBuilder(memory, VertexFormat.Mode.TRIANGLES, FORMAT);
            for (int i = 0; i < positions.length; i += 2) {
                builder.addVertex(positions[i], positions[i + 1], 0);
            }
            var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(builder.buildOrThrow());
            VertexBuffer.unbind();
            return buffer;
        }
    }
}
