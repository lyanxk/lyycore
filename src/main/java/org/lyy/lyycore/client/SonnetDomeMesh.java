package org.lyy.lyycore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Static dome geometry is uploaded at resource reload, then shared by every dome. */
final class SonnetDomeMesh {
    private static final VertexBuffer[] LAYERS = new VertexBuffer[SonnetDomeRenderer.MODELS.length];
    // A single textured surface is visible from both sides; no reversed geometry or overlay pass.
    private static final RenderType SHELL = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType GLOW_SHELL = RenderType.create("lyycore_dome_surface", DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS, 1536, false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeBeaconBeamShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static boolean shaderLayout;

    private SonnetDomeMesh() {}

    static void reload(ModelManager models) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> reload(models));
            return;
        }
        for (int layer = 0; layer < LAYERS.length; layer++) {
            if (LAYERS[layer] != null) LAYERS[layer].close();
            LAYERS[layer] = null;
        }
        shaderLayout = ShaderPackCompat.active();
        // Use a private allocator: do not interfere with an active world/item buffer.
        try (var memory = new ByteBufferBuilder(4 * 1024 * 1024)) {
            for (int layer = 0; layer < LAYERS.length; layer++) {
                var vertices = new BufferBuilder(memory, VertexFormat.Mode.QUADS,
                        shaderLayout ? DefaultVertexFormat.BLOCK : DefaultVertexFormat.NEW_ENTITY);
                Minecraft.getInstance().getItemRenderer().renderModelLists(models.getModel(SonnetDomeRenderer.MODELS[layer]),
                        ItemStack.EMPTY, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, new PoseStack(), vertices);
                var mesh = vertices.build();
                if (mesh == null) continue;
                var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                LAYERS[layer] = buffer;
                buffer.bind();
                buffer.upload(mesh);
            }
        } finally {
            VertexBuffer.unbind();
        }
    }

    static void render(PoseStack pose, MultiBufferSource buffers) {
        // This is a luminous visual field, not a physical occluder. Do not redraw its
        // surface into the shadow map (or darken the entire arena underneath it).
        if (ShaderPackCompat.shadowPass()) return;
        // Outlines and other wrapped passes need vertices through their own consumer.
        if (!(buffers instanceof MultiBufferSource.BufferSource)) {
            var mc = Minecraft.getInstance();
            for (int layer = 0; layer < LAYERS.length; layer++) {
                RenderType type = SHELL;
                mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(SonnetDomeRenderer.MODELS[layer]),
                        ItemStack.EMPTY, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers.getBuffer(type));
            }
            return;
        }
        boolean shaders = ShaderPackCompat.active();
        // Shader toggles change Iris's vertex layout without necessarily reloading models.
        if (shaderLayout != shaders) reload(Minecraft.getInstance().getModelManager());
        // Entity poses contain camera-relative translation. The camera rotation is
        // in RenderSystem's model-view matrix and must be applied exactly once.
        var modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        var previousShader = RenderSystem.getShader();
        try {
            for (int layer = 0; layer < LAYERS.length; layer++) {
                if (LAYERS[layer] == null) continue;
                // Full-bright surfaces need no entity PBR/shadow sampling. The beacon
                // channel still participates in the shader pack's fog, bloom and compositing.
                RenderType type = shaders ? GLOW_SHELL : SHELL;
                type.setupRenderState();
                try {
                    LAYERS[layer].bind();
                    LAYERS[layer].drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                } finally {
                    VertexBuffer.unbind();
                    type.clearRenderState();
                }
            }
        } finally {
            RenderSystem.setShader(() -> previousShader);
        }
    }
}
