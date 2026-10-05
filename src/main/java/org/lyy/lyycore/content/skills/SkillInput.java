package org.lyy.lyycore.content.skills;

import net.minecraft.server.level.ServerPlayer;
import org.lyy.lyycore.network.SkillNetwork;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-owned key edges and directional modifiers; holding a key does not recast it. */
public final class SkillInput {
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State { boolean held; int forward, strafe, receivedAt; }
    private SkillInput() { }

    public static void accept(ServerPlayer player, boolean held, int forward, int strafe) {
        var state = STATES.computeIfAbsent(player, ignored -> new State());
        boolean pressed = held && !state.held;
        state.held = held;
        state.forward = Integer.signum(forward);
        state.strafe = Integer.signum(strafe);
        state.receivedAt = player.tickCount;
        if (!SkillSystem.canUse(player)) {
            state.held = false;
            GuardSkill.reset(player);
        } else {
            GuardSkill.releaseIfExpired(player);
            if (pressed) SkillSystem.cast(player, SkillSystem.SPECIAL);
        }
        SkillNetwork.sync(player);
    }

    public static boolean held(ServerPlayer player) { var state = STATES.get(player); return state != null && state.held; }
    public static boolean fresh(ServerPlayer player) { var state = STATES.get(player); return state != null && player.tickCount - state.receivedAt <= 20; }
    public static int forward(ServerPlayer player) { var state = STATES.get(player); return state == null ? 0 : state.forward; }
    public static int strafe(ServerPlayer player) { var state = STATES.get(player); return state == null ? 0 : state.strafe; }
    public static boolean forwardSpecial(ServerPlayer player) {
        return fresh(player) && player.isShiftKeyDown() && forward(player) > 0;
    }

    static void tick(ServerPlayer player) {
        if (!fresh(player) || !SkillSystem.canUse(player)) {
            var state = STATES.get(player);
            if (state != null) state.held = false;
            GuardSkill.reset(player);
        }
    }
    static void forget(ServerPlayer player) { STATES.remove(player); }
    static void clear() { STATES.clear(); }
}
