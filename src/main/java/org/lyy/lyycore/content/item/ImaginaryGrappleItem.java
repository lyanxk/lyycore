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
import org.lyy.lyycore.content.entity.GrappleHook;
import org.lyy.lyycore.registry.LyyEntities;

import java.util.List;

public final class ImaginaryGrappleItem extends Item {
    public ImaginaryGrappleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            if (GrappleHook.active(serverPlayer) != null) return InteractionResultHolder.fail(stack);
            GrappleHook hook = LyyEntities.GRAPPLE_HOOK.get().create(level);
            if (hook == null) return InteractionResultHolder.fail(stack);
            hook.launch(serverPlayer);
            if (!level.addFreshEntity(hook)) return InteractionResultHolder.fail(stack);
            player.getPersistentData().putUUID(GrappleHook.ACTIVE_TAG, hook.getUUID());
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.grapple.use"));
        lines.add(Component.translatable("tooltip.lyycore.grapple.steer"));
        lines.add(Component.translatable("tooltip.lyycore.grapple.retract"));
        lines.add(Component.translatable("tooltip.lyycore.grapple.entities"));
    }
}
