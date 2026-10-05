package org.lyy.lyycore.content.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.registry.LyyEffects;
import org.lyy.lyycore.content.cauldron.IncompletePotionContents;

public final class ExperimentPotionItem extends Item {
    public enum Kind { BLUE, SILVER, STRANGE, INCOMPLETE, ENHANCEMENT, ADHESIVE }
    private final Kind kind;
    public ExperimentPotionItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1).component(DataComponents.POTION_CONTENTS, new PotionContents(java.util.Optional.empty(), java.util.Optional.empty(), initialEffects(kind))));
        this.kind = kind;
    }
    private static List<MobEffectInstance> initialEffects(Kind kind) {
        return switch (kind) {
            case ENHANCEMENT -> List.of(new MobEffectInstance(LyyEffects.FLIGHT_SPEED, 12000), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000),
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 4), new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 2));
            case INCOMPLETE -> List.of(new MobEffectInstance(MobEffects.GLOWING, 600));
            default -> List.of();
        };
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            for (var effect : stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects()) {
                if (effect.getEffect().value().isInstantenous()) effect.getEffect().value().applyInstantenousEffect(null, null, entity, effect.getAmplifier(), 1);
                else entity.addEffect(new MobEffectInstance(effect));
            }
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        return new ItemStack(Items.GLASS_BOTTLE);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (kind == Kind.INCOMPLETE) {
            tooltip.add(Component.translatable("tooltip.lyycore.incomplete_potion"));
            for (var material : IncompletePotionContents.read(stack)) {
                tooltip.add(Component.literal("  ").append(material.getHoverName()).append(" ×" + material.getCount())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).addPotionTooltip(tooltip::add, 1, context.tickRate());
    }
}
