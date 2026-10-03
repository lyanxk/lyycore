package org.lyy.lyycore.compat.jei;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.menu.IAFMenu;
import org.lyy.lyycore.content.menu.IAFScreen;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyMenus;
import org.lyy.lyycore.registry.LyyRecipes;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.menu.ResourceGatheringScreen;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class LyyJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new ImaginaryAlloyingCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new CrystalCondensingCategory(reg.getJeiHelpers().getGuiHelper()));
        for (var kind : ResourceFrameKind.values()) reg.addRecipeCategories(new ResourceGatheringCategory(reg.getJeiHelpers().getGuiHelper(), kind));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        Minecraft mc = Minecraft.getInstance();
        RecipeManager rm = null;
        if (mc.level != null) rm = mc.level.getRecipeManager();
        else if (mc.getConnection() != null) rm = mc.getConnection().getRecipeManager();
        if (rm == null) {
            LogUtils.getLogger().warn("[LyyCore JEI] RecipeManager is null; skipping recipe registration.");
            return;
        }

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
        reg.addRecipes(LyyJeiTypes.CRYSTAL_CONDENSING, rm.getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get())
                .stream().map(RecipeHolder::value).toList());
        for (var kind : ResourceFrameKind.values()) reg.addRecipes(LyyJeiTypes.RESOURCE_GATHERING.get(kind),
                rm.getAllRecipesFor(kind.recipeType()).stream().map(RecipeHolder::value).toList());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addGuiContainerHandler(ResourceGatheringScreen.class, new IGuiContainerHandler<ResourceGatheringScreen>() {
            @Override public java.util.Collection<IGuiClickableArea> getGuiClickableAreas(ResourceGatheringScreen screen, double x, double y) {
                if (screen.isSelectorOpen()) return List.of();
                return List.of(IGuiClickableArea.createBasic(61, 33, 15, 25,
                        LyyJeiTypes.RESOURCE_GATHERING.get(screen.getMenu().kind())));
            }
        });
        reg.addRecipeClickArea(org.lyy.lyycore.content.menu.CrystalCondensingScreen.class,
                org.lyy.lyycore.content.menu.CrystalCondensingScreen.RECIPE_X,
                org.lyy.lyycore.content.menu.CrystalCondensingScreen.RECIPE_Y,
                org.lyy.lyycore.content.menu.CrystalCondensingScreen.RECIPE_W,
                org.lyy.lyycore.content.menu.CrystalCondensingScreen.RECIPE_H, LyyJeiTypes.CRYSTAL_CONDENSING);
        reg.addRecipeClickArea(IAFScreen.class, IAFScreen.RECIPE_X, IAFScreen.RECIPE_Y,
                IAFScreen.RECIPE_W, IAFScreen.RECIPE_H, LyyJeiTypes.IMAGINARY_ALLOYING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        for (var kind : ResourceFrameKind.values()) reg.addRecipeCatalyst(new ItemStack(kind.block()), LyyJeiTypes.RESOURCE_GATHERING.get(kind));
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.CRYSTAL_CONDENSING_FRAME.get()), LyyJeiTypes.CRYSTAL_CONDENSING);
        reg.addRecipeCatalyst(new ItemStack(LyyBlocks.IAF.get()), LyyJeiTypes.IMAGINARY_ALLOYING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(IAFMenu.class, LyyMenus.IAF_MENU.get(),
                LyyJeiTypes.IMAGINARY_ALLOYING,
                0, 3,   // machine input slots start, count
                4, 36); // player inventory start, count
    }
}
