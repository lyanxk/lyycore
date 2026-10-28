package org.lyy.lyycore.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lyy.lyycore.content.SpacetimeChains;

import java.util.List;

@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeChainItem extends Item {
    public SpacetimeChainItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && !player.isSpectator()) SpacetimeChains.pull(server);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.isSecondaryUseActive() ? useOn(context) : InteractionResult.PASS;
    }

    private static void interact(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!(player.getItemInHand(event.getHand()).getItem() instanceof SpacetimeChainItem) || player.isSpectator()) return;
        if (player instanceof ServerPlayer server) {
            if (event.getTarget() instanceof LivingEntity living) SpacetimeChains.toggle(server, living);
            else SpacetimeChains.pull(server);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entity(PlayerInteractEvent.EntityInteract event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        // Specific interaction runs first. Consume it so the generic packet cannot toggle twice.
        var player = event.getEntity();
        if (!(player.getItemInHand(event.getHand()).getItem() instanceof SpacetimeChainItem) || player.isSpectator()) return;
        if (player instanceof ServerPlayer server) {
            if (event.getTarget() instanceof LivingEntity living) SpacetimeChains.toggle(server, living);
            else SpacetimeChains.pull(server);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.spacetime_chain").withColor(0x9CD9F3));
    }
}
