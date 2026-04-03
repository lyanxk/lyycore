package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.menu.EnergyCellMenu;
import org.lyy.lyycore.content.menu.IAFMenu;

public class LyyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LyyCore.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyCellMenu>> IMAGINARY_ENERGY_CELL =
            MENUS.register("imaginary_energy_cell",
                    () -> IMenuTypeExtension.create(EnergyCellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<IAFMenu>> IAF_MENU =
            MENUS.register("iaf_menu",
                    () -> IMenuTypeExtension.create(IAFMenu::new));
}
