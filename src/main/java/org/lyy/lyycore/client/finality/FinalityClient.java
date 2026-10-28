package org.lyy.lyycore.client.finality;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

import org.lyy.lyycore.content.finality.*;

@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class FinalityClient {
    private static int progress = -1, layers;

    private record Impact(Vec3 position, long born) {}

    private static final java.util.List<Impact> IMPACTS = new java.util.ArrayList<>();
    private static net.minecraft.client.model.geom.ModelPart impactModel;

    public static void impact(Vec3 position) {
        var level = Minecraft.getInstance().level;
        if (level != null)
            IMPACTS.add(new Impact(new Vec3(position.x, .03, position.z), level.getGameTime()));
    }

    public static void status(FinalityNetwork.Status packet) {
        progress = packet.progress();
        layers = packet.layers();
        if (progress < 0) IMPACTS.clear();
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(FinalityContent.SETTINGS.get(), FinalitySettingsScreen::new);
        event.register(FinalityContent.GENESIS_MENU.get(), GenesisScreen::new);
    }

    @SubscribeEvent
    public static void entities(EntityRenderersEvent.RegisterRenderers event) {
        impactModel =
                org.lyy.lyycore.client.finality.model.StarfallImpactGeometry.createBodyLayer()
                        .bakeRoot();
        event.registerEntityRenderer(FinalityContent.SUMMON.get(), FinalitySummonRenderer::new);
        event.registerEntityRenderer(
                FinalityContent.PROJECTILE.get(), FinalityProjectileRenderer::new);
        event.registerEntityRenderer(FinalityContent.WITHER.get(), WitherBossRenderer::new);
    }

    @SubscribeEvent
    public static void hud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                FinalityContent.id("finality_trial"),
                (g, delta) -> {
                    var mc = Minecraft.getInstance();
                    if (mc.level == null
                            || !mc.level.dimension().equals(FinalityTrials.DIMENSION)) {
                        progress = -1;
                        return;
                    }
                    if (progress < 0 || mc.options.hideGui) return;
                    int x = (g.guiWidth() - 256) / 2;
                    bar(g, x, 8, "trial_progress", progress, 100);
                    bar(g, x, 43, "finality_mark", layers, 100);
                    g.drawCenteredString(
                            mc.font,
                            net.minecraft.network.chat.Component.translatable(
                                    "hud.lyycore.finality.progress", progress),
                            g.guiWidth() / 2,
                            8,
                            0xF5E3B7);
                    g.drawCenteredString(
                            mc.font,
                            net.minecraft.network.chat.Component.translatable(
                                    "hud.lyycore.finality.mark", layers),
                            g.guiWidth() / 2,
                            43,
                            0xF5C1EB);
                });
    }

    private static ResourceLocation barTexture(String name, String part) {
        return FinalityContent.id("textures/gui/finality_trial/" + name + "_" + part + ".png");
    }

    private static void bar(GuiGraphics g, int x, int y, String name, int amount, int max) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(.5F, .5F, 1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(barTexture(name, "track"), 0, 0, 0, 0, 512, 64, 512, 64);
        int width = Math.clamp(amount, 0, max) * 400 / max;
        if (width > 0) g.blit(barTexture(name, "fill"), 56, 26, 0, 0, width, 12, 400, 12);
        g.blit(barTexture(name, "frame"), 0, 0, 0, 0, 512, 64, 512, 64);
        if (amount > 1)
            g.blit(barTexture(name, "cursor"), 56 + width - 12, 8, 0, 0, 24, 48, 24, 48);
        g.pose().popPose();
    }

    @SubscribeEvent
    public static void links(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
                || mc.level == null
                || !mc.level.dimension().equals(FinalityTrials.DIMENSION)) return;
        var entities = new java.util.ArrayList<net.minecraft.world.entity.LivingEntity>();
        for (var entity : mc.level.entitiesForRendering())
            if (entity instanceof FinalitySummon || entity instanceof FinalityWither)
                if (entity.isAlive())
                    entities.add((net.minecraft.world.entity.LivingEntity) entity);
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        IMPACTS.removeIf(impact -> mc.level.getGameTime() - impact.born() > 8);
        for (var impact : IMPACTS) {
            float age = (float) (mc.level.getGameTime() - impact.born()) + partial;
            pose.pushPose();
            pose.translate(impact.position().x, impact.position().y, impact.position().z);
            FinalityProjectileRenderer.draw(
                    impactModel,
                    FinalitySummonRenderer.texture("starfall_impact"),
                    pose,
                    buffers,
                    1 + age * .1F,
                    Math.max(.01F, 1 - age / 8),
                    1 + age * .1F);
            pose.popPose();
        }
        for (var entity : entities)
            if (entity instanceof FinalitySummon summon) {
                Vec3 from = summon.getPosition(partial).add(0, .75, 0);
                if (summon.kind() == FinalitySummon.Kind.HEART)
                    for (var target : entities) {
                        if (target == summon) continue;
                        Vec3 to = target.getPosition(partial).add(0, target.getBbHeight() / 2, 0);
                        FinalityProjectileRenderer.line(pose, buffers, from, to, .012F, 0x70FFE2A1);
                        double time = ((summon.tickCount % 40) + partial) / 40;
                        FinalityProjectileRenderer.line(
                                pose,
                                buffers,
                                from.lerp(to, time),
                                from.lerp(to, Math.min(1, time + .025)),
                                .045F,
                                0xFFFFE9BC);
                    }
                if (summon.kind() == FinalitySummon.Kind.STAKE
                        && mc.level.getEntity(summon.linkedPlayer())
                                instanceof net.minecraft.world.entity.player.Player player)
                    FinalityProjectileRenderer.line(
                            pose,
                            buffers,
                            from,
                            player.getPosition(partial).add(0, 1, 0),
                            .025F,
                            0xE0FFC4E3);
            }
        buffers.endBatch(net.minecraft.client.renderer.RenderType.lightning());
        pose.popPose();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        progress = -1;
        layers = 0;
        IMPACTS.clear();
    }
}
