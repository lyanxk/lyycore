package org.lyy.lyycore.content.skills;

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
    private OffenseSkills() { }

    static boolean scoop(ServerPlayer player) {
        return SkillInput.forwardSpecial(player) && AegisWings.scoop(player);
    }

    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && !CombatDamage.extra()
                && event.getSource().getEntity() instanceof ServerPlayer player
                && BasicSkills.active(player, StyleSystem.Style.OFFENSE)) {
            AegisWings.attack(player, event.getEntity());
        }
    }
}
