package org.lyy.lyycore.content.cauldron;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Materials bottled from an unfinished cauldron mixture. */
public final class IncompletePotionContents {
    private static final String KEY = "CauldronIngredients";

    public static List<ItemStack> read(ItemStack potion) {
        var materials = new ArrayList<ItemStack>();
        var data = potion.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (var raw : data.getList(KEY, Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) raw;
            materials.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Item"))), entry.getInt("Count")));
        }
        return materials;
    }

    public static void write(ItemStack potion, Map<Item, Integer> materials) {
        var contents = new ListTag();
        materials.forEach((item, count) -> {
            var entry = new CompoundTag();
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(item).toString());
            entry.putInt("Count", count);
            contents.add(entry);
        });
        CustomData.update(DataComponents.CUSTOM_DATA, potion, tag -> tag.put(KEY, contents));
    }

    private IncompletePotionContents() { }
}
