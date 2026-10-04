package org.lyy.lyycore.content.skills;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.lyy.lyycore.content.DamageReductionEvent;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.network.ResearchNetwork;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.registry.LyyEffects;
import java.util.*;

/** Server-owned input, guard window and short-lived attacks for the Exploration research. */
@EventBusSubscriber(modid = "lyycore")
public final class BasicSkills {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/exploration");
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> STRIKE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, ResourceLocation.parse("lyycore:skill_strike"));
    private static final String GUARD = "lyycore:guard", FLIGHT = "lyycore:building_flight";
    private static final int PERFECT_GUARD_TICKS = 4, PERFECT_COUNTER_TICKS = 2;
    private static final int INPUT_BUFFER_TICKS = 1;
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static boolean dealingExtraDamage, applyingBuildingPenalty;
    private static final class State {
        boolean held, guarding, counterPending;
        int forward, strafe, receivedAt, guardStarted;
        final List<FeatherHits> feathers = new ArrayList<>();
    }
    private static final class FeatherHits {
        final LivingEntity target;
        final int started;
        int feather;
        FeatherHits(LivingEntity target, int now) { this.target = target; started = now; }
    }
    private BasicSkills() { }
    public static void register() {
        SkillSystem.register(StyleSystem.Style.MOBILITY, SkillSystem.SPECIAL, new SkillSystem.Skill(RESEARCH, BasicSkills::dash));
        SkillSystem.register(StyleSystem.Style.TECHNIQUE, SkillSystem.SPECIAL, new SkillSystem.Skill(RESEARCH, BasicSkills::startGuard));
    }
    public static boolean available(Player player) { return SkillSystem.unlocked(player) && (player.level().isClientSide ? player.getPersistentData().getBoolean("lyycore:exploration") : ResearchProgress.completed(player, RESEARCH)); }
    private static boolean active(ServerPlayer player, StyleSystem.Style style) {
        return available(player) && player.isAlive() && !player.isSpectator() && !player.hasEffect(LyyEffects.CRYSTALLIZATION)
                && StyleSystem.current(player) == style;
    }
    public static int guard(Player player) { return Math.clamp(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(GUARD), 0, 100); }
    public static void setGuard(Player player, int value) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(GUARD, Math.clamp(value, 0, 100));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        if (player instanceof ServerPlayer server) ResearchNetwork.sync(server);
    }
    private static boolean guarding(State state, int tick) {
        int elapsed = tick - state.guardStarted;
        return state.guarding && (state.held || elapsed <= INPUT_BUFFER_TICKS
                || state.counterPending && elapsed < PERFECT_COUNTER_TICKS);
    }
    public static boolean guarding(ServerPlayer player) { var state = STATES.get(player); return state != null && guarding(state, player.tickCount); }
    public static void input(ServerPlayer player, boolean held, int forward, int strafe) {
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        boolean wasGuarding = state.guarding;
        boolean pressed = held && !state.held;
        state.held = held; state.forward = Integer.signum(forward); state.strafe = Integer.signum(strafe); state.receivedAt = player.tickCount;
        if (!SkillSystem.canUse(player) || !available(player)) { resetGuard(state); if (wasGuarding) ResearchNetwork.sync(player); return; }
        // A plain tap survives the next combat tick; a counter tap owns its full
        // short judgment window. Once those buffers end, release stops guarding.
        if (!guarding(state, player.tickCount)) state.guarding = false;
        if (pressed) SkillSystem.cast(player, SkillSystem.SPECIAL);
        if (wasGuarding != state.guarding) ResearchNetwork.sync(player);
    }
    private static void resetGuard(State state) { state.held = state.guarding = state.counterPending = false; }
    public static void resetInput(ServerPlayer player) {
        var state = STATES.get(player);
        if (state != null) state.guarding = state.counterPending = false;
    }
    private static boolean dash(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) return false;
        Vec3 direction = player.getLookAngle();
        if (state.forward != 0 || state.strafe != 0) {
            double yaw = Math.toRadians(player.getYRot());
            direction = new Vec3(-Math.sin(yaw) * state.forward + Math.cos(yaw) * state.strafe, 0,
                    Math.cos(yaw) * state.forward + Math.sin(yaw) * state.strafe).normalize();
        }
        Vec3 destination = player.position();
        for (int step = 1; step <= 16; step++) {
            Vec3 offset = direction.scale(step * .25);
            var bounds = player.getBoundingBox().move(offset);
            if (!player.level().hasChunkAt(net.minecraft.core.BlockPos.containing(player.position().add(offset)))
                    || !player.level().getWorldBorder().isWithinBounds(bounds) || !player.level().noCollision(player, bounds)) break;
            destination = player.position().add(offset);
        }
        if (destination.distanceToSqr(player.position()) < .01) return false;
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 8, .2, .2, .2, .02);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0;
        return true;
    }
    private static boolean startGuard(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) return false;
        state.guarding = true; state.guardStarted = player.tickCount;
        // W is a modifier sampled when the special key is pressed, not a second action.
        state.counterPending = state.forward > 0;
        return true;
    }
    private static void finishOrdinaryCounter(ServerPlayer player, State state) {
        state.counterPending = false;
        if (guard(player) == 0) return;
        var target = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                entity -> hostile(entity, player) && player.distanceToSqr(entity) <= 100).stream()
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target == null) return;
        counterattack(player, target, 0);
    }
    private static boolean hostile(LivingEntity target, Player player) {
        return target != player && target.isAlive() && !target.isAlliedTo(player)
                && !(target instanceof TamableAnimal pet && pet.isOwnedBy(player))
                && (target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == player);
    }
    private static void counterattack(ServerPlayer player, LivingEntity target, float bonusDamage) {
        float damage = guard(player) * .4F + bonusDamage;
        setGuard(player, 0);
        physicalHit(player, target, damage);
    }
    @SubscribeEvent public static void guardDamage(DamageReductionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player, StyleSystem.Style.TECHNIQUE)
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;
        State state = STATES.get(player);
        if (state == null || !guarding(state, player.tickCount) || player.tickCount - state.receivedAt > 20) return;
        int elapsed = player.tickCount - state.guardStarted;
        if (elapsed < PERFECT_GUARD_TICKS) {
            event.reduceBy(1);
            setGuard(player, guard(player) + 10);
            if (state.counterPending && elapsed < PERFECT_COUNTER_TICKS
                    && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player
                    && attacker.isAlive() && !attacker.isRemoved() && attacker.level() == player.level()) {
                // Consume this press before applying damage, including reflected/reentrant hits.
                state.counterPending = false;
                counterattack(player, attacker, 40);
            }
        } else if (guard(player) >= 4) {
            event.reduceBy(.5);
            setGuard(player, guard(player) - 4);
        }
    }
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        if (event.getEntity() instanceof ServerPlayer player && active(player, StyleSystem.Style.BUILDING) && !applyingBuildingPenalty) {
            int invulnerability = player.invulnerableTime;
            applyingBuildingPenalty = true;
            try { player.invulnerableTime = 0; player.hurt(player.damageSources().fellOutOfWorld(), player.getMaxHealth() / 4); }
            finally { player.invulnerableTime = invulnerability; applyingBuildingPenalty = false; }
        }
        if (!dealingExtraDamage && event.getSource().getEntity() instanceof ServerPlayer player && active(player, StyleSystem.Style.OFFENSE)
                && event.getEntity() != player && event.getEntity().isAlive()) {
            STATES.computeIfAbsent(player, ignored -> new State()).feathers.add(new FeatherHits(event.getEntity(), player.tickCount));
            WingsNetwork.attack(player, event.getEntity());
        }
    }
    private static void physicalHit(ServerPlayer player, LivingEntity target, float amount) {
        if (amount <= 0) return;
        int invulnerability = target.invulnerableTime;
        boolean wasExtra = dealingExtraDamage;
        dealingExtraDamage = true;
        try { target.invulnerableTime = 0; target.hurt(player.damageSources().source(STRIKE, player), amount); }
        finally { target.invulnerableTime = invulnerability; dealingExtraDamage = wasExtra; }
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 3, .2, .3, .2, .01);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        updateFlight(player);
        State state = STATES.get(player);
        if (state == null) return;
        if (!player.isAlive() || !available(player)) { resetGuard(state); state.feathers.clear(); return; }
        boolean wasGuarding = state.guarding;
        if (!guarding(state, player.tickCount)) state.guarding = false;
        if (player.tickCount - state.receivedAt > 20) resetGuard(state);
        if (!active(player, StyleSystem.Style.TECHNIQUE) || player.containerMenu != player.inventoryMenu) {
            state.guarding = state.counterPending = false;
        }
        // Give the same key press its short perfect window before spending guard on a normal counter.
        if (state.counterPending && player.tickCount - state.guardStarted >= PERFECT_COUNTER_TICKS)
            finishOrdinaryCounter(player, state);
        if (wasGuarding != state.guarding) ResearchNetwork.sync(player);
        if (state.guarding && player.getDeltaMovement().y < 0) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1)); player.fallDistance = 0; player.hurtMarked = true;
        }
        for (var iterator = state.feathers.iterator(); iterator.hasNext();) {
            var hit = iterator.next();
            if (!hit.target.isAlive() || hit.target.level() != player.level() || hit.target.isRemoved()) { iterator.remove(); continue; }
            while (hit.feather < 4 && hit.target.isAlive() && player.tickCount - hit.started >= FeatherAttack.hitTick(hit.feather)) {
                physicalHit(player, hit.target, 10);
                hit.feather++;
            }
            if (hit.feather == 4 || !hit.target.isAlive()) iterator.remove();
        }
    }
    public static void updateFlight(ServerPlayer player) {
        var abilities = player.getAbilities();
        boolean enabled = active(player, StyleSystem.Style.BUILDING);
        boolean owned = player.getPersistentData().getBoolean(FLIGHT);
        if (enabled && !abilities.mayfly) {
            abilities.mayfly = true; player.getPersistentData().putBoolean(FLIGHT, true); player.onUpdateAbilities();
        } else if (!enabled && owned) {
            player.getPersistentData().remove(FLIGHT);
            if (!player.isCreative() && !player.isSpectator()) { abilities.mayfly = abilities.flying = false; player.onUpdateAbilities(); }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) STATES.remove(player); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { STATES.clear(); }
}
