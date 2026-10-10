package org.lyy.lyycore.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.item.*;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.item.ImaginaryDisassemblerItem;
import org.lyy.lyycore.content.item.ImaginaryReaperItem;
import org.lyy.lyycore.content.item.WeatherBallItem;
import org.lyy.lyycore.content.item.ImaginaryGrappleItem;
import org.lyy.lyycore.content.item.SonnetBowItem;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import java.util.EnumMap;
import java.util.Map;

public class LyyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LyyCore.MODID);
    public static final DeferredItem<BlockItem> PURE_SMELTING_PLANT_SHELL = ITEMS.registerSimpleBlockItem(LyyBlocks.PURE_SMELTING_PLANT_SHELL);
    public static final DeferredItem<BlockItem> PURE_SMELTING_PLANT = ITEMS.registerSimpleBlockItem(LyyBlocks.PURE_SMELTING_PLANT);
    public static final DeferredItem<BlockItem> IMAGINARY_DRAGON_NEST = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_DRAGON_NEST);
    public static final DeferredItem<BlockItem> IMAGINARY_STEEL_BLOCK = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_STEEL_BLOCK);
    public static final DeferredItem<Item> PURE_IMAGINARY_STEEL = ITEMS.registerSimpleItem("pure_imaginary_steel");
    public static final DeferredItem<BlockItem> ALLOY_CAULDRON = ITEMS.registerSimpleBlockItem(LyyBlocks.ALLOY_CAULDRON);
    public static final DeferredItem<BlockItem> MIND_CONTROL_BEACON = ITEMS.registerSimpleBlockItem(LyyBlocks.MIND_CONTROL_BEACON);
    public static final DeferredItem<Item> CONTROL_CRYSTAL = ITEMS.registerSimpleItem("control_crystal", new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<ExperimentPotionItem> BLUE_POTION = potion("blue_potion", ExperimentPotionItem.Kind.BLUE);
    public static final DeferredItem<ExperimentPotionItem> SILVER_POTION = potion("silver_potion", ExperimentPotionItem.Kind.SILVER);
    public static final DeferredItem<ExperimentPotionItem> STRANGE_POTION = potion("strange_potion", ExperimentPotionItem.Kind.STRANGE);
    public static final DeferredItem<ExperimentPotionItem> INCOMPLETE_POTION = potion("incomplete_potion", ExperimentPotionItem.Kind.INCOMPLETE);
    public static final DeferredItem<ExperimentPotionItem> CONTROL_ENHANCEMENT_POTION = potion("control_enhancement_potion", ExperimentPotionItem.Kind.ENHANCEMENT);
    public static final DeferredItem<ExperimentPotionItem> ADHESIVE_POTION = potion("adhesive_potion", ExperimentPotionItem.Kind.ADHESIVE);
    public static final DeferredItem<Item> GUIDING_REAGENT = ITEMS.registerSimpleItem("guiding_reagent", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PROOF = ITEMS.registerSimpleItem("proof", new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> FRIENDLY_PROOF = ITEMS.registerSimpleItem("friendly_proof");
    public static final DeferredItem<ExperienceFoodItem> UNEXTINGUISHED_DESIRE = ITEMS.registerItem("unextinguished_desire",
            properties -> new ExperienceFoodItem(properties, 1000), new Item.Properties().rarity(Rarity.EPIC));
    private static DeferredItem<ExperimentPotionItem> potion(String name, ExperimentPotionItem.Kind kind) {
        return ITEMS.registerItem(name, properties -> new ExperimentPotionItem(properties, kind), new Item.Properties());
    }
    public static final DeferredItem<BlockItem> IMAGINARY_CONDENSING_BEACON = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_CONDENSING_BEACON);
    public static final DeferredItem<BlockItem> SPATIAL_TRANSMISSION_TOWER = ITEMS.registerSimpleBlockItem(LyyBlocks.SPATIAL_TRANSMISSION_TOWER);
    public static final DeferredItem<CoordinateDeviceItem> COORDINATE_DEVICE = ITEMS.registerItem("coordinate_device", CoordinateDeviceItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<SupermutationFactorItem> SUPERMUTATION_FACTOR = ITEMS.registerItem("supermutation_factor", SupermutationFactorItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> PRODUCTION_LAB = ITEMS.registerSimpleBlockItem(LyyBlocks.PRODUCTION_LAB);
    public static final DeferredItem<ResearchNotesItem> RESEARCH_NOTES = ITEMS.registerItem("research_notes", ResearchNotesItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> IMAGINARY_RESEARCH_TABLE = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_RESEARCH_TABLE);
    public static final Map<ResourceFrameKind, DeferredItem<Item>> RESOURCE_CORES = new EnumMap<>(ResourceFrameKind.class);
    public static final Map<ResourceFrameKind, DeferredItem<BlockItem>> RESOURCE_FACTORY_ITEMS = new EnumMap<>(ResourceFrameKind.class);
    public static final Map<ResourceFrameKind, DeferredItem<BlockItem>> RESOURCE_FRAME_ITEMS = new EnumMap<>(ResourceFrameKind.class);
    static {
        for (var kind : ResourceFrameKind.values()) {
            RESOURCE_CORES.put(kind, ITEMS.registerSimpleItem(kind.coreId()));
            RESOURCE_FRAME_ITEMS.put(kind, ITEMS.registerSimpleBlockItem(LyyBlocks.RESOURCE_FRAMES.get(kind)));
        }
    }

    static {
        for (var kind : ResourceFrameKind.FACTORY_KINDS)
            RESOURCE_FACTORY_ITEMS.put(kind, ITEMS.registerSimpleBlockItem(LyyBlocks.RESOURCE_FACTORIES.get(kind)));
    }
    public static final DeferredItem<BlockItem> MINIATURE_CRYSTAL_FACTORY = ITEMS.registerSimpleBlockItem(LyyBlocks.MINIATURE_CRYSTAL_FACTORY);

    public static final DeferredItem<ImaginaryGrappleItem> IMAGINARY_GRAPPLE =
            ITEMS.registerItem("imaginary_grapple", ImaginaryGrappleItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredItem<Item> ENERGY_CORE = ITEMS.registerSimpleItem("energy_core");
    public static final DeferredItem<DragonMightItem> DRAGON_MIGHT = ITEMS.registerItem("dragon_might", DragonMightItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> AMPLIFICATION_POTION = ITEMS.registerSimpleItem("amplification_potion", new Item.Properties().stacksTo(1));
    public static final DeferredItem<ExperienceFoodItem> ENDLESS_EROSION = ITEMS.registerItem("endless_erosion",
            properties -> new ExperienceFoodItem(properties, 3000), new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> JUMP_POTION = ITEMS.registerSimpleItem("jump_potion", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PROPULSION_POTION = ITEMS.registerSimpleItem("propulsion_potion", new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> PHANTOM_MATRIX = ITEMS.registerSimpleBlockItem(LyyBlocks.PHANTOM_MATRIX);
    public static final DeferredItem<BlockItem> FISSION_FURNACE = ITEMS.registerSimpleBlockItem(LyyBlocks.FISSION_FURNACE);
    public static final DeferredItem<BlockItem> SUMMONING_ALTAR = ITEMS.registerSimpleBlockItem(LyyBlocks.SUMMONING_ALTAR);
    public static final DeferredItem<BlockItem> SUMMONING_PEDESTAL = ITEMS.registerSimpleBlockItem(LyyBlocks.SUMMONING_PEDESTAL);
    // Custom items
    public static final DeferredItem<SonnetBowItem> WHISPER_OF_THE_PAST =
            ITEMS.registerItem("whisper_of_the_past", SonnetBowItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> IMAGINARY_DISASSEMBLER =
            ITEMS.registerItem("imaginary_disassembler", ImaginaryDisassemblerItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());

    public static final DeferredItem<ImaginaryReaperItem> IMAGINARY_REAPER =
            ITEMS.registerItem("imaginary_reaper", ImaginaryReaperItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Item> LIGHTNING_BOTTLE = ITEMS.registerSimpleItem("lightning_bottle");
    public static final DeferredItem<Item> SMALL_IMAGINARY_CORE = ITEMS.registerSimpleItem("small_imaginary_core");
    public static final DeferredItem<ExperienceFoodItem> HEART_OF_NOTHINGNESS = ITEMS.registerItem("heart_of_nothingness",
            properties -> new ExperienceFoodItem(properties, 500));
    public static final DeferredItem<WeatherBallItem> STORM_BALL = ITEMS.registerItem("storm_ball",
            properties -> new WeatherBallItem(properties, true), new Item.Properties().stacksTo(16));
    public static final DeferredItem<WeatherBallItem> SUN_BALL = ITEMS.registerItem("sun_ball",
            properties -> new WeatherBallItem(properties, false), new Item.Properties().stacksTo(16));

    public static final DeferredItem<ExperienceFoodItem> PURE_CRYSTAL = ITEMS.registerItem("pure_crystal",
            properties -> new ExperienceFoodItem(properties, 300), new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem(LyyBlocks.CRYSTAL_BLOCK);
    public static final DeferredItem<BlockItem> ALLOY_BLOCK = ITEMS.registerSimpleBlockItem(LyyBlocks.ALLOY_BLOCK);
    public static final DeferredItem<BlockItem> IMAGINARY_GATE = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_GATE);
    public static final DeferredItem<BlockItem> ADVANCED_IMAGINARY_GATE = ITEMS.registerSimpleBlockItem(LyyBlocks.ADVANCED_IMAGINARY_GATE);
    public static final DeferredItem<BlockItem> ENDER_SENTRY = ITEMS.registerSimpleBlockItem(LyyBlocks.ENDER_SENTRY);
    public static final DeferredItem<BlockItem> IMAGINARY_CRAFTING_TABLE = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_CRAFTING_TABLE);

    // Simple items
    public static final DeferredItem<Item> IMAGINARY_CRYSTAL = ITEMS.registerSimpleItem("imaginary_crystal");
    public static final DeferredItem<Item> IMAGINARY_ALLOY_INGOT = ITEMS.registerSimpleItem("imaginary_alloy_ingot");
    public static final DeferredItem<Item> IM_BATTERY = ITEMS.registerSimpleItem("im_battery");

    // Block items
    public static final DeferredItem<BlockItem> CRYSTAL_CONDENSING_FRAME_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.CRYSTAL_CONDENSING_FRAME);
    public static final DeferredItem<BlockItem> IMAGINARY_ENERGY_CELL_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_ENERGY_CELL);
    public static final DeferredItem<BlockItem> IMAGINIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_IMAGINIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.DEEPSLATE_IMAGINIUM_ORE);
    public static final DeferredItem<BlockItem> ITEM_COLLECTOR_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.ITEM_COLLECTOR);
    public static final DeferredItem<BlockItem> IAF_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.IAF);
    public static final DeferredItem<BlockItem> IGB_ITEM =
            ITEMS.registerSimpleBlockItem(LyyBlocks.IGB);

    static {
        // The altar material is the existing summoning crystal; migrate the former duplicate.
        ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "summoning_core"),
                CONTROL_CRYSTAL.getId());
        // Resolve old item stacks and datapack references to the renamed item.
        // Saving those stacks again writes the canonical imaginary_crystal ID.
        ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "raw_imaginium"),
                IMAGINARY_CRYSTAL.getId());
    }
}
