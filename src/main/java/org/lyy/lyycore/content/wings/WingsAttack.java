package org.lyy.lyycore.content.wings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.CombatDamage;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.registry.LyyEffects;

/** Innate weapon execution, independent of style and skill unlocks. */
@EventBusSubscriber(modid = "lyycore")
public final class WingsAttack {
    public static final int PURSUIT_HIT_TICKS = 13, PURSUIT_DURATION_TICKS = 28;
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State {
        int lastAttackTick = Integer.MIN_VALUE;
        int busyUntil;
        Pursuit pursuit;
        final List<Volley> pending = new ArrayList<>();
    }
    private record Pursuit(LivingEntity target, int hitAt) { }
    private static final class Volley {
        final LivingEntity target;
        final WingsTier tier;
        final int started;
        int feather;
        Volley(LivingEntity target, WingsTier tier, int started) {
            this.target = target; this.tier = tier; this.started = started;
        }
    }
    private WingsAttack() { }

    private static boolean canAttack(ServerPlayer player) {
        return AegisWings.unlocked(player) && player.isAlive() && !player.isSpectator()
                && !player.hasEffect(LyyEffects.CRYSTALLIZATION);
    }

    public static boolean start(ServerPlayer player, LivingEntity target) {
        if (!canAttack(player) || WingsScoop.active(player) || target == player || !target.isAlive() || target.isRemoved() || target.level() != player.level()) return false;
        var state = STATES.computeIfAbsent(player, ignored -> new State());
        int tick = player.server.getTickCount();
        if (state.lastAttackTick == tick) return false;
        int pursuit = WingsSettings.pursuit(player);
        if (pursuit == 0) return false;
        if (pursuit == 3) {
            // All feathers belong to this clip until recall finishes.
            if (busy(player)) return false;
            state.lastAttackTick = tick;
            state.busyUntil = player.tickCount + PURSUIT_DURATION_TICKS;
            state.pursuit = new Pursuit(target, player.tickCount + PURSUIT_HIT_TICKS);
            WingsNetwork.pursuit(player, target);
            return true;
        }
        state.lastAttackTick = tick;
        var tier = pursuit == 1 ? WingsTier.FIRST : WingsTier.SECOND;
        state.busyUntil = player.tickCount + (int)Math.ceil(FeatherAttack.duration(tier.featherCount()) * 20);
        // Snapshot stats so an upgrade does not change an attack already in flight.
        state.pending.add(new Volley(target, tier, player.tickCount));
        WingsNetwork.attack(player, target, tier.featherCount());
        return true;
    }
    static boolean busy(ServerPlayer player) {
        var state = STATES.get(player);
        return state != null && player.tickCount < state.busyUntil;
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var state = STATES.get(player);
        if (state == null) return;
        if (!canAttack(player)) { STATES.remove(player); return; }
        var pursuit = state.pursuit;
        if (pursuit != null && player.tickCount >= pursuit.hitAt) {
            state.pursuit = null;
            var target = pursuit.target;
            if (target.isAlive() && !target.isRemoved() && target.level() == player.level())
                CombatDamage.hit(player, target, CombatDamage.source(player, "blazing_pursuit"), 640);
        }
        for (var iterator = state.pending.iterator(); iterator.hasNext();) {
            var volley = iterator.next();
            if (!volley.target.isAlive() || volley.target.level() != player.level() || volley.target.isRemoved()) {
                iterator.remove(); continue;
            }
            while (volley.feather < volley.tier.featherCount() && volley.target.isAlive()
                    && player.tickCount - volley.started >= FeatherAttack.hitTick(volley.feather, volley.tier.featherCount())) {
                volley.feather++;
                CombatDamage.hit(player, volley.target, volley.tier.featherDamage());
            }
            if (volley.feather == volley.tier.featherCount() || !volley.target.isAlive()) iterator.remove();
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) STATES.remove(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) STATES.remove(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { STATES.clear(); }
}
