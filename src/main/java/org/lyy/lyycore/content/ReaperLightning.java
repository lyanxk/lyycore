package org.lyy.lyycore.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.network.ReaperLightningPayload;
import org.lyy.lyycore.registry.LyyItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

@EventBusSubscriber(modid = "lyycore")
public final class ReaperLightning {
    public static final int MAX_TARGETS = 5;
    public static final double LINK_RANGE = 3;
    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);

    private ReaperLightning() { }

    @SubscribeEvent public static void onHit(LivingDamageEvent.Post event) {
        if (CHAINING.get() || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof ServerPlayer player)
                || !player.getMainHandItem().is(LyyItems.IMAGINARY_REAPER.get())
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0
                || friendly(event.getEntity(), player)) return;

        CHAINING.set(true);
        try {
            var level = player.serverLevel();
            var visited = new HashSet<Integer>();
            var points = new ArrayList<Vec3>();
            LivingEntity previous = event.getEntity();
            visited.add(previous.getId());
            points.add(player.getEyePosition().add(player.getLookAngle().scale(0.5)).add(0, -0.25, 0));
            points.add(previous.getBoundingBox().getCenter());
            // The primary hit has already applied cooldown, critical damage and armor.
            // Propagate its pre-armor amount, applying each new target's armor normally.
            float damage = event.getOriginalDamage();
            while (visited.size() < MAX_TARGETS) {
                Vec3 origin = previous.position();
                LivingEntity next = level.getEntitiesOfClass(LivingEntity.class, previous.getBoundingBox().inflate(LINK_RANGE),
                                target -> !visited.contains(target.getId()) && enemy(target, player)
                                        && target.position().distanceToSqr(origin) <= LINK_RANGE * LINK_RANGE)
                        .stream().min(Comparator.comparingDouble(target -> target.position().distanceToSqr(origin))).orElse(null);
                if (next == null) break;
                visited.add(next.getId());
                damage *= 0.9F;
                next.hurt(level.damageSources().playerAttack(player), damage);
                points.add(next.getBoundingBox().getCenter());
                previous = next;
            }
            PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 64,
                    new ReaperLightningPayload(points));
            level.playSound(null, event.getEntity().blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.35F, 1.7F);
        } finally {
            CHAINING.remove();
        }
    }

    private static boolean friendly(LivingEntity target, Player owner) {
        return target == owner || target.isAlliedTo(owner) || owner.isAlliedTo(target)
                || target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID());
    }

    private static boolean enemy(LivingEntity target, Player owner) {
        return target.isAlive() && !target.isSpectator() && !friendly(target, owner)
                && (target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == owner
                || owner.getLastHurtByMob() == target || target.getLastHurtByMob() == owner);
    }
}
