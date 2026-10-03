package org.lyy.lyycore.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.item.ImaginaryDisassemblerItem;
import org.lyy.lyycore.content.item.SonnetBowItem;
import java.util.EnumMap;
import java.util.Map;

public class LyyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LyyCore.MODID);
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

    // Custom items
    public static final DeferredItem<SonnetBowItem> WHISPER_OF_THE_PAST =
            ITEMS.registerItem("whisper_of_the_past", SonnetBowItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> IMAGINARY_DISASSEMBLER =
            ITEMS.registerItem("imaginary_disassembler", ImaginaryDisassemblerItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());

    public static final DeferredItem<Item> PURE_CRYSTAL = ITEMS.registerSimpleItem("pure_crystal", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem(LyyBlocks.CRYSTAL_BLOCK);
    public static final DeferredItem<BlockItem> ALLOY_BLOCK = ITEMS.registerSimpleBlockItem(LyyBlocks.ALLOY_BLOCK);
    public static final DeferredItem<BlockItem> IMAGINARY_GATE = ITEMS.registerSimpleBlockItem(LyyBlocks.IMAGINARY_GATE);
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
        // Resolve old item stacks and datapack references to the renamed item.
        // Saving those stacks again writes the canonical imaginary_crystal ID.
        ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "raw_imaginium"),
                IMAGINARY_CRYSTAL.getId());
    }
}
