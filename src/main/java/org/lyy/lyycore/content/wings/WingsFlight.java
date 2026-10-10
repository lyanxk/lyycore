package org.lyy.lyycore.content.wings;

import com.illusivesoulworks.caelus.api.CaelusApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.AegisWings;
import java.util.Map;
import java.util.WeakHashMap;

/** Research grants a Caelus attribute; Caelus owns input, flight state and attribute synchronization. */
@EventBusSubscriber(modid = "lyycore")
public final class WingsFlight {
    private static final AttributeModifier FLIGHT = new AttributeModifier(
            ResourceLocation.parse("lyycore:aegis_wings_flight"), 1, AttributeModifier.Operation.ADD_VALUE);
    private static final Map<ServerPlayer, Long> LAST_GLIDE = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> BOOST_INPUT = new WeakHashMap<>();
    private WingsFlight() { }
    public static boolean boosting(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getBoolean("lyycore:wings_boosting");
    }
    public static void input(ServerPlayer player, boolean held) {
        if (held && AegisWings.level(player) >= 2 && player.isFallFlying() && player.containerMenu == player.inventoryMenu)
            BOOST_INPUT.put(player, player.tickCount);
        else BOOST_INPUT.remove(player);
    }

    /** Called for research changes and login/respawn/dimension sync. Never alter another provider's modifier. */
    public static void update(ServerPlayer player) {
        var attribute = player.getAttribute(CaelusApi.getInstance().getFallFlyingAttribute());
        if (attribute == null) throw new IllegalStateException("Caelus flight attribute missing from player");
        if (AegisWings.unlocked(player) && WingsSettings.flight(player)) {
            if (!attribute.hasModifier(FLIGHT.id())) attribute.addTransientModifier(FLIGHT);
        } else attribute.removeModifier(FLIGHT.id());
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }
    @SubscribeEvent(receiveCanceled = true) public static void glide(VanillaGameEvent event) {
        // Also remember canceled events: a compatibility mod's cancellation must not trigger a replacement.
        if (event.getVanillaEvent().equals(GameEvent.ELYTRA_GLIDE) && event.getCause() instanceof ServerPlayer player)
            LAST_GLIDE.put(player, event.getLevel().getGameTime());
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean boost = player.isAlive() && !player.isSpectator() && player.isFallFlying() && AegisWings.level(player) >= 2
                && WingsSettings.flight(player) && WingsSettings.acceleration(player) > 0
                && player.containerMenu == player.inventoryMenu && player.tickCount - BOOST_INPUT.getOrDefault(player, player.tickCount - 100) < 20;
        if (boost != boosting(player)) {
            player.getPersistentData().putBoolean("lyycore:wings_boosting", boost);
            org.lyy.lyycore.network.WingsNetwork.sync(player);
        }
        if (boost) AegisWings.boostFlight(player);
        if (!player.isFallFlying() || !AegisWings.unlocked(player)) return;
        int ticks = player.getFallFlyingTicks();
        long now = player.level().getGameTime();
        if (ticks > 0 && ticks % 10 == 0 && !Long.valueOf(now).equals(LAST_GLIDE.get(player)))
            player.gameEvent(GameEvent.ELYTRA_GLIDE);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { LAST_GLIDE.remove(event.getEntity()); BOOST_INPUT.remove(event.getEntity()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LAST_GLIDE.clear(); BOOST_INPUT.clear(); }
}
