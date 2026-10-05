package org.lyy.lyycore.content.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ExperienceFoodItem extends Item {
    private final int experience;

    public ExperienceFoodItem(Properties properties, int experience) {
        super(properties.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build()));
        this.experience = experience;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) player.giveExperiencePoints(experience);
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.experience_food", experience).withStyle(ChatFormatting.GRAY));
    }
}
