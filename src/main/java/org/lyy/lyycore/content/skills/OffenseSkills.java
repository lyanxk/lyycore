package org.lyy.lyycore.content.skills;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.CombatDamage;

/** The style decides when to attack; the equipped innate wings own the attack itself. */
@EventBusSubscriber(modid = "lyycore")
public final class OffenseSkills {
    public static final ResourceLocation SCOOP_RESEARCH = ResourceLocation.parse("lyycore:research/go_blades");
    private static final int FOLLOW_UP_COOLDOWN = 20;
    private static final Map<ServerPlayer, Integer> LAST_FOLLOW_UP = new WeakHashMap<>();
    private OffenseSkills() { }

    static boolean scoop(ServerPlayer player) {
        return SkillInput.forwardSpecial(player) && AegisWings.scoop(player);
    }

    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && !CombatDamage.extra()
                && event.getSource().getEntity() instanceof ServerPlayer player
                && BasicSkills.active(player, StyleSystem.Style.OFFENSE)) {
            int tick = player.server.getTickCount();
            var previous = LAST_FOLLOW_UP.get(player);
            if (previous != null && tick - previous < FOLLOW_UP_COOLDOWN) return;
            if (AegisWings.attack(player, event.getEntity())) LAST_FOLLOW_UP.put(player, tick);
        }
    }
    static void forget(ServerPlayer player) { LAST_FOLLOW_UP.remove(player); }
    static void clear() { LAST_FOLLOW_UP.clear(); }
}
