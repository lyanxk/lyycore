package org.lyy.lyycore.client;

import org.lyy.lyycore.client.screen.ImaginaryGateScreen;
import org.lyy.lyycore.client.screen.AdvancedImaginaryGateScreen;
import org.lyy.lyycore.client.screen.ImaginaryCraftingScreen;
import org.lyy.lyycore.client.screen.ResearchScreen;
import org.lyy.lyycore.client.screen.ProductionLabScreen;
import org.lyy.lyycore.client.screen.ImaginaryReaperScreen;
import org.lyy.lyycore.registry.LyyBlockEntities;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.client.screen.EnergyCellScreen;
import org.lyy.lyycore.client.screen.IAFScreen;
import org.lyy.lyycore.registry.LyyMenus;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent public static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event) {
        org.lyy.lyycore.content.ProductionCatalog.clear();
    }
    @SubscribeEvent public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new AegisWingsLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void uploadDomeMeshes(ModelEvent.BakingCompleted event) {
        SonnetDomeMesh.reload(event.getModelManager());
        ProductionLabRenderer.reload();
        DragonMightRenderer.reload();
        HailItemRenderer.reload();
    }

    @SubscribeEvent
    public static void wrapCustomItemModels(ModelEvent.ModifyBakingResult event) {
        var hail = new net.minecraft.client.resources.model.ModelResourceLocation(LyyItems.HAIL.getId(), "inventory");
        event.getModels().computeIfPresent(hail, (id, model) -> HailItemRenderer.wrap(model));
        var dragonMight = new net.minecraft.client.resources.model.ModelResourceLocation(LyyItems.DRAGON_MIGHT.getId(), "inventory");
        event.getModels().computeIfPresent(dragonMight, (id, model) -> DragonMightRenderer.wrap(model));
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
        event.register(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "machine_mesh"), MachineMeshGeometry.LOADER);
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
        event.registerBlockEntityRenderer(LyyBlockEntities.EXPERIMENT_TABLE.get(), ExperimentTableRenderer::new);
        event.registerEntityRenderer(LyyEntities.HAIL_CRYSTAL.get(), HailCrystalRenderer::new);
        event.registerEntityRenderer(LyyEntities.HAIL_FLOWER.get(), HailFlowerRenderer::new);
        event.registerEntityRenderer(LyyEntities.MAGIC_BEAM.get(), MagicBeamRenderer::new);
        event.registerEntityRenderer(LyyEntities.LIFE_DEFENDER.get(), c -> new LifeSovereignRenderer(c, "life_defender", .9F));
        event.registerEntityRenderer(LyyEntities.LIFE_COCOON.get(), c -> new LifeSovereignRenderer(c, "life_cocoon", .6F));
        event.registerEntityRenderer(LyyEntities.LIFE_USURPER.get(), c -> new LifeSovereignRenderer(c, "life_usurper", .76F));
        event.registerEntityRenderer(LyyEntities.LIFE_SPELL.get(), LifeSpellRenderer::new);
        event.registerEntityRenderer(LyyEntities.DEFENDER_SWORD.get(), DefenderSwordRenderer::new);
        event.registerEntityRenderer(LyyEntities.RECON_CRYSTAL.get(), context -> new CrystalTroopRenderer(context, false));
        event.registerEntityRenderer(LyyEntities.ASSAULT_CRYSTAL.get(), context -> new CrystalTroopRenderer(context, true));
        event.registerEntityRenderer(LyyEntities.IMAGINARY_DRAGON.get(), ImaginaryDragonRenderer::new);
        event.registerEntityRenderer(LyyEntities.GATE_METEOR.get(), GateMeteorRenderer::new);
        event.registerBlockEntityRenderer(LyyBlockEntities.PHANTOM_MATRIX.get(), context -> new EnergyMachineRenderer<>("phantom_matrix", "working"));
        event.registerBlockEntityRenderer(LyyBlockEntities.SUMMONING_ALTAR.get(), context -> new SummoningPedestalRenderer<>());
        event.registerBlockEntityRenderer(LyyBlockEntities.SUMMONING_PEDESTAL.get(), context -> new SummoningPedestalRenderer<>());
        event.registerBlockEntityRenderer(LyyBlockEntities.PURE_SMELTING_PLANT.get(), PureSmeltingRenderer::new);
        event.registerEntityRenderer(LyyEntities.SCOOP_FEATHER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(LyyEntities.GUIDING_LIGHT.get(), context -> new GuidingMobRenderer<>(context, "guiding_light"));
        event.registerEntityRenderer(LyyEntities.ENDLESS_DEMAND.get(), context -> new GuidingMobRenderer<>(context, "endless_demand"));
        event.registerEntityRenderer(LyyEntities.LOST_ADHERENT.get(), context -> new GuidingMobRenderer<>(context, "lost_follower"));
        event.registerEntityRenderer(LyyEntities.FANATICAL_SUPPORTER.get(), context -> new GuidingMobRenderer<>(context, "fanatic_supporter"));
        event.registerEntityRenderer(LyyEntities.GUIDING_LASER.get(), GuidingEffectRenderer::new);
        event.registerEntityRenderer(LyyEntities.GUIDING_GRAB.get(), GuidingGrabRenderer::new);
        event.registerBlockEntityRenderer(LyyBlockEntities.MIND_CONTROL_BEACON.get(), context -> new EnergyMachineRenderer<>("mind_control_beacon", "idle"));
        event.registerBlockEntityRenderer(LyyBlockEntities.ALLOY_CAULDRON.get(), AlloyCauldronRenderer::new);
        event.registerBlockEntityRenderer(LyyBlockEntities.IMAGINARY_CONDENSING_BEACON.get(), context -> new EnergyMachineRenderer<>("imaginary_condensing_beacon", "idle"));
        event.registerBlockEntityRenderer(LyyBlockEntities.SPATIAL_TRANSMISSION_TOWER.get(), context -> new EnergyMachineRenderer<>("spatial_transmission_tower", "working"));
        event.registerEntityRenderer(LyyEntities.LIFE_REVEL.get(), context -> new RevelRenderer<>(context, "life_revel"));
        event.registerEntityRenderer(LyyEntities.REVEL_DANCER.get(), context -> new RevelRenderer<>(context, "revel_dancer"));
        event.registerEntityRenderer(LyyEntities.REVEL_BLIND.get(), context -> new RevelRenderer<>(context, "revel_blind"));
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
        event.register(LyyMenus.EXPERIMENT_TABLE.get(), org.lyy.lyycore.client.screen.ExperimentTableScreen::new);
        event.register(LyyMenus.EROSION_FACTORY.get(), org.lyy.lyycore.client.screen.ErosionFactoryScreen::new);
        event.register(LyyMenus.OTHERWORLD_CHEST.get(), org.lyy.lyycore.client.screen.OtherworldChestScreen::new);
        event.register(LyyMenus.DRAGON_CONTROL.get(), org.lyy.lyycore.client.screen.DragonControlScreen::new);
        event.register(LyyMenus.IMAGINARY_REAPER.get(), ImaginaryReaperScreen::new);
        event.register(LyyMenus.FISSION_FURNACE.get(), org.lyy.lyycore.client.screen.FissionFurnaceScreen::new);
        event.register(LyyMenus.PURE_SMELTING.get(), org.lyy.lyycore.client.screen.PureSmeltingScreen::new);
        event.register(LyyMenus.DRAGON_NEST.get(), org.lyy.lyycore.client.screen.DragonNestScreen::new);
        event.register(LyyMenus.ADVANCED_IMAGINARY_GATE.get(), AdvancedImaginaryGateScreen::new);
        event.register(LyyMenus.RESEARCH.get(), ResearchScreen::new);
        event.register(LyyMenus.MEMORY.get(), ResearchScreen::new);
        event.register(LyyMenus.PRODUCTION_LAB.get(), ProductionLabScreen::new);
        event.register(LyyMenus.IMAGINARY_ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(LyyMenus.IAF_MENU.get(), IAFScreen::new);
        event.register(LyyMenus.IMAGINARY_GATE.get(), ImaginaryGateScreen::new);
        event.register(LyyMenus.IMAGINARY_CRAFTING.get(), ImaginaryCraftingScreen::new);
        event.register(LyyMenus.MIND_CONTROL.get(), org.lyy.lyycore.client.screen.MindControlScreen::new);
        event.register(LyyMenus.CRYSTAL_CONDENSING.get(), org.lyy.lyycore.client.screen.CrystalCondensingScreen::new);
        event.register(LyyMenus.RESOURCE_GATHERING.get(), org.lyy.lyycore.client.screen.ResourceGatheringScreen::new);
    }

    @SubscribeEvent
    public static void registerWaterColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level, pos) : 0x3F76E4, org.lyy.lyycore.registry.LyyBlocks.ALLOY_CAULDRON.get());
    }
    @SubscribeEvent
    public static void registerWeatherColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> 0xFF94BFFF, LyyItems.STORM_BALL.get());
        event.register((stack, layer) -> 0xFFFFD45F, LyyItems.SUN_BALL.get());
    }
}
