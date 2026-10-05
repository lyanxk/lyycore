package org.lyy.lyycore.content.control;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.content.entity.*;

@EventBusSubscriber(modid = "lyycore")
public final class MindControl {
    private static final String OWNER = "lyycore:mind_owner", BINDING = "lyycore:mind_binding";
    private MindControl() { }
    public static boolean eligible(Mob mob) {
        return mob instanceof Enemy && mob.isAlive() && !mob.getType().is(Tags.EntityTypes.BOSSES)
                && !(mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                && !(mob instanceof ImaginaryGuardian) && !(mob instanceof LifeRevel);
    }
    public static MindControlData.Binding binding(Mob mob) {
        var tag = mob.getPersistentData();
        if (!(mob.level() instanceof ServerLevel level) || !tag.hasUUID(OWNER) || !tag.hasUUID(BINDING)) return null;
        var binding = MindControlData.get(level).active(tag.getUUID(OWNER));
        return binding != null && binding.id.equals(tag.getUUID(BINDING)) && binding.mobs.contains(mob.getUUID()) ? binding : null;
    }
    public static void bind(Mob mob, MindControlData.Binding binding) {
        if (!eligible(mob)) return;
        var previous = binding(mob);
        // Repeated passive scans must not reset an already controlled monster's combat target.
        if (previous != null) return;
        mob.getPersistentData().putUUID(OWNER, binding.owner); mob.getPersistentData().putUUID(BINDING, binding.id);
        binding.mobs.add(mob.getUUID()); MindControlData.get((ServerLevel)mob.level()).setDirty();
        mob.setTarget(null); install(mob);
    }
    private static void install(Mob mob) {
        mob.targetSelector.disableControlFlag(Goal.Flag.TARGET);
        if (mob.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof GatherGoal))
            mob.goalSelector.addGoal(-1, new GatherGoal(mob));
    }
    private static void release(Mob mob) {
        mob.getPersistentData().remove(OWNER); mob.getPersistentData().remove(BINDING);
        mob.targetSelector.enableControlFlag(Goal.Flag.TARGET);
        var goals = mob.goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal()).filter(GatherGoal.class::isInstance).toList();
        goals.forEach(mob.goalSelector::removeGoal);
        mob.setTarget(null);
    }
    private static boolean validEnemy(Mob mob, LivingEntity target) {
        return target instanceof Enemy && !(target instanceof Creeper) && target != mob && target.isAlive()
                && !(target instanceof Mob other && binding(other) != null);
    }
    /** Use the normal player-kill path so loot, experience and kill credit are preserved. */
    public static void execute(Mob mob, Player player) {
        if (!mob.isAlive()) return;
        mob.invulnerableTime = 0;
        mob.hurt(mob.damageSources().playerAttack(player), Float.MAX_VALUE);
    }
    private static Player executionOwner(Mob mob, ServerLevel level) {
        UUID owner = mob.getPersistentData().getUUID(OWNER);
        var player = level.getServer().getPlayerList().getPlayer(owner);
        // A deferred execution may run after its owner logs out; retain the recorded player's UUID.
        return player != null ? player : FakePlayerFactory.get(level, new GameProfile(owner, "[Mind Control]"));
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && !event.getLevel().isClientSide && mob.getPersistentData().hasUUID(OWNER)) install(mob);
    }
    @SubscribeEvent public static void despawn(MobDespawnEvent event) {
        if (binding(event.getEntity()) != null) event.setResult(MobDespawnEvent.Result.DENY);
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        var binding = binding(mob); if (binding == null || event.getNewAboutToBeSetTarget() == null) return;
        if (binding.mode != MindControlData.Mode.ATTACK || !validEnemy(mob, event.getNewAboutToBeSetTarget())) event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent public static void friendlyFire(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Mob attacker && binding(attacker) != null) {
            if (event.getEntity() instanceof Mob victim && binding(victim) != null
                    || event.getEntity().getUUID().equals(binding(attacker).owner)) event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !mob.getPersistentData().hasUUID(OWNER)) return;
        var data = MindControlData.get(level);
        if (data.takeExecution(mob.getUUID())) { execute(mob, executionOwner(mob, level)); return; }
        var binding = binding(mob);
        if (binding == null) { release(mob); return; }
        var beaconLevel = level.getServer().getLevel(binding.beacon.dimension());
        if (beaconLevel != null && beaconLevel.hasChunkAt(binding.beacon.pos()) && !beaconLevel.getBlockState(binding.beacon.pos()).is(LyyBlocks.MIND_CONTROL_BEACON)) {
            data.release(binding.owner, binding.beacon); release(mob); return;
        }
        if (binding.mode != MindControlData.Mode.ATTACK) { if (mob.getTarget() != null) mob.setTarget(null); }
        else if (mob.tickCount % 20 == 0 && (mob.getTarget() == null || !validEnemy(mob, mob.getTarget()) || mob.distanceToSqr(mob.getTarget()) > 1024)) {
            var target = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(24), entity -> validEnemy(mob, entity)).stream()
                    .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            mob.setTarget(target);
        }
    }
    @SubscribeEvent public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level) {
            var binding = binding(mob);
            if (binding != null && binding.mobs.remove(mob.getUUID())) MindControlData.get(level).setDirty();
        }
    }
    private static final class GatherGoal extends Goal {
        private final Mob mob;
        private BlockPos destination;
        private int nextSearch;
        GatherGoal(Mob mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() {
            if (mob.tickCount < nextSearch) return false;
            nextSearch = mob.tickCount + 20;
            var binding = binding(mob);
            if (binding == null || binding.mode != MindControlData.Mode.GATHER || !binding.beacon.dimension().equals(mob.level().dimension())) return false;
            if (mob.distanceToSqr(binding.beacon.pos().getCenter()) < 25) return false;
            destination = standable(mob, binding.beacon.pos()); return destination != null;
        }
        @Override public void start() { mob.setTarget(null); mob.getNavigation().moveTo(destination.getX() + .5, destination.getY(), destination.getZ() + .5, 1.2); }
        @Override public boolean canContinueToUse() {
            var binding = binding(mob);
            return binding != null && binding.mode == MindControlData.Mode.GATHER && mob.distanceToSqr(destination.getCenter()) > 2;
        }
        @Override public void tick() {
            if (mob instanceof Phantom) mob.getMoveControl().setWantedPosition(destination.getX() + .5, destination.getY() + 1, destination.getZ() + .5, 1.2);
            else if (mob.tickCount % 20 == 0) start();
        }
        @Override public void stop() { mob.getNavigation().stop(); }
    }
    private static BlockPos standable(Mob mob, BlockPos center) {
        for (int radius = 3; radius <= 7; radius++) for (int step = 0; step < 16; step++) {
            double angle = (step + mob.getId() % 16) * Math.PI / 8;
            for (int dy = 3; dy >= -5; dy--) {
                var pos = center.offset((int)Math.round(Math.cos(angle) * radius), dy, (int)Math.round(Math.sin(angle) * radius));
                if (!mob.level().hasChunkAt(pos) || !mob.level().getWorldBorder().isWithinBounds(pos)) continue;
                var box = mob.getBoundingBox().move(pos.getX() + .5 - mob.getX(), pos.getY() - mob.getY(), pos.getZ() + .5 - mob.getZ());
                if (mob.level().getBlockState(pos.below()).isFaceSturdy(mob.level(), pos.below(), net.minecraft.core.Direction.UP)
                        && mob.level().noCollision(mob, box)) return pos;
            }
        }
        return null;
    }
}
