package org.lyy.lyycore.content.equipment;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.registry.LyyArmorMaterials;

public final class SpacetimeArmorItem extends ArmorItem {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public SpacetimeArmorItem(Type type, Properties properties) {
        super(LyyArmorMaterials.SPACETIME, type, properties.stacksTo(1));
    }

    @Override public boolean isEnchantable(ItemStack stack) { return true; }

    public static int equippedPieces(LivingEntity wearer) {
        int pieces = 0;
        for (var slot : SLOTS) {
            if (wearer.getItemBySlot(slot).getItem() instanceof SpacetimeArmorItem armor && armor.getEquipmentSlot() == slot) pieces++;
        }
        return pieces;
    }
}
