package org.lyy.lyycore.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lyy.lyycore.LyyCore;

/** Short, consistently styled descriptions for the utility and machine family. */
@EventBusSubscriber(modid = LyyCore.MODID, value = Dist.CLIENT)
public final class UtilityTooltips {
    private UtilityTooltips() { }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(LyyCore.MODID)) return;
        String hint = switch (id.getPath()) {
            case "imaginary_energy_cell", "imaginary_alloy_forge", "crystal_condensing_frame" -> "open";
            case "tree_gathering_frame", "overworld_gathering_frame", "mineral_gathering_frame", "nether_gathering_frame" -> "open";
            case "tree_core", "overworld_core", "mineral_core", "nether_core" -> "";
            case "item_collector" -> "range";
            case "im_generator" -> "scan";
            case "imaginary_crystal", "imaginary_alloy_ingot", "im_battery",
                    "imaginium_ore", "deepslate_imaginium_ore" -> "";
            default -> null;
        };
        if (hint == null) return;
        event.getToolTip().add(Component.translatable("tooltip.lyycore.description." + id.getPath())
                .withStyle(style -> style.withColor(0xCBD2E2)));
        if (!hint.isEmpty()) {
            event.getToolTip().add(Component.translatable("tooltip.lyycore.action." + hint)
                    .withStyle(style -> style.withColor(0xDEA6C9)));
        }
    }
}
