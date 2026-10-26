package org.lyy.lyycore.content.equipment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.lyy.lyycore.content.PlayerAttributes.Stat;

/** Rolls and allocations travel with the item, including after unequipping and reforging. */
public record AccessoryData(List<Stat> traits, int[] points, int total) {
    private static final String KEY = "LyyAccessory";
    public int allocated(Stat stat) { return points[stat.ordinal()]; }
    public int remaining() { return total() - java.util.Arrays.stream(points).sum(); }
    public static AccessoryData read(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
        var traits = tag.getList("Traits", 8).stream().map(entry -> Stat.valueOf(entry.getAsString())).toList();
        int[] points = new int[Stat.values().length];
        var allocations = tag.getCompound("Points");
        for (var stat : traits) points[stat.ordinal()] = allocations.getInt(stat.name());
        return new AccessoryData(traits, points, pointBudget(stack, traits.size()));
    }
    private static int pointBudget(ItemStack stack, int traitCount) {
        return stack.getItem() instanceof Accessory accessory ? accessory.attributePoints(traitCount) : 0;
    }
    private void save(ItemStack stack) {
        var tag = new CompoundTag(); var names = new ListTag(); var allocations = new CompoundTag();
        for (var stat : traits) { names.add(StringTag.valueOf(stat.name())); allocations.putInt(stat.name(), allocated(stat)); }
        tag.put("Traits", names); tag.put("Points", allocations);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put(KEY, tag));
    }
    public static void initialize(ItemStack stack, RandomSource random) {
        if (!(stack.getItem() instanceof Accessory accessory)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(KEY)) return;
        roll(stack, List.of(), accessory.rollTraitCount(random), random).save(stack);
    }
    private static AccessoryData roll(ItemStack stack, List<Stat> locked, int count, RandomSource random) {
        var traits = new ArrayList<>(locked);
        var available = new ArrayList<>(List.of(Stat.values())); available.removeAll(locked);
        while (traits.size() < count) traits.add(available.remove(random.nextInt(available.size())));
        return new AccessoryData(List.copyOf(traits), new int[Stat.values().length], pointBudget(stack, count));
    }
    public static void reforge(ItemStack stack, int locks, RandomSource random) {
        var old = read(stack); var kept = new ArrayList<Stat>();
        for (int i = 0; i < old.traits.size(); i++) if ((locks & 1 << i) != 0) kept.add(old.traits.get(i));
        roll(stack, kept, old.traits.size(), random).save(stack);
    }
    public static boolean allocate(ItemStack stack, Stat stat, boolean add, int amount) {
        var data = read(stack);
        if (!data.traits.contains(stat)) return false;
        int change = Math.min(amount, add ? data.remaining() : data.allocated(stat));
        if (change <= 0) return false;
        data.points[stat.ordinal()] += add ? change : -change;
        data.save(stack); return true;
    }
}
