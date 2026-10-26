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
                    .icon(() -> new ItemStack(LyyItems.IMAGINARY_STEEL_BLOCK.get()))
                    .title(Component.literal("LyyCore"))
                    .displayItems((params, output) -> {
                        // Starting materials.
                        output.accept(LyyBlocks.IMAGINIUM_ORE.get());
                        output.accept(LyyBlocks.DEEPSLATE_IMAGINIUM_ORE.get());
                        output.accept(LyyItems.IMAGINARY_CRYSTAL.get());
                        output.accept(LyyItems.PURE_CRYSTAL.get());
                        output.accept(LyyBlocks.CRYSTAL_BLOCK.get());
                        output.accept(LyyItems.IMAGINARY_ALLOY_INGOT.get());
                        output.accept(LyyBlocks.ALLOY_BLOCK.get());
                        output.accept(LyyItems.IM_BATTERY.get());
                        // Starting production and research.
                        output.accept(LyyBlocks.IGB.get());
                        output.accept(LyyBlocks.IMAGINARY_ENERGY_CELL.get());
                        output.accept(LyyBlocks.IAF.get());
                        output.accept(LyyBlocks.CRYSTAL_CONDENSING_FRAME.get());
                        output.accept(LyyBlocks.ITEM_COLLECTOR.get());
                        output.accept(LyyBlocks.IMAGINARY_CRAFTING_TABLE.get());
                        output.accept(LyyBlocks.IMAGINARY_RESEARCH_TABLE.get());
                        output.accept(LyyBlocks.PRODUCTION_LAB.get());
                        for (var kind : org.lyy.lyycore.content.ResourceFrameKind.values()) {
                            output.accept(kind.core());
                            output.accept(kind.block());
                        }
                        // Exploration and the first challenge.
                        output.accept(LyyItems.IMAGINARY_DISASSEMBLER.get());
                        output.accept(LyyItems.IMAGINARY_REAPER.get());
                        output.accept(LyyItems.IMAGINARY_GRAPPLE.get());
                        output.accept(LyyBlocks.IMAGINARY_GATE.get());
                        output.accept(LyyBlocks.ADVANCED_IMAGINARY_GATE.get());
                        output.accept(LyyBlocks.ENDER_SENTRY.get());
                        output.accept(LyyItems.SMALL_IMAGINARY_CORE.get());
                        output.accept(LyyItems.HEART_OF_NOTHINGNESS.get());
                        output.accept(LyyItems.IMAGINARY_CONDENSING_BEACON.get());
                        output.accept(LyyItems.SPATIAL_TRANSMISSION_TOWER.get());
                        output.accept(LyyItems.COORDINATE_DEVICE.get());
                        output.accept(LyyItems.SUPERMUTATION_FACTOR.get());
                        // Potions and further exploration.
                        output.accept(LyyBlocks.ALLOY_CAULDRON.get());
                        output.accept(LyyItems.INCOMPLETE_POTION.get());
                        output.accept(LyyItems.ADHESIVE_POTION.get());
                        output.accept(LyyItems.BLUE_POTION.get());
                        output.accept(LyyItems.SILVER_POTION.get());
                        output.accept(LyyItems.STRANGE_POTION.get());
                        output.accept(LyyItems.CONTROL_CRYSTAL.get());
                        output.accept(LyyItems.CONTROL_ENHANCEMENT_POTION.get());
                        output.accept(LyyBlocks.MIND_CONTROL_BEACON.get());
                        output.accept(LyyItems.GUIDING_REAGENT.get());
                        output.accept(LyyItems.PROOF.get());
                        output.accept(LyyItems.UNEXTINGUISHED_DESIRE.get());
                        output.accept(LyyItems.JUMP_POTION.get());
                        output.accept(LyyItems.PROPULSION_POTION.get());
                        // Advanced materials and production.
                        output.accept(LyyItems.PURE_IMAGINARY_STEEL.get());
                        output.accept(LyyItems.IMAGINARY_STEEL_BLOCK.get());
                        output.accept(LyyItems.ENERGY_CORE.get());
                        output.accept(LyyItems.PURE_SMELTING_PLANT_SHELL.get());
                        output.accept(LyyItems.PURE_SMELTING_PLANT.get());
                        output.accept(LyyItems.FISSION_FURNACE.get());
                        output.accept(LyyItems.PHANTOM_MATRIX.get());
                        output.accept(LyyBlocks.MINIATURE_CRYSTAL_FACTORY.get());
                        for (var kind : org.lyy.lyycore.content.ResourceFrameKind.FACTORY_KINDS) output.accept(kind.factoryBlock());
                        // Rituals and dragon equipment.
                        output.accept(LyyItems.SUMMONING_ALTAR.get());
                        output.accept(LyyItems.SUMMONING_PEDESTAL.get());
                        output.accept(LyyItems.FRIENDLY_PROOF.get());
                        output.accept(LyyItems.ZHUZHU.get());
                        output.accept(LyyItems.ELEPHANT.get());
                        output.accept(LyyItems.GREEN_ONION.get());
                        output.accept(LyyItems.IMAGINARY_DRAGON_NEST.get());
                        output.accept(LyyItems.DRAGON_MIGHT.get());
                        output.accept(LyyItems.AMPLIFICATION_POTION.get());
                        output.accept(LyyItems.ENDLESS_EROSION.get());
                        output.accept(LyyItems.MIRROR.get());
                        output.accept(LyyItems.REFORGING_TABLE.get());
                        output.accept(LyyItems.TIME_KNOCKING_STONE.get());
                        output.accept(LyyItems.SPACETIME_CORE.get());
                        output.accept(LyyItems.SPACETIME_FACTORY.get());
                        output.accept(LyyItems.SPACETIME_MATRIX.get());
                        output.accept(LyyItems.SPACETIME_COORDINATE_DEVICE.get());
                        output.accept(LyyItems.SPACETIME_SHARD.get());
                        output.accept(LyyItems.SPACETIME_CRYSTAL.get());
                        output.accept(LyyItems.SPACETIME_ALLOY.get());
                        output.accept(LyyItems.SPACETIME_ALLOY_BLOCK.get());
                        output.accept(LyyItems.SPACETIME_ALLOY_PLATE.get());
                        output.accept(LyyItems.SPACETIME_BLESSING_NECKLACE.get());
                        output.accept(LyyItems.SPACETIME_BLESSING_NOTEBOOK.get());
                        output.accept(LyyItems.SPACETIME_CHAIN.get());
                        output.accept(LyyItems.SPACETIME_HOE.get());
                        output.accept(LyyItems.SUCHEN.get());
                        output.accept(LyyItems.CELESTIAL_ORDER.get());
                        output.accept(LyyItems.FROZEN_MOMENT.get());
                        var liuxi = new ItemStack(LyyItems.LIUXI.get());
                        liuxi.enchant(params.holders().lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 20);
                        output.accept(liuxi);
                        output.accept(LyyItems.RIFTBREAKER.get());
                        output.accept(LyyItems.SPACETIME_TRIAL_PROOF.get());
                        output.accept(LyyItems.PURE_IMAGINARY_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.PURE_IMAGINARY_STEEL_NOTEBOOK.get());
                        output.accept(LyyItems.FROST_STEEL_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.FROST_STEEL_NOTEBOOK.get());
                        output.accept(LyyItems.INFERNAL_STEEL_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.INFERNAL_STEEL_NOTEBOOK.get());
                        output.accept(LyyItems.CHARGED_STEEL_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.CHARGED_STEEL_NOTEBOOK.get());
                        output.accept(LyyItems.VOID_ALLOY_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.VOID_ALLOY_NOTEBOOK.get());
                        output.accept(LyyItems.UNEXTINGUISHED_ALLOY_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.UNEXTINGUISHED_ALLOY_NOTEBOOK.get());
                        output.accept(LyyItems.STAINED_ALLOY_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.STAINED_ALLOY_NOTEBOOK.get());
                        output.accept(LyyItems.SIXFOLD_ALLOY_CRYSTAL_NECKLACE.get());
                        output.accept(LyyItems.SIXFOLD_ALLOY_NOTEBOOK.get());
                        // Endgame materials.
                        output.accept(LyyItems.HIGH_PURITY_CRYSTAL.get());
                        output.accept(LyyItems.FLAWLESS_CRYSTAL.get());
                        output.accept(LyyItems.VOID_ALLOY.get());
                        output.accept(LyyItems.VOID_ALLOY_BLOCK.get());
                        output.accept(LyyItems.UNEXTINGUISHED_ALLOY.get());
                        output.accept(LyyItems.UNEXTINGUISHED_ALLOY_BLOCK.get());
                        output.accept(LyyItems.STAINED_ALLOY.get());
                        output.accept(LyyItems.STAINED_ALLOY_BLOCK.get());
                        output.accept(LyyItems.FROST_STEEL.get());
                        output.accept(LyyItems.FROST_STEEL_BLOCK.get());
                        output.accept(LyyItems.FROST_ROPE.get());
                        output.accept(LyyItems.FROST_CHAIN.get());
                        output.accept(LyyItems.VOID_FISHING_ROD.get());
                        output.accept(LyyItems.INFERNAL_STEEL.get());
                        output.accept(LyyItems.INFERNAL_STEEL_BLOCK.get());
                        output.accept(LyyItems.CHARGED_STEEL.get());
                        output.accept(LyyItems.CHARGED_STEEL_BLOCK.get());
                        output.accept(LyyItems.SIXFOLD_ALLOY_INGOT.get());
                        output.accept(LyyItems.SIXFOLD_ALLOY_BLOCK.get());
                        output.accept(LyyItems.PURE_IMAGINARY_STEEL_PLATE.get());
                        output.accept(LyyItems.VOID_ALLOY_PLATE.get());
                        output.accept(LyyItems.UNEXTINGUISHED_ALLOY_PLATE.get());
                        output.accept(LyyItems.STAINED_ALLOY_PLATE.get());
                        output.accept(LyyItems.FROST_STEEL_PLATE.get());
                        output.accept(LyyItems.INFERNAL_STEEL_PLATE.get());
                        output.accept(LyyItems.CHARGED_STEEL_PLATE.get());
                        output.accept(LyyItems.SIXFOLD_ALLOY_PLATE.get());
                        // Endgame production, research and logistics.
                        output.accept(LyyItems.EXPERIMENT_TABLE.get());
                        output.accept(LyyItems.EROSION_CORE.get());
                        output.accept(LyyItems.EROSION_FACTORY.get());
                        output.accept(LyyItems.CAPTURE_CAGE.get());
                        output.accept(LyyItems.CRYSTAL_PRISM.get());
                        output.accept(LyyItems.SUMMONING_BEACON.get());
                        output.accept(LyyItems.OTHERWORLD_CHEST.get());
                        output.accept(LyyItems.MATRIX_CASING.get());
                        output.accept(LyyItems.CHANNEL_CORE.get());
                        output.accept(LyyItems.LOGISTICS_MATRIX.get());
                        output.accept(LyyItems.MATRIX_INPUT.get());
                        output.accept(LyyItems.MATRIX_OUTPUT.get());
                        output.accept(LyyItems.MATRIX_COLLECTOR.get());
                        // Special weapons and weather.
                        output.accept(LyyItems.WHISPER_OF_THE_PAST.get());
                        output.accept(LyyItems.HAIL.get());
                        output.accept(LyyItems.LIGHTNING_CORE.get());
                        output.accept(LyyItems.THUNDER_SCYTHE.get());
                        output.accept(LyyItems.FLAME_CORE.get());
                        output.accept(LyyItems.EMBERFALL.get());
                        output.accept(LyyItems.LIGHTNING_BOTTLE.get());
                        output.accept(LyyItems.STORM_BALL.get());
                        output.accept(LyyItems.SUN_BALL.get());
                    })
                    .build());
}
