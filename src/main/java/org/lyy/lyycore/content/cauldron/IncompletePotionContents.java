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
import org.lyy.lyycore.registry.LyyItems;

/** Stored materials use the existing component format, so previously bottled mixtures still work. */
public final class IncompletePotionContents {
    private static final String KEY = "CauldronIngredients";

    public static List<ItemStack> read(ItemStack potion) {
        var materials = new ArrayList<ItemStack>();
        var data = potion.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (var raw : data.getList(KEY, Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) raw;
            var id = ResourceLocation.tryParse(entry.getString("Item"));
            int count = entry.getInt("Count");
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || count <= 0) continue;
            var material = new ItemStack(BuiltInRegistries.ITEM.get(id), count);
            // Old empty bottles could accidentally record themselves as an ingredient.
            if (!material.isEmpty() && !material.is(LyyItems.INCOMPLETE_POTION)) materials.add(material);
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
