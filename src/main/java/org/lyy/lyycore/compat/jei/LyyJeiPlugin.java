package org.lyy.lyycore.compat.jei;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.FrameProduction;
import org.lyy.lyycore.content.equipment.Accessory;
import org.lyy.lyycore.content.equipment.SpacetimeAccessoryItem;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.client.screen.CrystalCondensingScreen;
import org.lyy.lyycore.content.menu.IAFMenu;
import org.lyy.lyycore.content.menu.SpacetimeFactoryMenu;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.menu.FissionFurnaceMenu;
import org.lyy.lyycore.client.screen.IAFScreen;
import org.lyy.lyycore.client.screen.ImaginaryCraftingScreen;
import org.lyy.lyycore.client.screen.ResourceGatheringScreen;
import org.lyy.lyycore.client.screen.SpacetimeFactoryScreen;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyMenus;
import org.lyy.lyycore.registry.LyyRecipes;
import java.util.List;
import java.util.function.Supplier;

@JeiPlugin
public class LyyJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new SpacetimeCategory(reg.getJeiHelpers().getGuiHelper()));
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
        for (var entry : LyyItems.ITEMS.getEntries()) {
            if (!(entry.get() instanceof Accessory)) continue;
            addDynamicInfo(reg, entry.get().getDefaultInstance(), () -> {
                var equipmentInfo = Component.translatable("jei.lyycore.accessory", Component.keybind("key.lyycore.memory"));
                return entry.get() instanceof SpacetimeAccessoryItem
                        ? List.of(equipmentInfo, Component.translatable("jei.lyycore.spacetime_blessing"))
                        : List.of(equipmentInfo);
            });
        }
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.SUMMONING_BEACON.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.summoning_beacon"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.EXPERIMENT_TABLE.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.experiment_table"));
        addWeaponInfo(reg, LyyItems.EMBERFALL.get(), "jei.lyycore.emberfall");
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.CAPTURE_CAGE.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.capture_cage"));
        addWeaponInfo(reg, LyyItems.HAIL.get(), "jei.lyycore.hail");
        addWeaponInfo(reg, LyyItems.DRAGON_MIGHT.get(), "jei.lyycore.dragon_might");
        addWeaponInfo(reg, LyyItems.WHISPER_OF_THE_PAST.get(), "jei.lyycore.whisper_of_the_past");
        addWeaponInfo(reg, LyyItems.THUNDER_SCYTHE.get(), "jei.lyycore.thunder_scythe");
        addDynamicInfo(reg, LyyItems.RIFTBREAKER.get().getDefaultInstance(), () -> List.of(
                Component.translatable("jei.lyycore.riftbreaker", Component.keybind("key.lyycore.tool_mode"))));
        addWeaponInfo(reg, LyyItems.LIUXI.get(), "jei.lyycore.liuxi");
        addWeaponInfo(reg, LyyItems.FROZEN_MOMENT.get(), "jei.lyycore.frozen_moment");
        reg.addItemStackInfo(LyyItems.CELESTIAL_ORDER.get().getDefaultInstance(), Component.translatable("jei.lyycore.celestial_order"));
        reg.addItemStackInfo(new ItemStack(LyyItems.SUCHEN.get()), Component.translatable("jei.lyycore.suchen"));
        reg.addItemStackInfo(new ItemStack(LyyItems.GREEN_ONION.get()), Component.translatable("jei.lyycore.green_onion"));
        addWeaponInfo(reg, LyyItems.IMAGINARY_REAPER.get(), "jei.lyycore.imaginary_reaper");
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.FRIENDLY_PROOF.get()), net.minecraft.network.chat.Component.translatable("research.lyycore.ritual.description"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.TIME_KNOCKING_STONE.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.spacetime_trial"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.SPACETIME_TRIAL_PROOF.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.spacetime_trial_proof"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.SPACETIME_SHARD.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.spacetime_shard"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.SPACETIME_MATRIX.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.spacetime_matrix"));
        reg.addItemStackInfo(new ItemStack(org.lyy.lyycore.registry.LyyItems.SPACETIME_COORDINATE_DEVICE.get()), net.minecraft.network.chat.Component.translatable("jei.lyycore.spacetime_coordinates"));
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
            var researchEntries = ResearchManager.all(mc.level).stream()
                    .sorted(java.util.Comparator.comparing(entry -> entry.id().toString())).toList();
            for (var entry : researchEntries) {
                var research = entry.value();
                var production = research.production().orElse(null);
                if (production == null) continue;
                addDynamicInfo(reg, production.result(), () -> List.of(
                        Component.translatable("jei.lyycore.research_obtain",
                                Component.translatable(research.title()), Component.keybind("key.lyycore.memory")),
                        Component.translatable(production.experiment().isPresent()
                                ? "jei.lyycore.research_experiment" : "jei.lyycore.research_production")));
            }
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
        reg.addRecipes(LyyJeiTypes.IMAGINARY_ALLOYING, recipes);
        reg.addRecipes(PureSmeltingCategory.TYPE, rm.getAllRecipesFor(LyyRecipes.PURE_SMELTING.get()).stream().map(RecipeHolder::value).toList());
        reg.addRecipes(SpacetimeCategory.TYPE, rm.getAllRecipesFor(LyyRecipes.SPACETIME.get()).stream().map(RecipeHolder::value).toList());
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

    private static void addWeaponInfo(IRecipeRegistration reg, Item item, String descriptionKey) {
        addDynamicInfo(reg, item.getDefaultInstance(), () -> List.of(
                Component.translatable(descriptionKey, Component.keybind("key.lyycore.special_skill"))));
    }

    /** Resolve translated text and key bindings when the information page opens. */
    private static void addDynamicInfo(IRecipeRegistration reg, ItemStack stack, Supplier<List<FormattedText>> description) {
        var ingredient = reg.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, stack, false).orElseThrow();
        reg.addRecipes(RecipeTypes.INFORMATION, List.of(new IJeiIngredientInfoRecipe() {
            @Override public List<ITypedIngredient<?>> getIngredients() { return List.of(ingredient); }
            @Override public List<FormattedText> getDescription() { return description.get(); }
        }));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addGuiContainerHandler(SpacetimeFactoryScreen.class, new IGuiContainerHandler<SpacetimeFactoryScreen>() {
            @Override public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(SpacetimeFactoryScreen screen, double x, double y) {
                var type = switch (screen.getMenu().page()) {
                    case SPACETIME -> SpacetimeCategory.TYPE;
                    case CONDENSING -> LyyJeiTypes.IMAGINARY_CONDENSING;
                    case CRAFTING -> LyyJeiTypes.IMAGINARY_CRAFTING;
                    case ALLOYING -> LyyJeiTypes.IMAGINARY_ALLOYING;
                };
                return List.of(IGuiClickableArea.createBasic(screen.recipeX(), screen.recipeY(), 20, 12, type));
            }
        });
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
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.SPACETIME_FACTORY.get()), SpacetimeCategory.TYPE,
                LyyJeiTypes.IMAGINARY_CONDENSING, LyyJeiTypes.IMAGINARY_CRAFTING, LyyJeiTypes.IMAGINARY_ALLOYING);
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
        var helper = reg.getTransferHelper();
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, SpacetimeFactoryMenu.class, LyyMenus.SPACETIME_FACTORY.get(), SpacetimeCategory.TYPE), SpacetimeCategory.TYPE);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, SpacetimeFactoryMenu.class, LyyMenus.SPACETIME_FACTORY.get(), LyyJeiTypes.IMAGINARY_CONDENSING), LyyJeiTypes.IMAGINARY_CONDENSING);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, SpacetimeFactoryMenu.class, LyyMenus.SPACETIME_FACTORY.get(), LyyJeiTypes.IMAGINARY_CRAFTING), LyyJeiTypes.IMAGINARY_CRAFTING);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, SpacetimeFactoryMenu.class, LyyMenus.SPACETIME_FACTORY.get(), LyyJeiTypes.IMAGINARY_ALLOYING), LyyJeiTypes.IMAGINARY_ALLOYING);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, FissionFurnaceMenu.class, LyyMenus.FISSION_FURNACE.get(), FissionCategory.TYPE), FissionCategory.TYPE);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, ImaginaryCraftingMenu.class, LyyMenus.IMAGINARY_CRAFTING.get(), LyyJeiTypes.IMAGINARY_CRAFTING), LyyJeiTypes.IMAGINARY_CRAFTING);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, ImaginaryCraftingMenu.class, LyyMenus.IMAGINARY_CRAFTING.get(), LyyJeiTypes.IMAGINARY_CONDENSING), LyyJeiTypes.IMAGINARY_CONDENSING);
        reg.addRecipeTransferHandler(new MachineRecipeTransferHandler<>(helper, IAFMenu.class, LyyMenus.IAF_MENU.get(), LyyJeiTypes.IMAGINARY_ALLOYING), LyyJeiTypes.IMAGINARY_ALLOYING);
    }
}
