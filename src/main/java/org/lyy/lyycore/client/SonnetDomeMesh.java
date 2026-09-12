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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Static dome geometry is uploaded at resource reload, then shared by every dome. */
final class SonnetDomeMesh {
    private static final VertexBuffer[] LAYERS = new VertexBuffer[SonnetDomeRenderer.MODELS.length];
    private static final RenderType SHELL = RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType DETAILS = RenderType.entityCutoutNoCullZOffset(TextureAtlas.LOCATION_BLOCKS);

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
        // Use a private allocator: do not interfere with an active world/item buffer.
        try (var memory = new ByteBufferBuilder(4 * 1024 * 1024)) {
            for (int layer = 0; layer < LAYERS.length; layer++) {
                var vertices = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
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

    static void render(PoseStack pose) {
        // Entity poses contain camera-relative translation. The camera rotation is
        // in RenderSystem's model-view matrix and must be applied exactly once.
        var modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        for (int layer = 0; layer < LAYERS.length; layer++) {
            if (LAYERS[layer] == null) continue;
            RenderType type = layer == 0 ? SHELL : DETAILS;
            type.setupRenderState();
            try {
                LAYERS[layer].bind();
                LAYERS[layer].drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            } finally {
                VertexBuffer.unbind();
                type.clearRenderState();
            }
        }
    }
}
