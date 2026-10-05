// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// package org.lyy.lyycore.client.originsea;

// import com.mojang.blaze3d.vertex.PoseStack;
// import net.minecraft.client.Camera;
// import net.minecraft.client.multiplayer.ClientLevel;
// import net.minecraft.client.renderer.DimensionSpecialEffects;
// import net.minecraft.client.renderer.LightTexture;
// import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
// import net.neoforged.api.distmarker.Dist;
// import net.neoforged.bus.api.SubscribeEvent;
// import net.neoforged.fml.common.EventBusSubscriber;
// import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
// import org.joml.Matrix4f;
// import org.lyy.lyycore.LyyCore;

// /** Uses NeoForge's sky hook; inactive sessions retain vanilla Overworld behavior. */
// @EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
// public final class OriginSeaEffects extends DimensionSpecialEffects.OverworldEffects {
//     @SubscribeEvent
//     public static void register(RegisterDimensionSpecialEffectsEvent event) {
//         event.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new OriginSeaEffects());
//     }

//     @Override
//     public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f view,
//                              Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog) {
//         if (!OriginSeaClient.isVisible() || foggy) return false;
//         OriginSeaRenderer.renderSky(view, OriginSeaClient.skyProjection(projection), camera.getPosition(),
//                 OriginSeaClient.time(partialTick));
//         return true;
//     }

//     @Override
//     public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack pose,
//                                 double x, double y, double z, Matrix4f view, Matrix4f projection) {
//         return OriginSeaClient.isActive();
//     }

//     @Override
//     public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick,
//                                     LightTexture light, double x, double y, double z) {
//         return OriginSeaClient.isActive();
//     }

//     @Override
//     public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
//         return OriginSeaClient.isActive();
//     }
// }
