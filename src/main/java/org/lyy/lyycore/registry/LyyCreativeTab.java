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
                        output.accept(LyyItems.PURE_CRYSTAL.get());
                        output.accept(LyyBlocks.CRYSTAL_BLOCK.get());
                        output.accept(LyyBlocks.ALLOY_BLOCK.get());
                        output.accept(LyyBlocks.IMAGINARY_GATE.get());
                        output.accept(LyyBlocks.ADVANCED_IMAGINARY_GATE.get());
                        output.accept(LyyBlocks.ENDER_SENTRY.get());
                        output.accept(LyyBlocks.IMAGINARY_CRAFTING_TABLE.get());
                        output.accept(LyyBlocks.IMAGINARY_RESEARCH_TABLE.get());
                        output.accept(LyyBlocks.PRODUCTION_LAB.get());
                        output.accept(LyyItems.IMAGINARY_DISASSEMBLER.get());
                        output.accept(LyyItems.IMAGINARY_REAPER.get());
                        output.accept(LyyItems.LIGHTNING_BOTTLE.get());
                        output.accept(LyyItems.STORM_BALL.get());
                        output.accept(LyyItems.SUN_BALL.get());
                        output.accept(LyyItems.IMAGINARY_GRAPPLE.get());
                        output.accept(LyyItems.WHISPER_OF_THE_PAST.get());
                        output.accept(LyyItems.IMAGINARY_CRYSTAL.get());
                        output.accept(LyyBlocks.IAF.get());
                        output.accept(LyyBlocks.IMAGINARY_ENERGY_CELL.get());
                        output.accept(LyyBlocks.DEEPSLATE_IMAGINIUM_ORE.get());
                        output.accept(LyyBlocks.IMAGINIUM_ORE.get());
                        output.accept(LyyBlocks.ITEM_COLLECTOR.get());
                        output.accept(LyyBlocks.IGB.get());
                        output.accept(LyyBlocks.CRYSTAL_CONDENSING_FRAME.get());
                        output.accept(LyyBlocks.MINIATURE_CRYSTAL_FACTORY.get());
                        for (var kind : org.lyy.lyycore.content.ResourceFrameKind.FACTORY_KINDS) output.accept(kind.factoryBlock());
                        for (var kind : org.lyy.lyycore.content.ResourceFrameKind.values()) {
                            output.accept(kind.core());
                            output.accept(kind.block());
                        }
                    })
                    .build());
}
