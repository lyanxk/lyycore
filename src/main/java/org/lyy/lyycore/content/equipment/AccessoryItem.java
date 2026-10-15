package org.lyy.lyycore.content.equipment;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.lyy.lyycore.content.PlayerAttributes.Stat;
import org.lyy.lyycore.registry.LyyItems;

public final class AccessoryItem extends Item {
    public enum Kind { NECKLACE, NOTEBOOK }
    public record Bonus(Stat stat, double amount) { }
    public enum Series {
        STEEL(() -> LyyItems.PURE_IMAGINARY_STEEL_PLATE.get(), new Bonus(Stat.ATTACK, 10), new Bonus(Stat.PHYSICAL, .2)),
        FROST(() -> LyyItems.FROST_STEEL_PLATE.get(), new Bonus(Stat.ATTACK, 10), new Bonus(Stat.ICE, .2)),
        INFERNAL(() -> LyyItems.INFERNAL_STEEL_PLATE.get(), new Bonus(Stat.ATTACK, 10), new Bonus(Stat.FIRE, .2)),
        CHARGED(() -> LyyItems.CHARGED_STEEL_PLATE.get(), new Bonus(Stat.ATTACK, 10), new Bonus(Stat.LIGHTNING, .2)),
        VOID(() -> LyyItems.VOID_ALLOY_PLATE.get(), new Bonus(Stat.TOUGHNESS, 6)),
        UNEXTINGUISHED(() -> LyyItems.UNEXTINGUISHED_ALLOY_PLATE.get(), new Bonus(Stat.ARMOR, 20)),
        STAINED(() -> LyyItems.STAINED_ALLOY_PLATE.get(), new Bonus(Stat.LIFE, 20)),
        SIXFOLD(() -> LyyItems.SIXFOLD_ALLOY_PLATE.get(), new Bonus(Stat.DAMAGE, .1));
        public final Supplier<Item> plate;
        public final List<Bonus> bonuses;
        Series(Supplier<Item> plate, Bonus... bonuses) { this.plate = plate; this.bonuses = List.of(bonuses); }
    }
    public final Kind kind;
    public final Series series;
    public AccessoryItem(Properties properties, Kind kind, Series series) {
        super(properties.stacksTo(1).rarity(Rarity.RARE)); this.kind = kind; this.series = series;
    }
    public static Component statName(Stat stat) { return Component.translatable(stat.attribute.value().getDescriptionId()); }
    public static String amount(Stat stat, double value) {
        return String.format(java.util.Locale.ROOT, stat.ordinal() < 4 ? "+%.0f" : "+%.0f%%", stat.ordinal() < 4 ? value : value * 100);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        for (var bonus : series.bonuses)
            lines.add(Component.translatable("tooltip.lyycore.accessory.bonus", statName(bonus.stat), amount(bonus.stat, bonus.amount)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.lyycore.accessory.set").withStyle(ChatFormatting.LIGHT_PURPLE));
        var data = AccessoryData.read(stack);
        if (data.traits().isEmpty()) lines.add(Component.translatable("tooltip.lyycore.accessory.unrolled").withStyle(ChatFormatting.GRAY));
        else {
            lines.add(Component.translatable("tooltip.lyycore.accessory.points", data.remaining(), data.total()).withStyle(ChatFormatting.GOLD));
            for (var stat : data.traits()) lines.add(Component.translatable("tooltip.lyycore.accessory.trait", statName(stat), data.allocated(stat)).withStyle(ChatFormatting.GRAY));
        }
    }
}
