package org.lyy.lyycore.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
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

/** Equipment inherits ownership; placeable items and weather balls remain transferable. */
@EventBusSubscriber(modid = "lyycore")
public final class ProductionOwnership {
    private static final String OWNER = "lyycore:production_owner";

    private ProductionOwnership() { }

    public static void bind(ItemStack stack, UUID owner) {
        if (exempt(stack)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(OWNER, owner));
    }

    public static boolean isForeign(ItemStack stack, UUID player) {
        if (stack.isEmpty() || exempt(stack)) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(OWNER) && (!tag.hasUUID(OWNER) || !tag.getUUID(OWNER).equals(player));
    }

    private static boolean exempt(ItemStack stack) {
        return stack.getItem() instanceof BlockItem || stack.getItem() instanceof WeatherBallItem;
    }

    /** Check once on either side; only the server destroys the rejected stack. */
    private static boolean rejectForeign(Player player, ItemStack stack) {
        if (!isForeign(stack, player.getUUID())) return false;
        if (player instanceof ServerPlayer) {
            stack.setCount(0);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.lyycore.foreign_production").withColor(0xED8CBC), true);
        }
        return true;
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        rejectForeign(player, player.getMainHandItem());
        rejectForeign(player, player.getOffhandItem());
    }

    // Also reject actions between a hand swap and the next player tick.
    private static <E extends PlayerInteractEvent & ICancellableEvent> void interact(E event) {
        if (rejectForeign(event.getEntity(), event.getItemStack())) event.setCanceled(true);
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
        if (rejectForeign(player, player.getMainHandItem())) event.setCanceled(true);
    }
}
