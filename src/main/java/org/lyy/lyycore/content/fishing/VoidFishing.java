package org.lyy.lyycore.content.fishing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.registry.LyyItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-only casts. Each slot is replaced independently, and never exceeds the batch limit. */
@EventBusSubscriber(modid = LyyCore.MODID)
public final class VoidFishing {
    public static final int HOOK_COUNT = 20;
    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    public static boolean toggle(ServerPlayer player, InteractionHand hand) {
        var active = CASTS.get(player.getUUID());
        if (active != null) {
            // A deliberate reel-in must run vanilla entity pulling before cleanup.
            // Death, logout and switching away still only discard the hooks.
            if (active.valid()) {
                for (var hook : active.hooks)
                    if (hook != null && !hook.isRemoved()) hook.retrieve(active.rod);
            }
            stop(player.getUUID());
            return false;
        }
        var cast = new Cast(player, hand);
        CASTS.put(player.getUUID(), cast);
        cast.fillHooks();
        return true;
    }

    static ItemStack rodFor(VoidFishingHook hook) {
        if (!(hook.getOwner() instanceof ServerPlayer player)) return ItemStack.EMPTY;
        var cast = CASTS.get(player.getUUID());
        if (cast == null || !cast.valid() || !cast.contains(hook)) return ItemStack.EMPTY;
        return cast.rod;
    }

    private static void stop(UUID player) {
        var cast = CASTS.remove(player);
        if (cast != null) for (var hook : cast.hooks) if (hook != null) hook.discard();
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var cast = CASTS.get(player.getUUID());
        if (cast == null) return;
        if (!cast.valid()) stop(player.getUUID());
        else cast.fillHooks();
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { stop(event.getEntity().getUUID()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { stop(event.getEntity().getUUID()); }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player.getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        for (var id : java.util.List.copyOf(CASTS.keySet())) stop(id);
    }

    private static final class Cast {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final InteractionHand hand;
        private final ItemStack rod;
        private final int selectedSlot;
        private final VoidFishingHook[] hooks = new VoidFishingHook[HOOK_COUNT];

        private Cast(ServerPlayer player, InteractionHand hand) {
            this.player = player;
            this.level = player.serverLevel();
            this.hand = hand;
            this.rod = player.getItemInHand(hand);
            this.selectedSlot = player.getInventory().selected;
        }

        private boolean valid() {
            return player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.serverLevel() == level
                    && player.getItemInHand(hand) == rod && rod.is(LyyItems.VOID_FISHING_ROD.get())
                    && (hand != InteractionHand.MAIN_HAND || player.getInventory().selected == selectedSlot);
        }

        private boolean contains(VoidFishingHook hook) {
            for (var candidate : hooks) if (candidate == hook) return true;
            return false;
        }

        private void fillHooks() {
            for (int i = 0; i < hooks.length; i++) {
                if (hooks[i] != null && !hooks[i].isRemoved()) continue;
                int luck = EnchantmentHelper.getFishingLuckBonus(level, rod, player);
                int lure = (int) (EnchantmentHelper.getFishingTimeReduction(level, rod, player) * 20);
                var hook = new VoidFishingHook(level, luck, lure);
                hooks[i] = hook;
                float yawOffset = -30 + 60F * i / (HOOK_COUNT - 1);
                hook.launch(player, hand, yawOffset);
                if (!level.addFreshEntity(hook)) hook.discard();
            }
        }
    }
}
