package org.lyy.lyycore.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import org.lyy.lyycore.registry.LyyItems;

@EventBusSubscriber(modid = "lyycore")
public final class LightningCollection {
    private LightningCollection() { }

    @SubscribeEvent
    public static void transformCrystal(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item) || item.level().isClientSide) return;
        if (item.getItem().is(LyyItems.SPACETIME_CRYSTAL.get())) {
            item.setItem(new ItemStack(LyyItems.SPACETIME_SHARD.get(), item.getItem().getCount()));
            item.clearFire();
            event.setCanceled(true);
        } else if (item.getItem().is(LyyItems.SPACETIME_SHARD.get())) {
            // A lightning bolt can strike again after the crystal has already transformed.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collect(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(Items.GLASS_BOTTLE) || !event.getLevel().isThundering()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var lightning = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
        if (lightning == null) return;
        lightning.moveTo(player.position());
        lightning.setCause(player);
        player.serverLevel().addFreshEntity(lightning);
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(event.getItemStack(), player,
                new ItemStack(LyyItems.LIGHTNING_BOTTLE.get())));
        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
    }
}
