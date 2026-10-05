// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// package org.lyy.lyycore.client.originsea;

// import com.mojang.blaze3d.pipeline.TextureTarget;
// import com.mojang.blaze3d.systems.RenderSystem;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.GraphicsStatus;
// import net.minecraft.client.Screenshot;
// import net.minecraft.client.gui.screens.TitleScreen;
// import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
// import net.minecraft.core.HolderSet;
// import net.minecraft.core.BlockPos;
// import net.minecraft.core.registries.Registries;
// import net.minecraft.world.Difficulty;
// import net.minecraft.world.level.GameRules;
// import net.minecraft.world.effect.MobEffectInstance;
// import net.minecraft.world.effect.MobEffects;
// import net.minecraft.world.level.GameType;
// import net.minecraft.world.level.LevelSettings;
// import net.minecraft.world.level.WorldDataConfiguration;
// import net.minecraft.world.level.biome.Biomes;
// import net.minecraft.world.level.block.Blocks;
// import net.minecraft.world.level.levelgen.FlatLevelSource;
// import net.minecraft.world.level.levelgen.WorldOptions;
// import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
// import net.minecraft.world.level.levelgen.presets.WorldPresets;
// import net.minecraft.world.phys.Vec3;
// import net.neoforged.api.distmarker.Dist;
// import net.neoforged.bus.api.SubscribeEvent;
// import net.neoforged.fml.common.EventBusSubscriber;
// import net.neoforged.neoforge.client.ClientCommandHandler;
// import net.neoforged.neoforge.client.event.ClientTickEvent;
// import net.neoforged.neoforge.client.event.RenderFrameEvent;
// import org.joml.Matrix4f;
// import org.lwjgl.opengl.GL11;
// import org.lyy.lyycore.LyyCore;

// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.List;
// import java.util.Optional;

// /** Runs inside a separate development client, without desktop input automation. */
// @EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
// public final class OriginSeaRenderCheck {
//     private static final boolean RUN = Boolean.getBoolean("lyycore.originSeaCheck");
//     private static final Path OUTPUT = Path.of(System.getProperty("lyycore.originSeaOutput", "build/origin-sea-check"));
//     private static boolean started;
//     private static boolean initialized;
//     private static boolean reloaded;
//     private static boolean reloadChecked;
//     private static boolean motionChecked;
//     private static int tick;
//     private static String capture;
//     private static long frameStart;
//     private static long frameNanos;
//     private static int frames;

//     @SubscribeEvent
//     public static void beforeFrame(RenderFrameEvent.Pre event) {
//         if (RUN) frameStart = System.nanoTime();
//     }

//     @SubscribeEvent
//     public static void afterFrame(RenderFrameEvent.Post event) throws Exception {
//         if (!RUN) return;
//         var mc = Minecraft.getInstance();
//         if (!started && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)
//                 && mc.getOverlay() == null && OriginSeaRenderer.isReady()) {
//             started = true;
//             Files.createDirectories(OUTPUT);
//             var target = new TextureTarget(1600, 900, true, Minecraft.ON_OSX);
//             target.setClearColor(0, 0, 0, 1);
//             var projection = new Matrix4f().perspective((float) Math.toRadians(70), 1600F / 900, 0.05F, 512F);
//             for (int i = 0; i < 11; i++) {
//                 float elevation = (float) Math.toRadians(i == 10 ? 89.9 : (i >= 4 ? 25 : (i == 2 ? 67 : 18)));
//                 float azimuth = (float) Math.toRadians(i >= 4 && i < 10 ? (i-4)*60 : (i == 3 ? 140 : 0));
//                 var view = new Matrix4f().lookAt(0,0,0,
//                         (float) Math.sin(azimuth)*(float) Math.cos(elevation), (float) Math.sin(elevation),
//                         -(float) Math.cos(azimuth)*(float) Math.cos(elevation), 0,1,0);
//                 target.clear(Minecraft.ON_OSX);
//                 target.bindWrite(true);
//                 var previewProjection = i == 10
//                         ? new Matrix4f().perspective((float) Math.toRadians(125),1600F/900,0.05F,512F) : projection;
//                 OriginSeaRenderer.renderSky(view, previewProjection, Vec3.ZERO, i == 1 ? 9 : 0);
//                 try (var pixels = Screenshot.takeScreenshot(target)) {
//                     pixels.writeToFile(OUTPUT.resolve("shader-" + i + ".png"));
//                 }
//                 require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error in shader preview " + i);
//             }
//             target.destroyBuffers();
//             mc.getMainRenderTarget().bindWrite(true);
//             mc.options.pauseOnLostFocus = false;
//             mc.options.hideGui = true;
//             mc.options.enableVsync().set(false);
//             mc.options.framerateLimit().set(120);
//             mc.options.renderDistance().set(8);
//             mc.options.simulationDistance().set(5);
//             mc.options.onboardAccessibility = false;
//             mc.execute(() -> {
//                 var settings = new LevelSettings("Origin Sea render check", GameType.SPECTATOR, false,
//                         Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
//                 mc.createWorldOpenFlows().createFreshLevel("origin-sea-" + System.currentTimeMillis(), settings,
//                         new WorldOptions(1751, false, false), access -> {
//                             var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
//                                     access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID), List.of());
//                             flat.updateLayers();
//                             return access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
//                                     .value().createWorldDimensions().replaceOverworldGenerator(access, new FlatLevelSource(flat));
//                         }, new TitleScreen());
//             });
//         }
//         if (initialized && tick > 40 && tick < 180) {
//             frameNanos += System.nanoTime() - frameStart;
//             frames++;
//         }
//         if (initialized && tick >= 85 && !motionChecked && mc.getOverlay() == null) {
//             OriginSeaMotionCheck.run(OUTPUT);
//             motionChecked = true;
//         }
//         if (capture != null && mc.level != null && mc.getOverlay() == null) {
//             try (var pixels = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
//                 pixels.writeToFile(OUTPUT.resolve(capture + ".png"));
//             }
//             LyyCore.LOGGER.info("ORIGIN_SEA_CHECK captured {}", capture);
//             capture = null;
//         }
//     }

