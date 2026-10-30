package org.lyy.lyycore.content.finality;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import org.lyy.lyycore.registry.LyyArmorMaterials;

public final class FinalityArmorItem extends ArmorItem {
    public static final int HEAD_EXHAUSTION = 0, HEAD_HEALING = 1, HEAD_DEBUFF_DECAY = 2, HEAD_NIGHT_VISION = 3;
    public static final int CHEST_CANCEL_DAMAGE = 0, CHEST_FLIGHT = 1, CHEST_PREVENT_TELEPORT = 2;

    public FinalityArmorItem(Type type, Properties properties) {
        super(LyyArmorMaterials.SPACETIME, type, properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    public static boolean enabled(ItemStack stack, int option) {
        return !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBoolean("FinalityDisabled" + option);
    }

    public static int optionCount(ItemStack stack) {
        if (!(stack.getItem() instanceof FinalityArmorItem armor)) return 0;
        return switch (armor.getEquipmentSlot()) {
            case HEAD -> 4;
            case CHEST -> 3;
            case LEGS -> 1;
            default -> 2;
        };
    }

    public static void toggle(ItemStack stack, int option) {
        boolean enabled = enabled(stack, option);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putBoolean("FinalityDisabled" + option, enabled));
    }

    public static double speed(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("FinalitySpeed") ? Math.clamp(tag.getDouble("FinalitySpeed"), 1, 4) : 2;
    }

    public static void speed(ItemStack stack, double value) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putDouble("FinalitySpeed", Math.clamp(value, 1, 4)));
    }

    public static boolean active(LivingEntity entity, EquipmentSlot slot, int option) {
        var stack = entity.getItemBySlot(slot);
        return stack.getItem() instanceof FinalityArmorItem item
                && item.getEquipmentSlot() == slot
                && enabled(stack, option);
    }

    public static int pieces(LivingEntity entity) {
        int count = 0;
        for (var slot :
                new EquipmentSlot[] {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
                })
            if (entity.getItemBySlot(slot).getItem() instanceof FinalityArmorItem armor
                    && armor.getEquipmentSlot() == slot) count++;
        return count;
    }
}
