package org.lyy.lyycore.client;

import org.lyy.lyycore.content.menu.ImaginaryGateScreen;
import org.lyy.lyycore.content.menu.AdvancedImaginaryGateScreen;
import org.lyy.lyycore.content.menu.ImaginaryCraftingScreen;
import org.lyy.lyycore.content.menu.ResearchScreen;
import org.lyy.lyycore.content.menu.ProductionLabScreen;
import org.lyy.lyycore.content.menu.ImaginaryReaperScreen;
import org.lyy.lyycore.registry.LyyBlockEntities;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.menu.EnergyCellScreen;
import org.lyy.lyycore.content.menu.IAFScreen;
import org.lyy.lyycore.registry.LyyMenus;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void uploadDomeMeshes(ModelEvent.BakingCompleted event) {
        SonnetDomeMesh.reload(event.getModelManager());
        ProductionLabRenderer.reload();
    }

    @SubscribeEvent
    public static void wrapCustomItemModels(ModelEvent.ModifyBakingResult event) {
        var grapple = new net.minecraft.client.resources.model.ModelResourceLocation(
                LyyItems.IMAGINARY_GRAPPLE.getId(), "inventory");
        var empty = event.getModels().get(GrappleRenderer.EMPTY_HELD_MODEL);
        if (empty != null) event.getModels().computeIfPresent(grapple, (id, model) -> GrappleHeldModel.wrap(model, empty));
        var itemModel = new net.minecraft.client.resources.model.ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "whisper_of_the_past"), "inventory");
        event.getModels().computeIfPresent(itemModel, (id, model) -> SonnetBowModel.wrapItem(model));
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "crystal_arrow"), CrystalArrowGeometry.LOADER);
    }

    @SubscribeEvent
    public static void registerArrowModel(ModelEvent.RegisterAdditional event) {
        event.register(GrappleRenderer.EMPTY_HELD_MODEL);
        for (var model : GrappleRenderer.MODELS) event.register(model);
        event.register(SonnetArrowRenderer.MODEL);
        event.register(SonnetVolleyRenderer.MODEL);
        for (var model : SonnetDomeRenderer.MODELS) event.register(model);
        for (var model : ResearchTableRenderer.MODELS) event.register(model);
        for (var model : ProductionLabRenderer.MODELS) event.register(model);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LyyEntities.ENDER_COMPANION.get(), EnderCompanionRenderer::new);
        event.registerBlockEntityRenderer(LyyBlockEntities.RESEARCH_TABLE.get(), ResearchTableRenderer::new);
        event.registerBlockEntityRenderer(LyyBlockEntities.PRODUCTION_LAB.get(), ProductionLabRenderer::new);
        event.registerEntityRenderer(LyyEntities.GRAPPLE_HOOK.get(), GrappleRenderer::new);
        event.registerEntityRenderer(LyyEntities.IMAGINARY_GUARDIAN.get(), context -> new GuardianRenderer<>(context, "boss"));
        event.registerEntityRenderer(LyyEntities.GUARDIAN_CRYSTAL.get(), context -> new GuardianRenderer<>(context, "projectile"));
        event.registerEntityRenderer(LyyEntities.GUARDIAN_SPIKES.get(), context -> new GuardianRenderer<>(context, "spikes"));
        event.registerEntityRenderer(LyyEntities.SONNET_DOME.get(), SonnetDomeRenderer::new);
        event.registerEntityRenderer(LyyEntities.SONNET_VOLLEY.get(), SonnetVolleyRenderer::new);
        event.registerEntityRenderer(LyyEntities.CRYSTAL_ARROW.get(), SonnetArrowRenderer::new);
        event.registerEntityRenderer(LyyEntities.CRYSTAL_SPECTRAL_ARROW.get(), SonnetArrowRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var bow = LyyItems.WHISPER_OF_THE_PAST.get();
            ItemProperties.register(bow, ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "crystal"),
                    (stack, level, entity, seed) -> org.lyy.lyycore.content.item.SonnetBowItem.isCrystal(stack) ? 1 : 0);
            ItemProperties.register(bow, ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "crystal_stage"),
                    (stack, level, entity, seed) -> org.lyy.lyycore.content.item.SonnetBowItem.crystalDrawStage(stack, level));
            ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> entity != null && entity.getUseItem() == stack
                            ? (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F
                            : 0.0F);
            ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
                            && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LyyMenus.IMAGINARY_REAPER.get(), ImaginaryReaperScreen::new);
        event.register(LyyMenus.ADVANCED_IMAGINARY_GATE.get(), AdvancedImaginaryGateScreen::new);
        event.register(LyyMenus.RESEARCH.get(), ResearchScreen::new);
        event.register(LyyMenus.MEMORY.get(), ResearchScreen::new);
        event.register(LyyMenus.PRODUCTION_LAB.get(), ProductionLabScreen::new);
        event.register(LyyMenus.IMAGINARY_ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(LyyMenus.IAF_MENU.get(), IAFScreen::new);
        event.register(LyyMenus.IMAGINARY_GATE.get(), ImaginaryGateScreen::new);
        event.register(LyyMenus.IMAGINARY_CRAFTING.get(), ImaginaryCraftingScreen::new);
        event.register(LyyMenus.CRYSTAL_CONDENSING.get(), org.lyy.lyycore.content.menu.CrystalCondensingScreen::new);
        event.register(LyyMenus.RESOURCE_GATHERING.get(), org.lyy.lyycore.content.menu.ResourceGatheringScreen::new);
    }

    @SubscribeEvent
    public static void registerWeatherColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> 0xFF94BFFF, LyyItems.STORM_BALL.get());
        event.register((stack, layer) -> 0xFFFFD45F, LyyItems.SUN_BALL.get());
    }
}
