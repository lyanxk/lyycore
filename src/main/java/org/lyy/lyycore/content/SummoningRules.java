package org.lyy.lyycore.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import org.lyy.lyycore.content.entity.*;
import org.lyy.lyycore.content.entity.guiding.*;
import org.lyy.lyycore.content.entity.sovereign.*;

/** Encounter summons share one dimension policy; ordinary weapon and skill effects do not. */
@EventBusSubscriber(modid = "lyycore")
public final class SummoningRules {
    private SummoningRules() { }

    public static boolean allowed(Level level) { return Level.OVERWORLD.equals(level.dimension()); }

    private static boolean summonActor(Entity entity) {
        return entity instanceof ImaginaryGuardian || entity instanceof GuardianCrystal || entity instanceof GuardianSpikes
                || entity instanceof LifeRevel || entity instanceof RevelMob || entity instanceof RevelMinion
                || entity instanceof GuidingBoss || entity instanceof GuidingGuard
                || entity instanceof GuidingGrab || entity instanceof GuidingLaser
                || entity instanceof LifeSovereign || entity instanceof LifeSpell || entity instanceof DefenderSword
                || entity instanceof CrystalTroop || entity instanceof EnderCompanion
                || entity instanceof ImaginaryDragon || entity instanceof GateMeteor;
    }

    /** Also guards subordinate summons and alternative spawn entry points, without touching existing saved entities. */
    @SubscribeEvent public static void spawn(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && !event.loadedFromDisk()
                && !allowed(event.getLevel()) && summonActor(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event) {
        if (!event.getDimension().equals(event.getEntity().level().dimension()) && summonActor(event.getEntity()))
            event.setCanceled(true);
    }
}
