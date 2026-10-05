package org.lyy.lyycore.content.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.CombatDamage;
import org.lyy.lyycore.content.DamageReductionEvent;
import org.lyy.lyycore.network.SkillNetwork;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "lyycore")
public final class GuardSkill {
    private static final String GUARD = "lyycore:guard";
    private static final ResourceLocation COUNTER = ResourceLocation.parse("lyycore:guard_counter");
    private static final int PERFECT_GUARD_TICKS = 4, PERFECT_COUNTER_TICKS = 4, INPUT_BUFFER_TICKS = 1;
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State { boolean guarding, counterPending; int started; }
    private GuardSkill() { }

    public static int value(Player player) { return Math.clamp(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(GUARD), 0, 100); }
    public static void setValue(Player player, int value) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(GUARD, Math.clamp(value, 0, 100));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static boolean guarding(ServerPlayer player) {
        var state = STATES.get(player);
        if (state == null) return false;
        int elapsed = player.tickCount - state.started;
        return state.guarding && (SkillInput.held(player) || elapsed <= INPUT_BUFFER_TICKS
                || state.counterPending && elapsed < PERFECT_COUNTER_TICKS);
    }
    public static void reset(ServerPlayer player) {
        var state = STATES.get(player);
        if (state != null) state.guarding = state.counterPending = false;
    }
    static void releaseIfExpired(ServerPlayer player) {
        if (!guarding(player)) { var state = STATES.get(player); if (state != null) state.guarding = false; }
    }
    static boolean start(ServerPlayer player) {
        if (!SkillInput.fresh(player)) return false;
        var state = STATES.computeIfAbsent(player, ignored -> new State());
        state.guarding = true;
        state.started = player.tickCount;
        state.counterPending = player.isShiftKeyDown() && SkillInput.forward(player) > 0;
        AegisWings.showShield(player);
        return true;
    }
    private static void counterattack(ServerPlayer player, LivingEntity target, float bonus) {
        if (!SkillSystem.consumeAction(player, COUNTER)) return;
        float damage = value(player) * .4F + bonus;
        setValue(player, 0);
        CombatDamage.hit(player, target, damage);
    }
    private static void ordinaryCounter(ServerPlayer player, State state) {
        state.counterPending = false;
        if (!player.isShiftKeyDown() || value(player) == 0) return;
        var target = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                entity -> hostile(entity, player) && player.distanceToSqr(entity) <= 100).stream()
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target != null) counterattack(player, target, 0);
    }
    private static boolean hostile(LivingEntity target, Player player) {
        return target != player && target.isAlive() && !target.isAlliedTo(player)
                && !(target instanceof TamableAnimal pet && pet.isOwnedBy(player))
                && (target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == player);
    }
    private static State activeGuard(ServerPlayer player, DamageSource source) {
        if (!BasicSkills.active(player, StyleSystem.Style.TECHNIQUE)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD) || !SkillInput.fresh(player) || !guarding(player)) return null;
        return STATES.get(player);
    }

    // Cancel before armor, absorption, knockback and hurt feedback are processed.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void perfectGuard(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        var state = activeGuard(player, event.getSource());
        if (state == null) return;
        int elapsed = player.tickCount - state.started;
        if (elapsed >= PERFECT_GUARD_TICKS) return;

        event.setCanceled(true);
        setValue(player, value(player) + 10);
        if (state.counterPending && player.isShiftKeyDown() && elapsed < PERFECT_COUNTER_TICKS
                && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player
                && attacker.isAlive() && !attacker.isRemoved() && attacker.level() == player.level()) {
            state.counterPending = false;
            counterattack(player, attacker, 40);
        }
        SkillNetwork.sync(player);
    }

    @SubscribeEvent public static void damage(DamageReductionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var state = activeGuard(player, event.getSource());
        if (state == null || player.tickCount - state.started < PERFECT_GUARD_TICKS || value(player) < 4) return;
        event.reduceBy(.5);
        setValue(player, value(player) - 4);
        SkillNetwork.sync(player);
    }
    static void tick(ServerPlayer player) {
        var state = STATES.get(player);
        if (state == null) return;
        releaseIfExpired(player);
        if (!BasicSkills.active(player, StyleSystem.Style.TECHNIQUE) || player.containerMenu != player.inventoryMenu) reset(player);
        if (state.counterPending && player.tickCount - state.started >= PERFECT_COUNTER_TICKS) ordinaryCounter(player, state);
        if (state.guarding && player.getDeltaMovement().y < 0) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
            player.fallDistance = 0;
            player.hurtMarked = true;
        }
    }
    static void forget(ServerPlayer player) { STATES.remove(player); }
    static void clear() { STATES.clear(); }
}
