package org.lyy.lyycore.client.originsea;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lyy.lyycore.LyyCore;

import java.io.IOException;
import java.util.Random;

/** Static GPU meshes; falling and tumbling are evaluated in the vertex shader. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class OriginSeaRenderer {
    private static ShaderInstance skyShader;
    private static ShaderInstance shardShader;
    private static VertexBuffer sky;
    private static VertexBuffer shards;
    private static VertexBuffer sea;

    private OriginSeaRenderer() {}

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        release();
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("origin_sea"), DefaultVertexFormat.POSITION),
                shader -> skyShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("origin_shards"), DefaultVertexFormat.POSITION_TEX_COLOR),
                shader -> shardShader = shader);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, name);
    }

    static boolean isReady() { return skyShader != null && shardShader != null; }

    static void release() {
        if (sky != null) sky.close();
        if (shards != null) shards.close();
        if (sea != null) sea.close();
        sky = shards = sea = null;
    }

    private static void ensureMeshes() {
        if (sky != null) return;
        try (var memory = new ByteBufferBuilder(2 * 1024 * 1024)) {
            var cube = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            // An enclosing cube is interpolated as a direction, without panorama seams or polar pinching.
            float[][] corners = {{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},
                    {-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
            int[][] faces = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
            for (int[] face : faces) for (int index : face) {
                float[] p = corners[index];
                cube.addVertex(p[0], p[1], p[2]);
            }
            sky = upload(cube);

            var floor = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            floor.addVertex(-1, 0, -1);
            floor.addVertex(-1, 0, 1);
            floor.addVertex(1, 0, 1);
            floor.addVertex(1, 0, -1);
            sea = upload(floor);

            var fragments = new BufferBuilder(memory, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
            var random = new Random(0xE1751A);
            for (int i = 0; i < OriginSeaClient.MAX_SHARDS; i++) {
                // Thin irregular four-to-six-sided plates, rather than long tetrahedral spikes.
                int sides = 4 + random.nextInt(3);
                float width = 0.6F + random.nextFloat() * 0.6F;
                float height = 0.5F + random.nextFloat() * 0.7F;
                float thickness = 0.035F + random.nextFloat() * 0.10F;
                float[][] edge = new float[sides][3];
                for (int n = 0; n < sides; n++) {
                    double angle = (n + random.nextFloat() * 0.35) * Math.PI * 2 / sides;
                    float radius = 0.72F + random.nextFloat() * 0.28F;
                    edge[n] = new float[]{(float) Math.cos(angle) * width * radius,
                            (float) Math.sin(angle) * height * radius, 0};
                }
                for (int n = 0; n < sides; n++) {
                    float[] a = edge[n], b = edge[(n + 1) % sides];
                    int shade = 105 + random.nextInt(105);
                    fragmentTriangle(fragments, i, shade, new float[]{0,0,thickness}, a, b);
                    fragmentTriangle(fragments, i, shade / 2, new float[]{0,0,-thickness}, b, a);
                }
            }
            shards = upload(fragments);
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static void fragmentTriangle(BufferBuilder builder, int id, int shade, float[]... points) {
        for (float[] p : points) builder.addVertex(p[0], p[1], p[2])
                .setColor(shade, shade, shade, 255).setUv(id, 0);
    }

    private static VertexBuffer upload(BufferBuilder builder) {
        var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        return buffer;
    }

    public static void renderSky(Matrix4f view, Matrix4f projection, Vec3 camera, float seconds) {
        if (!isReady()) return;
        ensureMeshes();
        var previous = RenderSystem.getShader();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            setupSky(seconds, camera, 0, 0);
            sky.bind();
            sky.drawWithShader(view, projection, skyShader);
            if (OriginSeaClient.shardCount() > 0) {
                shardShader.safeGetUniform("SceneTime").set(seconds);
                shardShader.safeGetUniform("ShardCount").set((float) OriginSeaClient.shardCount());
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                shards.bind();
                shards.drawWithShader(view, projection, shardShader);
            }
        } finally {
            VertexBuffer.unbind();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> previous);
        }
    }

    static void renderFloor(Matrix4f view, Matrix4f projection, Vec3 camera, double height, float seconds) {
        if (!isReady()) return;
        ensureMeshes();
        var previous = RenderSystem.getShader();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        try {
            setupSky(seconds, camera, 1, (float) (height - camera.y));
            sea.bind();
            sea.drawWithShader(view, projection, skyShader);
        } finally {
            VertexBuffer.unbind();
            RenderSystem.enableCull();
            RenderSystem.setShader(() -> previous);
        }
    }

    private static void setupSky(float seconds, Vec3 camera, float surface, float height) {
        skyShader.safeGetUniform("SceneTime").set(seconds);
        skyShader.safeGetUniform("SurfacePass").set(surface);
        skyShader.safeGetUniform("SeaHeight").set(height);
        skyShader.safeGetUniform("CameraXZ").set((float) (camera.x % 4096), (float) (camera.z % 4096));
    }
}
