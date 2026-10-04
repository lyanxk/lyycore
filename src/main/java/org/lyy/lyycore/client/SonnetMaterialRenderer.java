package org.lyy.lyycore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lyy.lyycore.LyyCore;

/** Cached pose vertices with sorted translucency for the crystal bow. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class SonnetMaterialRenderer {
    private static final ResourceLocation SPHERE = id("textures/item/sonnet_material/ex_crystal.png");
    private static final ResourceLocation TOON = id("textures/item/sonnet_material/toon.png");
    private static final Map<BakedModel, CachedMesh> MESHES = new IdentityHashMap<>();
    private static ShaderInstance shader;
    private static final RenderType SOLID = type(false, false);
    private static final RenderType TRANSLUCENT = type(true, false);
    private static final RenderType WORLD_TRANSLUCENT = type(true, true);
    private static final CrystalBatch CRYSTAL_BATCH = new CrystalBatch();
    private static final float CRYSTAL_OPACITY = 0.28F;

    private SonnetMaterialRenderer() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, path);
    }

    private static RenderType type(boolean translucent, boolean world) {
        return RenderType.create("lyycore_sonnet_material" + (translucent ? "_translucent" : "") + (world ? "_world" : ""),
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 65536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                        .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setTransparencyState(translucent ? RenderStateShard.TRANSLUCENT_TRANSPARENCY
                                : RenderStateShard.NO_TRANSPARENCY)
                        .setOutputState(world ? RenderStateShard.ITEM_ENTITY_TARGET : RenderStateShard.MAIN_TARGET)
                        // The PMX pose exporter emits reversed faces for double-sided
                        // materials. Culling keeps those from being rasterized twice.
                        .setCullState(RenderStateShard.CULL)
                        .createCompositeState(false));
    }

    @SubscribeEvent
    public static void buffers(RegisterRenderBuffersEvent event) {
        // A fixed buffer is flushed after opaque entities, so players and other
        // objects remain visible through bows regardless of entity iteration order.
        event.registerRenderBuffer(CRYSTAL_BATCH);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("sonnet_material"),
                DefaultVertexFormat.NEW_ENTITY), loaded -> shader = loaded);
    }

    @SubscribeEvent
    public static void modelsReloaded(ModelEvent.BakingCompleted event) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(SonnetMaterialRenderer::release);
        } else {
            release();
        }
    }

    private static void release() {
        CRYSTAL_BATCH.draws.clear();
        MESHES.values().forEach(CachedMesh::close);
        MESHES.clear();
        SonnetItemRenderer.clearSelection();
    }

    private static CachedMesh upload(BakedModel model) {
        try (var memory = new ByteBufferBuilder(1024 * 1024)) {
            var builder = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            // UV1 carries a static material flag; the real overlay is a per-draw uniform.
            // This lets the nocked crystal arrow use its own material in the same pass
            // as the ordinary bow, without duplicating the whole bow for an emissive layer.
            var quads = new ArrayList<BakedQuad>();
            var random = RandomSource.create(42);
            quads.addAll(model.getQuads(null, null, random));
            for (Direction direction : Direction.values()) {
                random.setSeed(42);
                quads.addAll(model.getQuads(null, direction, random));
            }
            var identity = new PoseStack().last();
            for (BakedQuad quad : quads) {
                int material = quad.getSprite().contents().name().getPath().endsWith("/pmx_crystal") ? 1 : 0;
                builder.putBulkData(identity, quad, 1, 1, 1, 1, LightTexture.FULL_BRIGHT, material, true);
            }
            var mesh = builder.build();
            if (mesh == null) return null;
            var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            try {
                var sorting = mesh.sortQuads(memory, VertexSorting.ORTHOGRAPHIC_Z);
                buffer.bind();
                buffer.upload(mesh);
                return new CachedMesh(buffer, sorting);
            } catch (RuntimeException error) {
                buffer.close();
                throw error;
            } finally {
                VertexBuffer.unbind();
            }
        }
    }

    static void render(BakedModel source, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay, boolean crystal, boolean gui) {
        render(source, pose, buffers, light, overlay, crystal, gui, 1.0F, false);
    }

    static void renderBow(BakedModel source, PoseStack pose, MultiBufferSource buffers,
                          int light, int overlay, boolean crystal, ItemDisplayContext context) {
        boolean gui = context == ItemDisplayContext.GUI;
        render(source, pose, buffers, light, overlay, crystal, gui,
                crystal ? CRYSTAL_OPACITY : 1.0F, crystal && !gui && !context.firstPerson());
    }

    private static void render(BakedModel source, PoseStack pose, MultiBufferSource buffers,
                               int light, int overlay, boolean crystal, boolean gui, float opacity, boolean deferred) {
        BakedModel model = SonnetBowModel.unwrap(source);
        // OutlineBufferSource and other wrappers must receive their vertices so that
        // team outlines, capture passes, etc. continue to work with the host renderer.
        if (shader == null || !(buffers instanceof MultiBufferSource.BufferSource immediate)) {
            var type = crystal ? RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)
                    : RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);
            Minecraft.getInstance().getItemRenderer().renderModelLists(model, ItemStack.EMPTY,
                    light, overlay, pose, buffers.getBuffer(type));
            return;
        }

        CachedMesh mesh = MESHES.computeIfAbsent(model, SonnetMaterialRenderer::upload);
        if (mesh == null) return;
        var draw = new Draw(mesh, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()),
                new Matrix4f(RenderSystem.getProjectionMatrix()), RenderSystem.getShaderColor().clone(),
                light, overlay, crystal, gui, opacity);
        if (deferred && immediate == Minecraft.getInstance().renderBuffers().bufferSource()) {
            CRYSTAL_BATCH.submit(immediate, draw);
        } else {
            // GUI backgrounds and first-person arms must precede translucent items.
            // Ordinary bows and projectiles retain the original opaque fast path.
            if (gui || opacity < 1.0F) immediate.endBatch();
            draw(draw, false);
        }
    }

    private static void draw(Draw draw, boolean world) {
        var previousShader = RenderSystem.getShader();
        float[] previousColor = RenderSystem.getShaderColor().clone();
        int sphereSlot = RenderSystem.getShaderTexture(3);
        int toonSlot = RenderSystem.getShaderTexture(4);
        boolean translucent = draw.opacity < 1.0F;
        RenderType type = !translucent ? SOLID : world && Minecraft.getInstance().level != null
                && Minecraft.getInstance().levelRenderer.getItemEntityTarget() != null ? WORLD_TRANSLUCENT : TRANSLUCENT;
        type.setupRenderState();
        try {
            RenderSystem.setShaderColor(draw.color[0], draw.color[1], draw.color[2], draw.color[3]);
            RenderSystem.setShaderTexture(3, SPHERE);
            RenderSystem.setShaderTexture(4, TOON);
            shader.safeGetUniform("NormalMat").set(new Matrix3f(draw.modelView).invert().transpose());
            shader.safeGetUniform("LightUV").set((float) LightTexture.block(draw.light), (float) LightTexture.sky(draw.light));
            shader.safeGetUniform("OverlayUV").set((float) (draw.overlay & 65535), (float) (draw.overlay >>> 16));
            shader.safeGetUniform("Crystal").set(draw.crystal ? 1.0F : 0.0F);
            shader.safeGetUniform("CrystalOpacity").set(draw.opacity);
            shader.safeGetUniform("Gui").set(draw.gui ? 1.0F : 0.0F);
            draw.mesh.buffer.bind();
            if (translucent) draw.mesh.sort(draw.modelView);
            draw.mesh.buffer.drawWithShader(draw.modelView, draw.projection, shader);
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
            RenderSystem.setShaderTexture(3, sphereSlot);
            RenderSystem.setShaderTexture(4, toonSlot);
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            RenderSystem.setShader(() -> previousShader);
        }
    }

    private record Draw(CachedMesh mesh, Matrix4f modelView, Matrix4f projection, float[] color,
                        int light, int overlay, boolean crystal, boolean gui, float opacity) {}

    private static final class CachedMesh implements AutoCloseable {
        private final VertexBuffer buffer;
        private final MeshData.SortState sorting;
        private final ByteBufferBuilder indices = new ByteBufferBuilder(256 * 1024);
        private final Vector3f lastDirection = new Vector3f(Float.NaN);

        private CachedMesh(VertexBuffer buffer, MeshData.SortState sorting) {
            this.buffer = buffer;
            this.sorting = sorting;
        }

        private void sort(Matrix4f view) {
            var direction = new Vector3f(view.m02(), view.m12(), view.m22()).normalize();
            if (direction.equals(lastDirection)) return;
            // Only index order changes with view direction. Cached vertices and
            // normals stay on the GPU, and translation never requires a re-sort.
            var sorted = sorting.buildSortedIndexBuffer(indices, VertexSorting.byDistance(v -> -direction.dot(v)));
            if (sorted != null) buffer.uploadIndexBuffer(sorted);
            lastDirection.set(direction);
        }

        @Override
        public void close() {
            buffer.close();
            indices.close();
        }
    }

    /** Uses the normal entity batch lifecycle without uploading the mesh again. */
    private static final class CrystalBatch extends RenderType {
        private final ArrayList<Draw> draws = new ArrayList<>();

        private CrystalBatch() {
            super("lyycore_sonnet_crystal_batch", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS,
                    256, false, false, () -> {}, () -> {});
        }

        private void submit(MultiBufferSource.BufferSource buffers, Draw draw) {
            var marker = buffers.getBuffer(this);
            draws.add(draw);
            // A tiny, never-rendered quad schedules draw() when the host flushes.
            for (int i = 0; i < 4; i++) marker.addVertex(0, 0, 0);
        }

        @Override
        public void draw(MeshData marker) {
            marker.close();
            try {
                draws.sort(Comparator.comparingDouble((Draw draw) -> draw.modelView.m32()));
                for (Draw draw : draws) SonnetMaterialRenderer.draw(draw, true);
            } finally {
                draws.clear();
            }
        }
    }
}
