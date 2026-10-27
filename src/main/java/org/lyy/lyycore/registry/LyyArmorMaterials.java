package org.lyy.lyycore.registry;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LyyArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "lyycore");
    // Diamond enchanting value, no defense, toughness or visible armor texture layers.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SPACETIME = MATERIALS.register("spacetime",
            () -> new ArmorMaterial(Map.of(), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.EMPTY, List.of(), 0, 0));
    private LyyArmorMaterials() { }
}
