package org.lyy.lyycore.content.wings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.registry.LyyEffects;
import org.lyy.lyycore.registry.LyyEntities;

/** Six transient collision entities share one cast and one set of already-hit targets. */
@EventBusSubscriber(modid = "lyycore")
public final class WingsScoop {
    public static final int DURATION = 40;
    public static final float CLIP_SECONDS = .96F;
    private static final Map<ServerPlayer, Attack> ACTIVE = new WeakHashMap<>();
    record Attack(int started, ResourceKey<Level> dimension, List<ScoopFeather> feathers, Set<UUID> hit) { }
    private WingsScoop() { }

    public static boolean active(ServerPlayer player) {
        var attack = ACTIVE.get(player);
        return attack != null && player.tickCount - attack.started < DURATION;
    }

    static boolean current(ServerPlayer player, Attack attack) {
        return ACTIVE.get(player) == attack && player.isAlive() && !player.isSpectator()
                && player.level().dimension().equals(attack.dimension);
    }

    public static boolean start(ServerPlayer player) {
        if (AegisWings.level(player) < 2 || !player.isAlive() || player.isSpectator()
                || player.hasEffect(LyyEffects.CRYSTALLIZATION) || active(player) || WingsAttack.busy(player)) return false;
        stop(player);
        var attack = new Attack(player.tickCount, player.level().dimension(), new ArrayList<>(ScoopPaths.COUNT), new HashSet<>());
        ACTIVE.put(player, attack);
        for (int path = 0; path < ScoopPaths.COUNT; path++) {
            var feather = LyyEntities.SCOOP_FEATHER.get().create(player.level());
            if (feather == null) { stop(player); return false; }
            feather.prepare(player, attack, path);
            if (!player.level().addFreshEntity(feather)) { stop(player); return false; }
            attack.feathers.add(feather);
        }
        WingsNetwork.scoop(player, player.getYRot());
        return true;
    }

    private static void stop(ServerPlayer player) {
        var attack = ACTIVE.remove(player);
        if (attack != null) attack.feathers.forEach(ScoopFeather::discard);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var attack = ACTIVE.get(player);
        if (attack != null && (!current(player, attack) || player.tickCount - attack.started >= DURATION)) stop(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        for (var player : List.copyOf(ACTIVE.keySet())) stop(player);
    }
}
