package org.lyy.lyycore.content.cauldron;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.lyy.lyycore.registry.LyyEffects;

@EventBusSubscriber(modid = "lyycore")
public final class ExperimentEffects {
    private static final net.minecraft.world.level.ExplosionDamageCalculator DESTRUCTION = new net.minecraft.world.level.ExplosionDamageCalculator() {
        @Override public float getEntityDamageAmount(net.minecraft.world.level.Explosion explosion, net.minecraft.world.entity.Entity entity) {
            return super.getEntityDamageAmount(explosion, entity) * 100;
        }
    };
    private ExperimentEffects() { }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide || !entity.isAlive()) return;
        var effect = entity.getEffect(LyyEffects.SPATIAL_CONFUSION);
        if (effect != null && effect.getDuration() % 60 == 0) {
            var random = entity.getRandom();
            double x = entity.getX() + random.nextInt(11) - 5, z = entity.getZ() + random.nextInt(11) - 5;
            double y = Math.clamp(entity.getY() + random.nextInt(11) - 5, entity.level().getMinBuildHeight(), entity.level().getMaxBuildHeight() - 1);
            var pos = net.minecraft.core.BlockPos.containing(x, y, z);
            if (entity.level().hasChunkAt(pos) && entity.level().getWorldBorder().isWithinBounds(pos)) {
                if (entity instanceof ServerPlayer player) player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
                else entity.teleportTo(x, y, z);
            }
        }
    }
    @SubscribeEvent public static void expired(MobEffectEvent.Expired event) {
        var entity = event.getEntity();
        var effect = event.getEffectInstance();
        if (entity.level().isClientSide || effect == null || !entity.isAlive()) return;
        if (effect.is(LyyEffects.STOMACH_THUNDER)) {
            // Vanilla vertical drag/gravity give about 60 blocks of ascent at this launch speed.
            var velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x, 3.86, velocity.z);
            entity.hurtMarked = true;
        } else if (effect.is(LyyEffects.WORLD_DESTRUCTION)) {
            entity.level().explode(null, entity.damageSources().explosion(entity, entity), DESTRUCTION,
                    entity.position(), 3, false, Level.ExplosionInteraction.NONE);
        }
    }
}
