package org.lyy.lyycore.content.finality;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;

import org.lyy.lyycore.content.PlayerAttributes.Stat;
import org.lyy.lyycore.content.equipment.*;

import java.util.List;

public final class FinalityAccessoryItem extends Item implements Accessory {
    private final Kind kind;

    public FinalityAccessoryItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
        this.kind = kind;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public int rollTraitCount(RandomSource random) {
        return Stat.values().length;
    }

    @Override
    public int attributePoints(int traits) {
        return 100;
    }

    @Override
    public List<Bonus> bonuses() {
        return List.of();
    }

    @Override
    public ResourceLocation setId() {
        return FinalityContent.id("finality_blessing");
    }

    @Override
    public Item reforgeMaterial() {
        return Items.AIR;
    }

    @Override
    public boolean canReforge() {
        return false;
    }

    @Override
    public List<Bonus> setBonuses() {
        return List.of(
                new Bonus(Stat.DAMAGE, 4),
                new Bonus(Stat.PHYSICAL, 4),
                new Bonus(Stat.ICE, 4),
                new Bonus(Stat.FIRE, 4),
                new Bonus(Stat.LIGHTNING, 4));
    }

    @Override
    public double setReachBonus() {
        return 10;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        AccessoryItem.appendTraitTooltip(
                stack, lines, Component.translatable("tooltip.lyycore.finality.origin"));
    }
}
