package org.lyy.lyycore.compat.jei;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.FrameProduction;
import org.lyy.lyycore.client.screen.CrystalCondensingScreen;
import org.lyy.lyycore.content.menu.IAFMenu;
import org.lyy.lyycore.client.screen.IAFScreen;
import org.lyy.lyycore.client.screen.ImaginaryCraftingScreen;
import org.lyy.lyycore.client.screen.ResourceGatheringScreen;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class LyyJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new EnergyProductionCategory(reg.getJeiHelpers().getGuiHelper(), true));
        reg.addRecipeCategories(new EnergyProductionCategory(reg.getJeiHelpers().getGuiHelper(), false));
        reg.addRecipeCategories(new FissionCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new PureSmeltingCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new AlloyCauldronCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new ImaginaryAlloyingCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new ImaginaryCraftingCategory<>(reg.getJeiHelpers().getGuiHelper(), LyyJeiTypes.IMAGINARY_CRAFTING, LyyBlocks.IMAGINARY_CRAFTING_TABLE.get()));
        reg.addRecipeCategories(new ImaginaryCraftingCategory<>(reg.getJeiHelpers().getGuiHelper(), LyyJeiTypes.IMAGINARY_CONDENSING, LyyBlocks.IMAGINARY_CONDENSING_BEACON.get()));
        reg.addRecipeCategories(new CrystalCondensingCategory(reg.getJeiHelpers().getGuiHelper(), FrameProduction.STANDARD));
        reg.addRecipeCategories(new CrystalCondensingCategory(reg.getJeiHelpers().getGuiHelper(), FrameProduction.MINIATURE_FACTORY));
        for (var kind : ResourceFrameKind.FACTORY_KINDS) reg.addRecipeCategories(new ResourceGatheringCategory(reg.getJeiHelpers().getGuiHelper(), kind, FrameProduction.MINIATURE_FACTORY));
        for (var kind : ResourceFrameKind.values()) reg.addRecipeCategories(new ResourceGatheringCategory(reg.getJeiHelpers().getGuiHelper(), kind, FrameProduction.STANDARD));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.EXPERIMENT_TABLE.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.experiment_table"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.HAIL.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.hail"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.DRAGON_MIGHT.get()), net.minecraft.network.chat.Component.translatable("research.lyycore.in_our_hands.description"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.FRIENDLY_PROOF.get()), net.minecraft.network.chat.Component.translatable("research.lyycore.ritual.description"));
        reg.addRecipes(AlloyCauldronCategory.TYPE, org.lyy.lyycore.content.cauldron.CauldronMixes.specials());
        Minecraft mc = Minecraft.getInstance();
        RecipeManager rm = null;
        if (mc.level != null) rm = mc.level.getRecipeManager();
        else if (mc.getConnection() != null) rm = mc.getConnection().getRecipeManager();
        if (rm == null) {
            LogUtils.getLogger().warn("[LyyCore JEI] RecipeManager is null; skipping recipe registration.");
            return;
        }

        if (mc.level != null) {
            reg.addRecipes(EnergyProductionCategory.OTHERWORLD, org.lyy.lyycore.content.ProductionCatalog.products(mc.level).stream()
                    .map(item -> new EnergyProductionCategory.View(item, List.of())).toList());
            var stones = List.of(net.minecraft.world.item.Items.STONE, net.minecraft.world.item.Items.COBBLESTONE,
                    net.minecraft.world.item.Items.DEEPSLATE, net.minecraft.world.item.Items.COBBLED_DEEPSLATE);
            var erosion = new java.util.ArrayList<EnergyProductionCategory.View>();
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                var template = new ItemStack(item);
                var inputs = stones.stream().map(ItemStack::new)
                        .filter(stone -> org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity.matches(template, stone)).toList();
                if (!inputs.isEmpty()) erosion.add(new EnergyProductionCategory.View(template, inputs));
            }
            reg.addRecipes(EnergyProductionCategory.EROSION, erosion);
        }
        reg.addItemStackInfo(new ItemStack(LyyBlocks.EROSION_FACTORY.get()), net.minecraft.network.chat.Component.translatable("gui.lyycore.erosion.template_hint"), net.minecraft.network.chat.Component.translatable("gui.lyycore.erosion.cost"));
        reg.addItemStackInfo(new ItemStack(LyyBlocks.OTHERWORLD_CHEST.get()), net.minecraft.network.chat.Component.translatable("gui.lyycore.otherworld.take"), net.minecraft.network.chat.Component.translatable("gui.lyycore.otherworld.cost"));
        List<ImaginaryAlloyingRecipe> recipes = rm.getAllRecipesFor(LyyRecipes.IMAGINARY_ALLOYING.get())
                .stream().map(RecipeHolder::value).toList();
        if (recipes.isEmpty()) {
            // Fallback: when type-key lookup misses, still collect by concrete recipe class.
            recipes = rm.getRecipes().stream()
                    .map(RecipeHolder::value)
                    .filter(ImaginaryAlloyingRecipe.class::isInstance)
                    .map(ImaginaryAlloyingRecipe.class::cast)
                    .toList();
        }

        LogUtils.getLogger().info("[LyyCore JEI] imaginary_alloying recipes = {}", recipes.size());
        if (recipes.isEmpty()) {
            long modRecipeCount = rm.getRecipes().stream()
                    .filter(h -> h.id().getNamespace().equals(LyyCore.MODID))
                    .count();
            List<String> modRecipeIds = rm.getRecipes().stream()
                    .filter(h -> h.id().getNamespace().equals(LyyCore.MODID))
                    .map(h -> h.id().toString())
                    .sorted()
                    .collect(Collectors.toList());
            LogUtils.getLogger().warn("[LyyCore JEI] no imaginary_alloying recipes found. lyycore recipe count={}, ids={}",
                    modRecipeCount, modRecipeIds);
        }
        reg.addRecipes(LyyJeiTypes.IMAGINARY_ALLOYING, recipes);
        reg.addRecipes(PureSmeltingCategory.TYPE, rm.getAllRecipesFor(LyyRecipes.PURE_SMELTING.get()).stream().map(RecipeHolder::value).toList());
        reg.addRecipes(FissionCategory.TYPE, rm.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING).stream().map(RecipeHolder::value).toList());
        reg.addItemStackInfo(new ItemStack(LyyBlocks.PURE_SMELTING_PLANT_SHELL.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.pure_smelting.activation"));
        reg.addRecipes(LyyJeiTypes.IMAGINARY_CONDENSING, rm.getAllRecipesFor(LyyRecipes.IMAGINARY_CONDENSING.get()).stream().map(RecipeHolder::value).toList());
        reg.addRecipes(LyyJeiTypes.IMAGINARY_CRAFTING, rm.getAllRecipesFor(LyyRecipes.IMAGINARY_CRAFTING.get())
                .stream().map(RecipeHolder::value).toList());
        reg.addRecipes(LyyJeiTypes.CRYSTAL_CONDENSING, rm.getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get())
                .stream().map(RecipeHolder::value).toList());
        reg.addRecipes(LyyJeiTypes.MINIATURE_CRYSTAL, rm.getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get())
                .stream().map(RecipeHolder::value).toList());
        for (var kind : ResourceFrameKind.FACTORY_KINDS) reg.addRecipes(LyyJeiTypes.RESOURCE_FACTORIES.get(kind),
                rm.getAllRecipesFor(kind.recipeType()).stream().map(RecipeHolder::value).toList());
        for (var kind : ResourceFrameKind.values()) reg.addRecipes(LyyJeiTypes.RESOURCE_GATHERING.get(kind),
                rm.getAllRecipesFor(kind.recipeType()).stream().map(RecipeHolder::value).toList());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addRecipeClickArea(org.lyy.lyycore.client.screen.ErosionFactoryScreen.class, 104, 40, 26, 18, EnergyProductionCategory.EROSION);
        reg.addRecipeClickArea(org.lyy.lyycore.client.screen.FissionFurnaceScreen.class, 62, 38, 36, 20, FissionCategory.TYPE);
        reg.addGuiContainerHandler(ImaginaryCraftingScreen.class, new IGuiContainerHandler<ImaginaryCraftingScreen>() {
            @Override public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(ImaginaryCraftingScreen screen, double x, double y) {
                var types = screen.getMenu().condensing() ? new mezz.jei.api.recipe.RecipeType<?>[]{LyyJeiTypes.IMAGINARY_CONDENSING, LyyJeiTypes.IMAGINARY_CRAFTING}
                        : new mezz.jei.api.recipe.RecipeType<?>[]{LyyJeiTypes.IMAGINARY_CRAFTING};
                return List.of(IGuiClickableArea.createBasic(ImaginaryCraftingScreen.RECIPE_X, ImaginaryCraftingScreen.RECIPE_Y,
                        ImaginaryCraftingScreen.RECIPE_W, ImaginaryCraftingScreen.RECIPE_H, types));
            }
        });
        reg.addGuiContainerHandler(ResourceGatheringScreen.class, new IGuiContainerHandler<ResourceGatheringScreen>() {
            @Override public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(ResourceGatheringScreen screen, double x, double y) {
                if (screen.isSelectorOpen()) return List.of();
                return List.of(IGuiClickableArea.createBasic(61, 33, 15, 25,
                        LyyJeiTypes.gathering(screen.getMenu().kind(), screen.getMenu().production())));
            }
        });
        reg.addGuiContainerHandler(CrystalCondensingScreen.class, new IGuiContainerHandler<CrystalCondensingScreen>() {
            @Override public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(CrystalCondensingScreen screen, double x, double y) {
                return List.of(IGuiClickableArea.createBasic(CrystalCondensingScreen.RECIPE_X, CrystalCondensingScreen.RECIPE_Y,
                        CrystalCondensingScreen.RECIPE_W, CrystalCondensingScreen.RECIPE_H,
                        LyyJeiTypes.crystal(screen.getMenu().production())));
            }
        });
        reg.addRecipeClickArea(IAFScreen.class, IAFScreen.RECIPE_X, IAFScreen.RECIPE_Y,
                IAFScreen.RECIPE_W, IAFScreen.RECIPE_H, LyyJeiTypes.IMAGINARY_ALLOYING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.EROSION_FACTORY.get()), EnergyProductionCategory.EROSION);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.OTHERWORLD_CHEST.get()), EnergyProductionCategory.OTHERWORLD);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.FISSION_FURNACE.get()), FissionCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.PURE_SMELTING_PLANT.get()), PureSmeltingCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.ALLOY_CAULDRON.get()), AlloyCauldronCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.IMAGINARY_CONDENSING_BEACON.get()), LyyJeiTypes.IMAGINARY_CONDENSING, LyyJeiTypes.IMAGINARY_CRAFTING);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.IMAGINARY_CRAFTING_TABLE.get()), LyyJeiTypes.IMAGINARY_CRAFTING);
        for (var kind : ResourceFrameKind.values()) reg.addRecipeCatalyst(new ItemStack(kind.block()), LyyJeiTypes.RESOURCE_GATHERING.get(kind));
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.MINIATURE_CRYSTAL_FACTORY.get()), LyyJeiTypes.MINIATURE_CRYSTAL);
        for (var kind : ResourceFrameKind.FACTORY_KINDS) reg.addRecipeCatalyst(new ItemStack(kind.factoryBlock()), LyyJeiTypes.RESOURCE_FACTORIES.get(kind));
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.CRYSTAL_CONDENSING_FRAME.get()), LyyJeiTypes.CRYSTAL_CONDENSING);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.IAF.get()), LyyJeiTypes.IMAGINARY_ALLOYING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(org.lyy.lyycore.content.menu.FissionFurnaceMenu.class, LyyMenus.FISSION_FURNACE.get(), FissionCategory.TYPE, 0, 1, 3, 36);
        reg.addRecipeTransferHandler(new ImaginaryCraftingTransferInfo<>(reg.getTransferHelper(), LyyJeiTypes.IMAGINARY_CRAFTING, false));
        reg.addRecipeTransferHandler(new ImaginaryCraftingTransferInfo<>(reg.getTransferHelper(), LyyJeiTypes.IMAGINARY_CONDENSING, true));
        reg.addRecipeTransferHandler(IAFMenu.class, LyyMenus.IAF_MENU.get(),
                LyyJeiTypes.IMAGINARY_ALLOYING,
                0, 3,   // machine input slots start, count
                4, 36); // player inventory start, count
    }
}
