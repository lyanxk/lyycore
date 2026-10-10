package org.lyy.lyycore.content.control;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
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
    /** Crystal troops can be registered for execution, but never become controlled allies. */
    public static boolean executionOnly(Mob mob) { return mob instanceof CrystalTroop; }
    public static MindControlData.Binding binding(Mob mob) {
        return executionOnly(mob) ? null : registeredBinding(mob);
    }
    private static MindControlData.Binding registeredBinding(Mob mob) {
        var tag = mob.getPersistentData();
        if (!(mob.level() instanceof ServerLevel level) || !tag.hasUUID(OWNER) || !tag.hasUUID(BINDING)) return null;
        var binding = MindControlData.get(level).active(tag.getUUID(OWNER));
        return binding != null && binding.id.equals(tag.getUUID(BINDING)) && binding.mobs.contains(mob.getUUID()) ? binding : null;
    }
    private static void resolveLegacyExecution(Mob mob, MindControlData data) {
        var tag = mob.getPersistentData();
        if (data.hasLegacyExecution(mob.getUUID()) && tag.hasUUID(OWNER) && tag.hasUUID(BINDING))
            data.resolveLegacyExecution(mob.getUUID(), tag.getUUID(OWNER), tag.getUUID(BINDING));
    }
    public static void bind(Mob mob, MindControlData.Binding binding) {
        if (!(mob.level() instanceof ServerLevel server) || !eligible(mob)) return;
        var data = MindControlData.get(server);
        resolveLegacyExecution(mob, data);
        // Do not overwrite the only attribution in an incomplete legacy execution record.
        if (data.hasLegacyExecution(mob.getUUID())) return;
        if (registeredBinding(mob) != null || !data.addMob(binding, mob.getUUID())) return;
        mob.getPersistentData().putUUID(OWNER, binding.owner); mob.getPersistentData().putUUID(BINDING, binding.id);
        if (!executionOnly(mob)) mob.setTarget(null);
        install(mob);
    }
    private static void install(Mob mob) {
        if (executionOnly(mob)) { removeControlGoals(mob); return; }
        mob.targetSelector.disableControlFlag(Goal.Flag.TARGET);
        if (mob.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof GatherGoal))
            mob.goalSelector.addGoal(-1, new GatherGoal(mob));
    }
    private static void removeControlGoals(Mob mob) {
        mob.targetSelector.enableControlFlag(Goal.Flag.TARGET);
        var goals = mob.goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal()).filter(GatherGoal.class::isInstance).toList();
        goals.forEach(mob.goalSelector::removeGoal);
    }
    private static void release(Mob mob) {
        mob.getPersistentData().remove(OWNER); mob.getPersistentData().remove(BINDING);
        removeControlGoals(mob);
        if (!executionOnly(mob)) mob.setTarget(null);
    }
    private static boolean validEnemy(Mob mob, LivingEntity target) {
        return target instanceof Enemy && !(target instanceof Creeper) && target != mob && target.isAlive()
                && !(target instanceof Mob other && binding(other) != null);
    }
    /** Normal player-kill damage: canceled damage and custom death rules remain authoritative. */
    public static boolean execute(Mob mob, Player player) {
        if (!mob.isAlive()) return true;
        int invulnerability = mob.invulnerableTime;
        try {
            mob.invulnerableTime = 0;
            mob.hurt(mob.damageSources().playerAttack(player), Float.MAX_VALUE);
            return !mob.isAlive();
        } finally { mob.invulnerableTime = invulnerability; }
    }
    private static Player executionOwner(MindControlData.Execution request, ServerLevel level) {
        var player = level.getServer().getPlayerList().getPlayer(request.owner());
        return player != null ? player : FakePlayerFactory.get(level, new GameProfile(request.owner(), "[Mind Control]"));
    }
    /** One attempt only. A failed request remains recorded until an explicit retry or removal. */
    public static void processExecution(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        var data = MindControlData.get(level);
        resolveLegacyExecution(mob, data);
        var request = data.beginExecution(mob.getUUID());
        if (request == null) return;
        boolean died = execute(mob, executionOwner(request, level));
        data.finishExecution(request, died);
        if (!died) {
            var player = level.getServer().getPlayerList().getPlayer(request.owner());
            if (player != null) player.displayClientMessage(Component.translatableWithFallback(
                    "message.lyycore.mind_execution_failed", "处决 %s 失败；仍有效的控制关系已保留。", mob.getDisplayName()), true);
        }
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level && mob.getPersistentData().hasUUID(OWNER)) {
            resolveLegacyExecution(mob, MindControlData.get(level));
            install(mob);
        }
    }
    @SubscribeEvent public static void despawn(MobDespawnEvent event) {
        if (binding(event.getEntity()) != null) event.setResult(MobDespawnEvent.Result.DENY);
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        var binding = binding(mob); if (binding == null || event.getNewAboutToBeSetTarget() == null) return;
        if (binding.mode() != MindControlData.Mode.ATTACK || !validEnemy(mob, event.getNewAboutToBeSetTarget())) event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent public static void friendlyFire(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker)) return;
        var binding = binding(attacker);
        if (binding == null) return;
        if (event.getEntity().getUUID().equals(binding.owner)
                || event.getEntity() instanceof Mob victim && binding(victim) != null) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !mob.getPersistentData().hasUUID(OWNER)) return;
        processExecution(mob);
        if (!mob.isAlive()) return;
        var data = MindControlData.get(level);
        var binding = registeredBinding(mob);
        if (binding == null) { release(mob); return; }
        var beaconLevel = level.getServer().getLevel(binding.beacon.dimension());
        if (beaconLevel != null && beaconLevel.hasChunkAt(binding.beacon.pos()) && !beaconLevel.getBlockState(binding.beacon.pos()).is(LyyBlocks.MIND_CONTROL_BEACON)) {
            data.release(binding.owner, binding.beacon); release(mob); return;
        }
        if (executionOnly(mob)) return;
        if (binding.mode() != MindControlData.Mode.ATTACK) { if (mob.getTarget() != null) mob.setTarget(null); }
        else if (mob.tickCount % 20 == 0 && (mob.getTarget() == null || !validEnemy(mob, mob.getTarget()) || mob.distanceToSqr(mob.getTarget()) > 1024)) {
            var target = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(24), entity -> validEnemy(mob, entity)).stream()
                    .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            mob.setTarget(target);
        }
    }
    /** Cancellable death events are not final. Cleanup follows permanent entity removal only. */
    @SubscribeEvent public static void left(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getLevel() instanceof ServerLevel level)
                || mob.getRemovalReason() == null || !mob.getRemovalReason().shouldDestroy()) return;
        var data = MindControlData.get(level);
        var binding = registeredBinding(mob);
        if (binding != null) data.removeMob(binding, mob.getUUID());
        data.forgetExecution(mob.getUUID());
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
            if (binding == null || binding.mode() != MindControlData.Mode.GATHER || !binding.beacon.dimension().equals(mob.level().dimension())) return false;
            if (mob.distanceToSqr(binding.beacon.pos().getCenter()) < 25) return false;
            destination = standable(mob, binding.beacon.pos()); return destination != null;
        }
        @Override public void start() { mob.setTarget(null); mob.getNavigation().moveTo(destination.getX() + .5, destination.getY(), destination.getZ() + .5, 1.2); }
        @Override public boolean canContinueToUse() {
            var binding = binding(mob);
            return binding != null && binding.mode() == MindControlData.Mode.GATHER && binding.beacon.dimension().equals(mob.level().dimension())
                    && mob.distanceToSqr(destination.getCenter()) > 2;
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
