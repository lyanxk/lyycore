package org.lyy.lyycore.content;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One shared daylight transition per server; game time and scheduled tasks are not advanced. */
@EventBusSubscriber(modid = "lyycore")
public final class CelestialTime {
    private static final int DURATION = 80;
    private record Shift(long started, long dayTime, int offset) { }
    private static final Map<MinecraftServer, Shift> SHIFTS = new WeakHashMap<>();

    public static boolean active(MinecraftServer server) { return SHIFTS.containsKey(server); }

    public static boolean shift(MinecraftServer server, int hours) {
        if (active(server) || Math.abs(hours) != 6 && Math.abs(hours) != 12) return false;
        var level = server.overworld();
        long start = level.getDayTime();
        // Keep protocol time nonnegative when rewinding the world's first morning.
        if (start + hours * 1000L < 0) start += 24000;
        SHIFTS.put(server, new Shift(level.getGameTime(), start, hours * 1000));
        return true;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var shift = SHIFTS.get(server);
        if (shift == null) return;
        var level = server.overworld();
        long elapsed = Math.min(DURATION, level.getGameTime() - shift.started);
        double progress = (double) elapsed / DURATION;
        double eased = progress * progress * (3 - 2 * progress);
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        level.setDayTime(shift.dayTime + Math.round(shift.offset * eased) + (daylight ? elapsed : 0));
        // Vanilla's 20-tick synchronization would make a four-second transition visibly jump.
        for (var player : server.getPlayerList().getPlayers()) {
            var destination = player.serverLevel();
            player.connection.send(new ClientboundSetTimePacket(destination.getGameTime(), destination.getDayTime(),
                    destination.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        }
        if (elapsed == DURATION) SHIFTS.remove(server);
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SHIFTS.remove(event.getServer()); }
}
