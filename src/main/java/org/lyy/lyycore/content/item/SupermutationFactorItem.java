package org.lyy.lyycore.content.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.network.SkillNetwork;

public final class SupermutationFactorItem extends Item {
    public SupermutationFactorItem(Properties properties) { super(properties); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public SoundEvent getDrinkingSound() { return SoundEvents.GENERIC_DRINK; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SkillSystem.unlockFromFactor(player);
            SkillNetwork.sync(player);
            stack.consume(1, player);
        }
        return stack;
    }
}
