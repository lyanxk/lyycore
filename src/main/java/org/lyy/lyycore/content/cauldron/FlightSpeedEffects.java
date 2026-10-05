package org.lyy.lyycore.content.cauldron;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.registry.LyyEffects;

/** Boost creative-style flight through vanilla abilities, without changing glide physics. */
@EventBusSubscriber(modid = "lyycore")
public final class FlightSpeedEffects {
    private static final String BASE_SPEED = "lyycore:flight_speed_base";
    private static final float MULTIPLIER = 1.5F;

    private FlightSpeedEffects() { }

    public static void update(ServerPlayer player) {
        update(player, player.isAlive() && !player.isSpectator() && player.getAbilities().mayfly
                && player.hasEffect(LyyEffects.FLIGHT_SPEED));
    }

    private static void update(ServerPlayer player, boolean enabled) {
        var data = player.getPersistentData();
        var abilities = player.getAbilities();
        float current = abilities.getFlyingSpeed();
        if (data.contains(BASE_SPEED, Tag.TAG_FLOAT)) {
            float base = data.getFloat(BASE_SPEED);
            if (Float.compare(current, base * MULTIPLIER) != 0) {
                // Another provider replaced the speed. Adopt its value rather than restore stale state.
                data.remove(BASE_SPEED);
            } else if (enabled) {
                return;
            } else {
                data.remove(BASE_SPEED);
                abilities.setFlyingSpeed(base);
                player.onUpdateAbilities();
                return;
            }
        }
        float boosted = current * MULTIPLIER;
        if (enabled && current >= 0 && Float.isFinite(boosted)) {
            // Keep the base alongside saved abilities so login cannot compound a saved boost.
            data.putFloat(BASE_SPEED, current);
            abilities.setFlyingSpeed(boosted);
            if (Float.compare(current, boosted) != 0) player.onUpdateAbilities();
        }
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player, false);
    }
}
