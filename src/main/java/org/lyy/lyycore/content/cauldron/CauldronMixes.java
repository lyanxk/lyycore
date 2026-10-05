package org.lyy.lyycore.content.cauldron;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.BushBlock;
import org.lyy.lyycore.registry.LyyItems;

/** Mixing rules shared by the pot and recipe display. Counts are minimums, not exact matches. */
public final class CauldronMixes {
    public record Special(List<ItemStack> ingredients, Item result) {
        public boolean matches(Map<Item, Integer> items) {
            return ingredients.stream().allMatch(stack -> items.getOrDefault(stack.getItem(), 0) >= stack.getCount());
        }
    }
    public static List<Special> specials() {
        return List.of(new Special(List.of(new ItemStack(LyyItems.HEART_OF_NOTHINGNESS.get()), new ItemStack(Items.PHANTOM_MEMBRANE, 2),
                        new ItemStack(Items.DRAGON_EGG)), LyyItems.CONTROL_ENHANCEMENT_POTION.get()),
                new Special(List.of(new ItemStack(Items.SLIME_BLOCK, 64), new ItemStack(Items.LAVA_BUCKET)), LyyItems.ADHESIVE_POTION.get()),
                new Special(List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(Items.MAGMA_CREAM), new ItemStack(Items.SPIDER_EYE),
                        new ItemStack(Items.ROTTEN_FLESH), new ItemStack(LyyItems.HEART_OF_NOTHINGNESS.get()), new ItemStack(Items.DRAGON_EGG),
                        new ItemStack(Items.NETHER_STAR), new ItemStack(LyyItems.CRYSTAL_BLOCK.get())), LyyItems.GUIDING_REAGENT.get()));
    }
    public static boolean specialIngredient(Item item) {
        return specials().stream().flatMap(recipe -> recipe.ingredients.stream()).anyMatch(stack -> stack.is(item));
    }
    public static boolean plant(Item item) {
        var stack = new ItemStack(item);
        return stack.is(ItemTags.FLOWERS) || stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.LEAVES)
                || item instanceof BlockItem block && block.getBlock() instanceof BushBlock
                || item == Items.KELP || item == Items.SEAGRASS || item == Items.VINE || item == Items.WHEAT
                || item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS;
    }
    public static Holder<MobEffect> brewingEffect(Item item) {
        if (item == Items.SUGAR) return MobEffects.MOVEMENT_SPEED;
        if (item == Items.RABBIT_FOOT) return MobEffects.JUMP;
        if (item == Items.BLAZE_POWDER) return MobEffects.DAMAGE_BOOST;
        if (item == Items.GHAST_TEAR) return MobEffects.REGENERATION;
        if (item == Items.GLISTERING_MELON_SLICE) return MobEffects.HEAL;
        if (item == Items.MAGMA_CREAM) return MobEffects.FIRE_RESISTANCE;
        if (item == Items.PUFFERFISH) return MobEffects.WATER_BREATHING;
        if (item == Items.GOLDEN_CARROT) return MobEffects.NIGHT_VISION;
        if (item == Items.PHANTOM_MEMBRANE) return MobEffects.SLOW_FALLING;
        if (item == Items.TURTLE_HELMET) return MobEffects.DAMAGE_RESISTANCE;
        return null;
    }
    public static boolean brewingModifier(Item item) {
        return item == Items.GUNPOWDER || item == Items.GLOWSTONE_DUST || item == Items.REDSTONE
                || item == Items.NETHER_WART || item == Items.DRAGON_BREATH || item == Items.FERMENTED_SPIDER_EYE || item == Items.SPIDER_EYE;
    }
    private CauldronMixes() { }
}