//     @SubscribeEvent
//     public static void tick(ClientTickEvent.Post event) throws Exception {
//         if (!RUN || !started) return;
//         var mc = Minecraft.getInstance();
//         if (mc.level == null || mc.player == null || mc.screen != null || mc.getOverlay() != null) return;
//         if (!initialized) {
//             initialized = true;
//             var server = mc.getSingleplayerServer();
//             server.execute(() -> {
//                 var player = server.getPlayerList().getPlayer(mc.player.getUUID());
//                 player.teleportTo(0, 80, 0);
//                 server.overworld().setDayTime(6000);
//                 server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
//                 server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
//             });
//         }
//         tick++;
//         mc.player.setYRot(0);
//         mc.player.setXRot(tick >= 160 && tick < 200 ? -65 : -15);
//         if (tick == 20) {
//             require(!OriginSeaClient.isActive(), "Background must default to off");
//             command("originsea on");
//             require(OriginSeaClient.isActive(), "on command did not activate render hook");
//         }
//         if (tick == 80) capture = "in-world-horizon";
//         if (tick == 150) capture = "in-world-motion";
//         if (tick == 190) capture = "in-world-zenith";
//         if (tick == 195) {
//             // Foreground geometry proves the sky and optional sea obey the game's depth buffer.
//             var server = mc.getSingleplayerServer();
//             server.execute(() -> {
//                 for (int y = 76; y <= 86; y++) {
//                     server.overworld().setBlockAndUpdate(new BlockPos(-3,y,13),Blocks.QUARTZ_PILLAR.defaultBlockState());
//                     server.overworld().setBlockAndUpdate(new BlockPos(3,y,13),Blocks.QUARTZ_PILLAR.defaultBlockState());
//                 }
//                 for (int x = -3; x <= 3; x++)
//                     server.overworld().setBlockAndUpdate(new BlockPos(x,86,13),Blocks.QUARTZ_BLOCK.defaultBlockState());
//             });
//         }
//         if (tick == 210) {
//             command("originsea floor height 78.4");
//             mc.player.setXRot(22);
//         }
//         if (tick >= 210 && tick <= 230) mc.player.setXRot(22);
//         if (tick == 225) capture = "in-world-sea";
//         if (tick == 240) {
//             command("originsea shards 0");
//             require(OriginSeaClient.shardCount() == 0, "shard control failed");
//             command("originsea off");
//             require(!OriginSeaClient.isActive(), "off command did not restore vanilla");
//             capture = "in-world-off";
//         }
//         if (tick == 260) {
//             command("originsea on");
//             command("originsea shards " + OriginSeaClient.DEFAULT_SHARDS);
//             mc.reloadResourcePacks().thenRun(() -> reloaded = true);
//         }
//         if (tick > 280 && reloaded && !reloadChecked) {
//             require(OriginSeaClient.isActive() && OriginSeaRenderer.isReady(), "resource reload lost shaders or activation");
//             require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error after resource reload");
//             reloadChecked = true;
//         }
//         if (tick == 300) {
//             mc.player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100));
//             require(!OriginSeaClient.isVisible(),"Background bypassed blindness");
//             mc.player.removeEffect(MobEffects.BLINDNESS);
//             mc.player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100));
//             require(!OriginSeaClient.isVisible(),"Background bypassed darkness");
//             mc.player.removeEffect(MobEffects.DARKNESS);
//             mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);
//             mc.levelRenderer.allChanged();
//             command("originsea floor height 78.4");
//         }
//         if (tick >= 305 && tick <= 335) mc.player.setXRot(18);
//         if (tick == 330) capture = "in-world-fabulous";
//         if (tick == 350) {
//             require(reloadChecked,"Resource reload did not complete");
//             require(motionChecked,"Motion/camera regression checks did not complete");
//             require(GL11.glGetError() == GL11.GL_NO_ERROR,"OpenGL error in Fabulous graphics");
//             double mean = frameNanos / 1_000_000.0 / Math.max(1,frames);
//             Files.writeString(OUTPUT.resolve("result.txt"), "PASS: shaders, six rim directions, sky hook, client commands, GPU shard motion, sky bob/translation invariance, FOV/turning, animated frames, sea, off, resource reload, blindness/darkness, Fabulous graphics.\n"
//                     + "Mean render-frame CPU submission: " + mean + " ms over " + frames + " frames (not a GPU benchmark).\n");
//             LyyCore.LOGGER.info("ORIGIN_SEA_CHECK PASS: {} frames, {} ms mean render submission", frames, mean);
//             mc.stop();
//         }
//         if (tick > 1200) throw new IllegalStateException("Origin Sea render check timed out");
//     }

//     private static void command(String command) {
//         require(ClientCommandHandler.runCommand(command), "Client command was not registered: " + command);
//     }

//     private static void require(boolean condition, String message) {
//         if (!condition) throw new IllegalStateException(message);
//     }
// }
