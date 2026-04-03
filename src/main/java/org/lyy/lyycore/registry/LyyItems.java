package org.lyy.lyycore.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.item.ImaginaryDisassemblerItem;

public class LyyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LyyCore.MODID);

    // Custom items
    public static final DeferredItem<Item> IMAGINARY_DISASSEMBLER =
            ITEMS.registerItem("imaginary_disassembler", ImaginaryDisassemblerItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());

    // Simple items
    public static final DeferredItem<Item> RAW_IMAGINIUM = ITEMS.registerSimpleItem("raw_imaginium");
    public static final DeferredItem<Item> IMAGINARY_ALLOY_INGOT = ITEMS.registerSimpleItem("imaginary_alloy_ingot");
    public static final DeferredItem<Item> IM_BATTERY = ITEMS.registerSimpleItem("im_battery");

    // Block items
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
}
