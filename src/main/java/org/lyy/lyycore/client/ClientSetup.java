package org.lyy.lyycore.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.menu.EnergyCellScreen;
import org.lyy.lyycore.content.menu.IAFScreen;
import org.lyy.lyycore.registry.LyyMenus;

@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LyyMenus.IMAGINARY_ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(LyyMenus.IAF_MENU.get(), IAFScreen::new);
    }
}
