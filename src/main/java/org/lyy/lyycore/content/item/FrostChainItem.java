package org.lyy.lyycore.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.entity.FrostChainHook;
import org.lyy.lyycore.content.entity.GrappleHook;
import org.lyy.lyycore.registry.LyyEntities;

import java.util.List;

public final class FrostChainItem extends Item {
    public FrostChainItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            var active = FrostChainHook.active(serverPlayer);
            if (active != null) {
                active.discard();
                return InteractionResultHolder.success(stack);
            }
            if (player.isSpectator() || player.isPassenger()) return InteractionResultHolder.fail(stack);
            var hook = LyyEntities.FROST_CHAIN_HOOK.get().create(level);
            if (hook == null) return InteractionResultHolder.fail(stack);
            hook.launch(serverPlayer);
            if (!level.addFreshEntity(hook)) return InteractionResultHolder.fail(stack);
            // Only one hook may drive the player's movement at a time.
            var oldHook = GrappleHook.active(serverPlayer);
            if (oldHook != null) oldHook.discard();
            player.getPersistentData().putUUID(FrostChainHook.ACTIVE_TAG, hook.getUUID());
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.frost_chain.use"));
        lines.add(Component.translatable("tooltip.lyycore.frost_chain.pull"));
    }
}
