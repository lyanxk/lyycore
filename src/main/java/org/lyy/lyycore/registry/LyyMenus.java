package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.menu.CrystalCondensingMenu;
import org.lyy.lyycore.content.menu.EnergyCellMenu;
import org.lyy.lyycore.content.menu.IAFMenu;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.menu.ImaginaryGateMenu;
import org.lyy.lyycore.content.menu.AdvancedImaginaryGateMenu;
import org.lyy.lyycore.content.menu.ResourceGatheringMenu;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.content.menu.ProductionLabMenu;
import org.lyy.lyycore.content.menu.ImaginaryReaperMenu;

public class LyyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LyyCore.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.ExperimentTableMenu>> EXPERIMENT_TABLE = MENUS.register("experiment_table", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.ExperimentTableMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.ErosionFactoryMenu>> EROSION_FACTORY =
            MENUS.register("erosion_factory", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.ErosionFactoryMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.OtherworldChestMenu>> OTHERWORLD_CHEST =
            MENUS.register("otherworld_chest", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.OtherworldChestMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.DragonControlMenu>> DRAGON_CONTROL =
            MENUS.register("dragon_control", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.DragonControlMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.FissionFurnaceMenu>> FISSION_FURNACE =
            MENUS.register("fission_furnace", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.FissionFurnaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.PureSmeltingMenu>> PURE_SMELTING =
            MENUS.register("pure_smelting", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.PureSmeltingMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.DragonNestMenu>> DRAGON_NEST =
            MENUS.register("dragon_nest", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.DragonNestMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<org.lyy.lyycore.content.menu.MindControlMenu>> MIND_CONTROL =
            MENUS.register("mind_control", () -> IMenuTypeExtension.create(org.lyy.lyycore.content.menu.MindControlMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ImaginaryReaperMenu>> IMAGINARY_REAPER =
            MENUS.register("imaginary_reaper", () -> IMenuTypeExtension.create(ImaginaryReaperMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedImaginaryGateMenu>> ADVANCED_IMAGINARY_GATE =
            MENUS.register("advanced_imaginary_gate", () -> IMenuTypeExtension.create(AdvancedImaginaryGateMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ResearchMenu>> MEMORY =
            MENUS.register("memory", () -> IMenuTypeExtension.create(ResearchMenu::memory));
    public static final DeferredHolder<MenuType<?>, MenuType<ProductionLabMenu>> PRODUCTION_LAB =
            MENUS.register("production_lab", () -> IMenuTypeExtension.create(ProductionLabMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ResearchMenu>> RESEARCH =
            MENUS.register("research", () -> IMenuTypeExtension.create(ResearchMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ImaginaryGateMenu>> IMAGINARY_GATE =
            MENUS.register("imaginary_gate", () -> IMenuTypeExtension.create(ImaginaryGateMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ImaginaryCraftingMenu>> IMAGINARY_CRAFTING =
            MENUS.register("imaginary_crafting", () -> IMenuTypeExtension.create(ImaginaryCraftingMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ResourceGatheringMenu>> RESOURCE_GATHERING =
            MENUS.register("resource_gathering", () -> IMenuTypeExtension.create(ResourceGatheringMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CrystalCondensingMenu>> CRYSTAL_CONDENSING =
            MENUS.register("crystal_condensing", () -> IMenuTypeExtension.create(CrystalCondensingMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyCellMenu>> IMAGINARY_ENERGY_CELL =
            MENUS.register("imaginary_energy_cell",
                    () -> IMenuTypeExtension.create(EnergyCellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<IAFMenu>> IAF_MENU =
            MENUS.register("iaf_menu",
                    () -> IMenuTypeExtension.create(IAFMenu::new));
}
