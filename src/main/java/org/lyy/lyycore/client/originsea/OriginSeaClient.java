// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// package org.lyy.lyycore.client.originsea;

// import com.mojang.brigadier.arguments.DoubleArgumentType;
// import com.mojang.brigadier.arguments.IntegerArgumentType;
// import net.minecraft.client.Minecraft;
// import net.minecraft.commands.Commands;
// import net.minecraft.network.chat.Component;
// import net.minecraft.world.effect.MobEffects;
// import net.minecraft.world.entity.LivingEntity;
// import net.minecraft.world.level.Level;
// import net.minecraft.world.level.material.FogType;
// import net.neoforged.api.distmarker.Dist;
// import net.neoforged.bus.api.SubscribeEvent;
// import net.neoforged.fml.common.EventBusSubscriber;
// import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
// import net.neoforged.neoforge.client.event.ClientTickEvent;
// import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
// import net.neoforged.neoforge.client.event.RenderFrameEvent;
// import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
// import net.neoforged.neoforge.client.event.ViewportEvent;
// import org.joml.Matrix4f;
// import org.lyy.lyycore.LyyCore;

// /** Session-local controls. No packets, entities, world edits or persistent options. */
// @EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
// public final class OriginSeaClient {
//     static final int DEFAULT_SHARDS = 1600;
//     static final int MAX_SHARDS = 3200;
//     private static boolean enabled;
//     private static boolean floor;
//     private static double floorHeight;
//     private static long ticks;
//     private static int shardCount = DEFAULT_SHARDS;
//     private static ViewportEvent.ComputeFov worldFov;

//     private OriginSeaClient() {}

//     public static boolean isActive() {
//         var level = Minecraft.getInstance().level;
//         return enabled && level != null && level.dimension().equals(Level.OVERWORLD)
//                 && level.effects() instanceof OriginSeaEffects && OriginSeaRenderer.isReady();
//     }

//     static boolean isVisible() {
//         if (!isActive()) return false;
//         var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
//         if (camera.getFluidInCamera() != FogType.NONE) return false;
//         return !(camera.getEntity() instanceof LivingEntity living)
//                 || (!living.hasEffect(MobEffects.BLINDNESS) && !living.hasEffect(MobEffects.DARKNESS));
//     }

//     static float time(float partialTick) {
//         // This clock stops with the game, and does not jump when /time changes.
//         return (ticks + partialTick) / 20.0F;
//     }

//     static int shardCount() { return shardCount; }

//     @SubscribeEvent
//     public static void beforeFrame(RenderFrameEvent.Pre event) {
//         worldFov = null;
//     }

//     @SubscribeEvent
//     public static void captureWorldFov(ViewportEvent.ComputeFov event) {
//         if (event.usedConfiguredFov()) worldFov = event;
//     }

//     static Matrix4f skyProjection(Matrix4f fallback) {
//         // In 1.21.1, walking/hurt bob is baked into the world PROJECTION, not the
//         // view matrix. Rebuild just the sky's projection before those transforms.
//         // Read the event after dispatch, preserving even later FOV/zoom modifiers;
//         // the hand's separate FOV event must never replace the world's value.
//         return worldFov == null ? fallback : worldFov.getRenderer().getProjectionMatrix(worldFov.getFOV());
//     }

//     @SubscribeEvent
//     public static void tick(ClientTickEvent.Post event) {
//         if (isActive() && !Minecraft.getInstance().isPaused()) ticks++;
//     }

//     @SubscribeEvent
//     public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
//         enabled = false;
//         floor = false;
//         ticks = 0;
//         shardCount = DEFAULT_SHARDS;
//         worldFov = null;
//         OriginSeaRenderer.release();
//     }

//     @SubscribeEvent
//     public static void registerCommands(RegisterClientCommandsEvent event) {
//         event.getDispatcher().register(Commands.literal("originsea")
//                 .executes(context -> setEnabled(!enabled))
//                 .then(Commands.literal("on").executes(context -> setEnabled(true)))
//                 .then(Commands.literal("off").executes(context -> setEnabled(false)))
//                 .then(Commands.literal("floor")
//                         .executes(context -> setFloor(!floor))
//                         .then(Commands.literal("on").executes(context -> setFloor(true)))
//                         .then(Commands.literal("off").executes(context -> setFloor(false)))
//                         .then(Commands.literal("height").then(Commands.argument("y", DoubleArgumentType.doubleArg(-2048, 2048))
//                                 .executes(context -> {
//                                     if (setEnabled(true) == 0) return 0;
//                                     floorHeight = DoubleArgumentType.getDouble(context, "y");
//                                     floor = true;
//                                     return 1;
//                                 }))))
//                 .then(Commands.literal("shards").then(Commands.argument("count", IntegerArgumentType.integer(0, MAX_SHARDS))
//                         .executes(context -> {
//                             shardCount = IntegerArgumentType.getInteger(context, "count");
//                             message("command.lyycore.originsea.shards", shardCount);
//                             return 1;
//                         })))
//                 .then(Commands.literal("status").executes(context -> {
//                     message("command.lyycore.originsea.status", enabled, floor, shardCount);
//                     return 1;
//                 })));
//     }

//     private static int setEnabled(boolean value) {
//         var minecraft = Minecraft.getInstance();
//         if (value && (minecraft.level == null || !minecraft.level.dimension().equals(Level.OVERWORLD))) {
//             message("command.lyycore.originsea.overworld");
//             return 0;
//         }
//         if (value && (!(minecraft.level.effects() instanceof OriginSeaEffects) || !OriginSeaRenderer.isReady())) {
//             message("command.lyycore.originsea.unavailable");
//             return 0;
//         }
//         enabled = value;
//         if (!value) {
//             floor = false;
//             ticks = 0;
//             OriginSeaRenderer.release();
//         }
//         message(value ? "command.lyycore.originsea.on" : "command.lyycore.originsea.off");
//         return 1;
//     }

//     private static int setFloor(boolean value) {
//         if (value) {
//             if (setEnabled(true) == 0) return 0;
//             floorHeight = Minecraft.getInstance().player.getY() - 0.025;
//         }
//         floor = value;
//         message(value ? "command.lyycore.originsea.floor_on" : "command.lyycore.originsea.floor_off");
//         return 1;
//     }

//     private static void message(String key, Object... args) {
//         var player = Minecraft.getInstance().player;
//         if (player != null) player.displayClientMessage(Component.translatable(key, args), false);
//     }

//     @SubscribeEvent
//     public static void renderFloor(RenderLevelStageEvent event) {
//         if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !floor || !isVisible()) return;
//         var position = event.getCamera().getPosition();
//         // Looking from below never covers the sky with the underside of the sea.
//         if (position.y <= floorHeight + 0.05) return;
//         OriginSeaRenderer.renderFloor(event.getModelViewMatrix(), event.getProjectionMatrix(), position,
//                 floorHeight, time(event.getPartialTick().getGameTimeDeltaPartialTick(false)));
//     }

//     @SubscribeEvent
//     public static void fogColor(ViewportEvent.ComputeFogColor event) {
//         if (!isVisible()) return;
//         event.setRed(0.045F);
//         event.setGreen(0.115F);
//         event.setBlue(0.28F);
//     }
// }
