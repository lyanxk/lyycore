package org.lyy.lyycore.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.lyy.lyycore.content.fishing.VoidFishing;

import java.util.List;

public final class VoidFishingRodItem extends FishingRodItem {
    public VoidFishingRodItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator()) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean casting = VoidFishing.toggle(serverPlayer, hand);
            level.playSound(null, player.blockPosition(), casting ? SoundEvents.FISHING_BOBBER_THROW
                    : SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.5F, 1);
            player.gameEvent(casting ? GameEvent.ITEM_INTERACT_START : GameEvent.ITEM_INTERACT_FINISH);
            if (casting) player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public boolean isEnchantable(ItemStack stack) { return stack.getCount() == 1; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.void_fishing_rod.cast"));
        lines.add(Component.translatable("tooltip.lyycore.void_fishing_rod.stop"));
        lines.add(Component.translatable("tooltip.lyycore.void_fishing_rod.durability"));
    }
}
