package org.lyy.lyycore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;

public class LyyCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LyyCore.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(LyyItems.IMAGINARY_ALLOY_INGOT.get()))
                    .title(Component.literal("LyyCore"))
                    .displayItems((params, output) -> {
                        output.accept(LyyItems.IMAGINARY_ALLOY_INGOT.get());
                        output.accept(LyyItems.IM_BATTERY.get());
                        output.accept(LyyItems.IMAGINARY_DISASSEMBLER.get());
                        output.accept(LyyItems.RAW_IMAGINIUM.get());
                        output.accept(LyyBlocks.IAF.get());
                        output.accept(LyyBlocks.IMAGINARY_ENERGY_CELL.get());
                        output.accept(LyyBlocks.DEEPSLATE_IMAGINIUM_ORE.get());
                        output.accept(LyyBlocks.IMAGINIUM_ORE.get());
                        output.accept(LyyBlocks.ITEM_COLLECTOR.get());
                        output.accept(LyyBlocks.IGB.get());
                    })
                    .build());
}
