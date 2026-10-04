package org.lyy.lyycore.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.registry.LyyItems;

/** Offscreen integration checks through Minecraft's real item/shader pipeline; no desktop input. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class SonnetRenderCheck {
    private static final boolean RUN = Boolean.getBoolean("lyycore.sonnetRenderCheck");
    private static final Path OUTPUT = Path.of(System.getProperty("lyycore.sonnetRenderOutput", "build/sonnet-render-check"));
    private static final String[] POSES = {"whisper_of_the_past", "whisper_of_the_past/pulling_0",
            "whisper_of_the_past/pulling_1", "whisper_of_the_past/pulling_2",
            "sanctuary_bow/horizontal_idle", "sanctuary_bow/horizontal_pulling_0",
            "sanctuary_bow/horizontal_pulling_1", "sanctuary_bow/horizontal"};
    private static int phase;
    private static boolean hidden;
    private static int worldTick;
    private static String worldCapture;

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        if (!RUN) return;
        for (int i = 1; i < POSES.length; i++) {
            event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("lyycore", "item/" + POSES[i])));
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        if (RUN && !hidden) {
            GLFW.glfwHideWindow(Minecraft.getInstance().getWindow().getWindow());
            hidden = true;
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!RUN) return;
        var mc = Minecraft.getInstance();
        if (phase == 3) {
            if (worldCapture != null && mc.level != null && mc.getOverlay() == null) {
                try (var pixels = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    pixels.writeToFile(OUTPUT.resolve(worldCapture + ".png"));
                }
                require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error in world: " + worldCapture);
                worldCapture = null;
            }
            return;
        }
        if (phase == 1 || phase > 3) return;
        if (!(mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen) || mc.getOverlay() != null) return;
        Files.createDirectories(OUTPUT);
        try {
            Files.writeString(OUTPUT.resolve("result.txt"), "RUNNING: render checks\n");
            int before = cache().size();
            require(before == 0, "GPU mesh cache was not cleared on resource reload: " + before);
            capture("poses-" + phase, false);
            int uploaded = cache().size();
            require(uploaded >= 8, "Missing pose meshes: " + uploaded);
            capture("oblique-" + phase, true);
            require(cache().size() == uploaded, "Model cache grew on repeated draws");
            contexts();
            transparency();
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error in context rendering");
            if (phase == 0) {
                phase = 1;
                mc.reloadResourcePacks().thenRun(() -> phase = 2);
            } else {
                phase = 3;
                createWorld();
            }
        } catch (Throwable error) {
            Files.writeString(OUTPUT.resolve("result.txt"), "FAIL: " + error + "\n");
            phase = 4;
            mc.stop();
            throw error;
        }
    }

    private static void createWorld() {
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        // Vanilla also hides first-person hands when hideGui is enabled.
        mc.options.hideGui = false;
        mc.options.graphicsMode().set(GraphicsStatus.FANCY);
        mc.options.onboardAccessibility = false;
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(60);
        mc.options.renderDistance().set(4);
        mc.options.simulationDistance().set(5);
        mc.execute(() -> {
            var settings = new LevelSettings("Sonnet rendering check", GameType.CREATIVE, false,
                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("sonnet-" + System.currentTimeMillis(), settings,
                    new WorldOptions(781, false, false), access -> {
                        var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
                                access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID), List.of());
                        flat.updateLayers();
                        return access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                                .value().createWorldDimensions().replaceOverworldGenerator(access, new FlatLevelSource(flat));
                    }, new TitleScreen());
        });
    }

    @SubscribeEvent
    public static void inWorld(ClientTickEvent.Post event) throws Exception {
        if (!RUN || phase != 3) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.getOverlay() != null) return;
        worldTick++;
        var server = mc.getSingleplayerServer();
        if (worldTick == 1) {
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                player.teleportTo(0, 80, 0);
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 10; z++)
                    server.overworld().setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                server.overworld().setDayTime(6000);
                server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack(false));
            });
        }
        mc.player.setYRot(0);
        mc.player.setXRot(0);
        if (worldTick == 45) worldCapture = "world-normal";
        if (worldTick == 50) {
            mc.options.keyUse.setDown(true);
            server.execute(() -> server.getPlayerList().getPlayer(mc.player.getUUID()).startUsingItem(InteractionHand.MAIN_HAND));
            mc.player.startUsingItem(InteractionHand.MAIN_HAND);
        }
        if (worldTick == 80) {
            require(mc.player.isUsingItem(), "Ordinary bow did not remain in its draw animation");
            worldCapture = "world-normal-draw";
        }
        if (worldTick == 90) {
            mc.options.keyUse.setDown(false);
            mc.player.stopUsingItem();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                player.stopUsingItem();
                player.setItemInHand(InteractionHand.MAIN_HAND, stack(true));
            });
        }
        if (worldTick == 115) worldCapture = "world-crystal";
        if (worldTick == 125) mc.options.mainHand().set(HumanoidArm.LEFT);
        if (worldTick == 145) worldCapture = "world-crystal-left";
        if (worldTick == 155) {
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (worldTick == 175) worldCapture = "world-third-person";
        if (worldTick == 185) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);
            mc.levelRenderer.allChanged();
        }
        if (worldTick == 215) worldCapture = "world-fabulous";
        if (worldTick == 225) {
            Files.writeString(OUTPUT.resolve("result.txt"), "PASS: eight poses, all item display contexts, shader linking, GPU cache reuse, resource reload, background transmission, deferred opaque occlusion, first/third person, left hand, Fabulous, and OpenGL checks.\n");
            phase = 4;
            mc.stop();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<BakedModel, ?> cache() throws Exception {
        Field field = SonnetMaterialRenderer.class.getDeclaredField("MESHES");
        field.setAccessible(true);
        return (Map<BakedModel, ?>) field.get(null);
    }

    private static BakedModel model(int index) {
        var id = index == 0
                ? new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath("lyycore", POSES[index]), "inventory")
                : ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("lyycore", "item/" + POSES[index]));
        var models = Minecraft.getInstance().getModelManager();
        BakedModel result = models.getModel(id);
        require(result != models.getMissingModel(), "Missing model: " + id);
        if (index == 0) {
            var quads = result.getQuads(null, null, RandomSource.create(42));
            require(quads.size() == 3852, "Expected the new PMX bow (3852 faces), got " + quads.size());
            require(quads.getFirst().getSprite().contents().name().getPath().endsWith("/pmx_sonnet"), "New base texture was not loaded");
        }
        return SonnetBowModel.wrapItem(SonnetBowModel.unwrap(result));
    }

    private static ItemStack stack(boolean crystal) {
        var stack = new ItemStack(LyyItems.WHISPER_OF_THE_PAST.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("SonnetCrystal", crystal));
        return stack;
    }

    private static void capture(String name, boolean oblique) throws Exception {
        var mc = Minecraft.getInstance();
        var target = new TextureTarget(1600, 1000, true, Minecraft.ON_OSX);
        target.setClearColor(0.045F, 0.055F, 0.085F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().ortho(0, 1600, 0, 1000, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            for (int i = 0; i < POSES.length; i++) {
                var pose = new PoseStack();
                pose.translate(200 + (i % 4) * 400, i < 4 ? 750 : 250, 0);
                pose.scale(225, 225, 225);
                if (oblique) pose.mulPose(Axis.YP.rotationDegrees(35));
                mc.getItemRenderer().render(stack(i >= 4), ItemDisplayContext.GUI, false,
                        pose, mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model(i));
                require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error rendering " + POSES[i]);
            }
            mc.renderBuffers().bufferSource().endBatch();
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(OUTPUT.resolve(name + ".png"));
            }
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void contexts() {
        var mc = Minecraft.getInstance();
        var target = new TextureTarget(128, 128, true, Minecraft.ON_OSX);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        try {
            for (var context : ItemDisplayContext.values()) {
                for (boolean crystal : new boolean[]{false, true}) {
                    var stack = stack(crystal);
                    // Exercise actual ItemOverrides (the loop above directly selects all eight poses).
                    var model = mc.getItemRenderer().getModel(stack, null, null, 0);
                    mc.getItemRenderer().render(stack, context, context.name().contains("LEFT"), new PoseStack(),
                            mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                }
            }
            mc.renderBuffers().bufferSource().endBatch();
        } finally {
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void transparency() throws Exception {
        var mc = Minecraft.getInstance();
        var target = new TextureTarget(400, 400, true, Minecraft.ON_OSX);
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(2000);
        RenderSystem.setShaderFogEnd(3000);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().ortho(0, 400, 0, 400, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        try {
            for (boolean crystal : new boolean[]{false, true}) {
                try (var black = transparencyFrame(target, crystal, 0, false, null);
                     var white = transparencyFrame(target, crystal, 1, false, null)) {
                    int covered = 0, transmitted = 0;
                    for (int y = 0; y < 400; y++) for (int x = 0; x < 400; x++) {
                        int a = black.getPixelRGBA(x, y), b = white.getPixelRGBA(x, y);
                        if ((a & 255) + (a >> 8 & 255) + (a >> 16 & 255) < 48) continue;
                        covered++;
                        if (rgbDifference(a, b) > 16) transmitted++;
                    }
                    require(covered > 500, "Bow not visible in opacity check");
                    require(crystal ? transmitted > covered / 2 : transmitted < covered / 100,
                            "Unexpected background transmission: crystal=" + crystal + ", pixels=" + transmitted + "/" + covered);
                    if (crystal && phase == 0) {
                        black.writeToFile(OUTPUT.resolve("transparency-black.png"));
                        white.writeToFile(OUTPUT.resolve("transparency-white.png"));
                    }
                }
            }
            for (boolean foreground : new boolean[]{false, true}) {
                try (var reference = transparencyFrame(target, true, 0, false, foreground);
                     var deferred = transparencyFrame(target, true, 0, true, foreground)) {
                    int mismatched = 0;
                    for (int y = 0; y < 400; y++) for (int x = 0; x < 400; x++) {
                        if (rgbDifference(reference.getPixelRGBA(x, y), deferred.getPixelRGBA(x, y)) > 3) mismatched++;
                    }
                    require(mismatched == 0, "Crystal/opaque draw order differs: foreground=" + foreground + ", pixels=" + mismatched);
                }
            }
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static NativeImage transparencyFrame(TextureTarget target, boolean crystal, float background,
                                                  boolean deferred, Boolean foreground) {
        var mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        target.setClearColor(background, background, background, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        var pose = new PoseStack();
        pose.translate(200, 200, 0);
        pose.scale(225, 225, 225);
        var model = SonnetBowModel.unwrap(model(crystal ? 4 : 0));
        model.applyTransform(ItemDisplayContext.GUI, pose, false);
        pose.translate(-0.5, -0.5, -0.5);
        // Compare opaque geometry submitted after a queued bow against the same
        // geometry rendered before an immediate bow, both behind and in front.
        if (!deferred && foreground != null) backdrop(buffers, foreground ? 500 : -500);
        SonnetMaterialRenderer.renderBow(model, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                crystal, deferred ? ItemDisplayContext.GROUND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        if (deferred && foreground != null) backdrop(buffers, foreground ? 500 : -500);
        buffers.endBatch();
        return Screenshot.takeScreenshot(target);
    }

    private static void backdrop(net.minecraft.client.renderer.MultiBufferSource buffers, float z) {
        var vertices = buffers.getBuffer(RenderType.gui());
        vertices.addVertex(0, 0, z).setColor(20, 200, 80, 255);
        vertices.addVertex(0, 400, z).setColor(20, 200, 80, 255);
        vertices.addVertex(400, 400, z).setColor(20, 200, 80, 255);
        vertices.addVertex(400, 0, z).setColor(20, 200, 80, 255);
    }

    private static int rgbDifference(int a, int b) {
        return Math.max(Math.abs((a & 255) - (b & 255)),
                Math.max(Math.abs((a >> 8 & 255) - (b >> 8 & 255)), Math.abs((a >> 16 & 255) - (b >> 16 & 255))));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
