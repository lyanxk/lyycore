package org.lyy.lyycore.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.item.WeatherBallItem;

import java.util.UUID;

/** Laboratory results inherit ownership, except freely transferable weather balls. */
@EventBusSubscriber(modid = "lyycore")
public final class ProductionOwnership {
    private static final String OWNER = "lyycore:production_owner";

    private ProductionOwnership() { }

    public static void bind(ItemStack stack, UUID owner) {
        if (stack.getItem() instanceof WeatherBallItem) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(OWNER, owner));
    }

    public static boolean isForeign(ItemStack stack, UUID player) {
        if (stack.isEmpty() || stack.getItem() instanceof WeatherBallItem) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(OWNER) && (!tag.hasUUID(OWNER) || !tag.getUUID(OWNER).equals(player));
    }

    private static void destroyForeign(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer) || !isForeign(stack, player.getUUID())) return;
        stack.setCount(0);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.lyycore.foreign_production").withColor(0xED8CBC), true);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        destroyForeign(player, player.getMainHandItem());
        destroyForeign(player, player.getOffhandItem());
    }

    // Also reject actions between a hand swap and the next player tick.
    private static void interact(PlayerInteractEvent event) {
        if (isForeign(event.getItemStack(), event.getEntity().getUUID()) && event instanceof ICancellableEvent cancellable) {
            destroyForeign(event.getEntity(), event.getItemStack());
            cancellable.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightItem(PlayerInteractEvent.RightClickItem event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightBlock(PlayerInteractEvent.RightClickBlock event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftBlock(PlayerInteractEvent.LeftClickBlock event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightEntity(PlayerInteractEvent.EntityInteract event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interact(event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var player = event.getEntity();
        if (isForeign(player.getMainHandItem(), player.getUUID())) {
            destroyForeign(player, player.getMainHandItem());
            event.setCanceled(true);
        }
    }
}
