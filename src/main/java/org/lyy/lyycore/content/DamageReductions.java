package org.lyy.lyycore.content;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** One settlement point for reductions contributed by research, equipment and other effects. */
@EventBusSubscriber(modid = "lyycore")
public final class DamageReductions {
    private DamageReductions() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void apply(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide || event.getNewDamage() <= 0) return;

        var reductions = new DamageReductionEvent(event.getEntity(), event.getSource(), event.getNewDamage());
        NeoForge.EVENT_BUS.post(reductions);
        // Contributors only multiply remaining-damage fractions. Write the result once,
        // then let vanilla consume absorption and health using that combined amount.
        event.setNewDamage((float) (reductions.getDamage() * reductions.getMultiplier()));
    }
}
