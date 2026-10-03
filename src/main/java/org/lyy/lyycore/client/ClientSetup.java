package org.lyy.lyycore.client;

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
    }

    @SubscribeEvent
    public static void addCrystalBowGlow(ModelEvent.ModifyBakingResult event) {
        var itemModel = new net.minecraft.client.resources.model.ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "whisper_of_the_past"), "inventory");
        event.getModels().computeIfPresent(itemModel, (id, model) -> SonnetCrystalGlowModel.wrapItem(model));
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "crystal_arrow"), CrystalArrowGeometry.LOADER);
    }

    @SubscribeEvent
    public static void registerArrowModel(ModelEvent.RegisterAdditional event) {
        event.register(SonnetArrowRenderer.MODEL);
        event.register(SonnetVolleyRenderer.MODEL);
        for (var model : SonnetDomeRenderer.MODELS) event.register(model);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
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
        event.register(LyyMenus.IMAGINARY_ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(LyyMenus.IAF_MENU.get(), IAFScreen::new);
        event.register(LyyMenus.CRYSTAL_CONDENSING.get(), org.lyy.lyycore.content.menu.CrystalCondensingScreen::new);
        event.register(LyyMenus.RESOURCE_GATHERING.get(), org.lyy.lyycore.content.menu.ResourceGatheringScreen::new);
    }
}
