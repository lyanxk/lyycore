package org.lyy.lyycore.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.entity.ImaginaryGuardian;
import org.lyy.lyycore.registry.LyyEntities;

@EventBusSubscriber(modid = LyyCore.MODID)
public final class GuardianEvents {
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(LyyEntities.IMAGINARY_GUARDIAN.get(), ImaginaryGuardian.attributes().build());
    }
    @SubscribeEvent public static void playerDied(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;
        for (var entity : level.getAllEntities())
            if (entity instanceof ImaginaryGuardian guardian && guardian.isParticipant(player)) guardian.discard();
    }
}
