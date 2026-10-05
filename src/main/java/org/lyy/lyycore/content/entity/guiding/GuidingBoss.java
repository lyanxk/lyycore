package org.lyy.lyycore.content.entity.guiding;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.registry.LyyEntities;

/** Two encounter phases share targeting and persistence, with explicit non-overlapping actions. */
public final class GuidingBoss extends GuidingMob {
    public enum Action { WAIT, ABSORB, WANDER, CHASE, GRAB, PULL, DEVOUR, LASER, CRYSTALS }
    // Final balance values are centralized alongside the other attack timings.
    public static final float TRACKING_DAMAGE = 40;
    public static final int TRACKING_DAMAGE_INTERVAL = 20;
    private static final int MISSING_GRAB_GRACE_TICKS = 20;
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(GuidingBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(GuidingBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_STARTED = SynchedEntityData.defineId(GuidingBoss.class, EntityDataSerializers.LONG);
    public static final int GRAB_WRAP_TICKS = 8;
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1, .08F, .18F), 1.5F);
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
    private UUID encounter, targetId, activeGrab;
    private BlockPos gate;
    private int actionTicks, recovery, guardMask, missingGrabTicks;
    private boolean initialized, transitioned;
    private Vec3 crystalCenter = Vec3.ZERO;
    public GuidingBoss(EntityType<? extends GuidingBoss> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = secondPhase() ? 1000 : 0;
    }
    @Override protected boolean isAlwaysExperienceDropper() { return secondPhase(); }
    public boolean secondPhase() { return getType() == LyyEntities.ENDLESS_DEMAND.get(); }
    public static AttributeSupplier.Builder attributes(boolean second) {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, second ? 4000 : 2000)
                .add(Attributes.ARMOR, second ? 30 : 20).add(Attributes.ARMOR_TOUGHNESS, second ? 30 : 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.MOVEMENT_SPEED, second ? .25 : 0)
                .add(Attributes.FOLLOW_RANGE, 20);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(ACTION, Action.WAIT.ordinal()); builder.define(TARGET, -1);
        builder.define(ACTION_STARTED, 0L);
    }
    public Action action() { return Action.values()[entityData.get(ACTION)]; }
    private void action(Action action) {
        if (action != Action.PULL && action != Action.DEVOUR) activeGrab = null;
        missingGrabTicks = 0;
        entityData.set(ACTION_STARTED, level().getGameTime());
        entityData.set(ACTION, action.ordinal()); actionTicks = 0; getNavigation().stop();
        noPhysics = action == Action.ABSORB; setNoGravity(!secondPhase() || noPhysics); refreshDimensions();
        animate(switch (action) {
            case ABSORB -> "hatch";
            case GRAB, PULL -> "grab";
            case DEVOUR -> "devour";
            case LASER -> "laser_charge";
            case CRYSTALS -> "crystal_pursuit";
            case CHASE -> "pursue";
            default -> "idle";
        });
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        return action() == Action.ABSORB ? EntityDimensions.fixed(0, 0) : super.getDefaultDimensions(pose);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (ACTION.equals(data)) { noPhysics = action() == Action.ABSORB; refreshDimensions(); }
    }
    public void beginSummoning(Player player, BlockPos gate) { targetId = valid(player) ? player.getUUID() : null; this.gate = gate.immutable(); }
    public boolean grabbing() { return action() == Action.GRAB; }
    public boolean ownsGrab(UUID id) { return grabbing() && id.equals(activeGrab); }
    public boolean maintainsGrab(UUID id) {
        return id.equals(activeGrab) && (grabbing() || action() == Action.PULL || action() == Action.DEVOUR);
    }
    public boolean wrapping() {
        return action() == Action.PULL && (level().isClientSide
                ? level().getGameTime() - entityData.get(ACTION_STARTED) < GRAB_WRAP_TICKS
                : actionTicks < GRAB_WRAP_TICKS);
    }
    public void captured(Player player) {
        if (grabbing() && player.getUUID().equals(targetId)) { player.stopFallFlying(); action(Action.PULL); }
    }
    public boolean pulling(Player player) {
        return player.getId() == entityData.get(TARGET) && (action() == Action.PULL || action() == Action.DEVOUR);
    }
    public Vec3 pullVelocity(Player player) {
        if (wrapping()) return Vec3.ZERO;
        var destination = action() == Action.PULL
                ? position().add(Vec3.directionFromRotation(0, getYRot()).scale(2)).add(0, .5, 0) : position();
        var offset = destination.subtract(player.position());
        return offset.normalize().scale(Math.min(action() == Action.PULL ? 6.0 / 20.0 : .02, offset.length()));
    }
    public void grabMissed(UUID id) { if (maintainsGrab(id)) finishAction(); }
    private GuidingEncounter.Battle battle() {
        return encounter == null || !(level() instanceof ServerLevel server) ? null : GuidingEncounter.get(server).battle(encounter);
    }
    private void initialize() {
        if (initialized) return;
        initialized = true;
        if (encounter == null) encounter = getUUID();
        var data = GuidingEncounter.get((ServerLevel)level());
        var battle = data.battle(encounter);
        if (battle == null) battle = data.create(encounter, getUUID(), targetId);
        if (secondPhase()) { data.transition(encounter, getUUID(), GuidingEncounter.Phase.ABSORB); action(Action.ABSORB); noPhysics = true; }
        else { summonGuards(battle); restartAnimation("summon"); }
    }
    private void summonGuards(GuidingEncounter.Battle battle) {
        var data = GuidingEncounter.get((ServerLevel)level());
        for (int i = 0; i < 4; i++) {
            if ((guardMask & 1 << i) != 0) continue;
            var type = i < 2 ? LyyEntities.LOST_ADHERENT.get() : LyyEntities.FANATICAL_SUPPORTER.get();
            var guard = type.create(level());
            if (guard == null) continue;
            guard.bind(encounter);
            double angle = Math.PI / 4 + i * Math.PI / 2;
            var desired = position().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
            for (int height = 0; height < 5; height++) {
                guard.setPos(desired.add(0, height, 0));
                if (level().noCollision(guard) && level().addFreshEntity(guard)) {
                    data.addGuard(encounter, guard.getUUID()); break;
                }
            }
            if (battle.guards().contains(guard.getUUID())) guardMask |= 1 << i;
        }
    }
    private boolean valid(Player player) { return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator(); }
    @Override protected void customServerAiStep() {
        initialize();
        var server = (ServerLevel)level();
        var target = targetId == null ? null : server.getPlayerByUUID(targetId);
        // Losing a chosen player ends the encounter; never silently retarget after their death.
        if (targetId != null && !valid(target)) { discard(); return; }
        if (target == null) {
            target = server.getNearestPlayer(getX(), getY(), getZ(), 20, entity -> entity instanceof Player p && valid(p));
            if (target != null) {
                targetId = target.getUUID(); GuidingEncounter.get(server).setTarget(encounter, targetId);
            }
        }
        setTarget(target);
        entityData.set(TARGET, target == null ? -1 : target.getId());
        bar.setProgress(getHealth() / getMaxHealth());
        if (target != null) {
            var offset = target.position().subtract(position());
            setYRot((float)Math.toDegrees(Math.atan2(-offset.x, offset.z))); yBodyRot = yHeadRot = getYRot();
            getLookControl().setLookAt(target, 360, 360);
        }
        if (!secondPhase()) {
            if (guardMask != 15 && tickCount % 20 == 0) summonGuards(battle());
            if (!playing("summon", 1.1F) && !playing("hit", .35F))
                animate(guardMask != 15 || !battle().standing().isEmpty() ? "shield" : "idle");
            setNoGravity(true); setDeltaMovement(Vec3.ZERO); getNavigation().stop(); return;
        }
        actionTicks++;
        if (action() == Action.ABSORB) {
            noPhysics = true; setDeltaMovement(Vec3.ZERO);
            if (actionTicks >= 40) {
                noPhysics = false; var battle = battle();
                GuidingEncounter.get(server).transition(encounter, getUUID(), GuidingEncounter.Phase.DEMAND);
                for (var id : List.copyOf(battle.guards())) if (server.getEntity(id) instanceof GuidingGuard guard) guard.consume();
                finishAction();
            }
            return;
        }
        if (target == null) { getNavigation().stop(); return; }
        switch (action()) {
            case GRAB -> {
                // Do not force-load a departed projectile's chunk. A stale projectile cannot
                // complete a later grab because every cast owns a different entity UUID.
                if (activeGrab != null && server.getEntity(activeGrab) instanceof GuidingGrab grab && !grab.isRemoved()) missingGrabTicks = 0;
                else if (++missingGrabTicks >= MISSING_GRAB_GRACE_TICKS) finishAction();
            }
            case PULL -> {
                if (activeGrab == null || !(server.getEntity(activeGrab) instanceof GuidingGrab grab) || grab.isRemoved()) {
                    if (++missingGrabTicks >= MISSING_GRAB_GRACE_TICKS) finishAction();
                    break;
                }
                missingGrabTicks = 0;
                if (wrapping()) { pull(target); break; }
                Vec3 destination = position().add(Vec3.directionFromRotation(0, getYRot()).scale(2)).add(0, .5, 0);
                var offset = destination.subtract(target.position());
                pull(target);
                // If terrain blocks the pull, still proceed to devour and release afterward.
                if (offset.lengthSqr() <= .4 || actionTicks >= 200) action(Action.DEVOUR);
            }
            case DEVOUR -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false));
                pull(target);
                if (actionTicks % 20 == 0) for (var victim : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4), this::valid))
                    if (distanceToSqr(victim) <= 16) victim.hurt(damageSources().fellOutOfWorld(), 5);
                if (actionTicks >= 80) finishAction();
            }
            case LASER -> {
                if (actionTicks >= 20) animate("laser_fire");
                if (actionTicks >= 140) finishAction();
            }
            case CRYSTALS -> updateCrystals();
            default -> idle(target);
        }
    }
    private void idle(Player target) {
        // Pursuit is movement, and does not interrupt an attack already in progress.
        if (distanceToSqr(target) > 100 || action() == Action.CHASE && distanceToSqr(target) >= 16) {
            if (action() != Action.CHASE) action(Action.CHASE);
            getNavigation().moveTo(target, 1);
        } else {
            if (action() != Action.WANDER) action(Action.WANDER);
            getNavigation().stop();
            getMoveControl().strafe(distanceToSqr(target) < 16 ? -.3F : .1F, tickCount / 40 % 2 == 0 ? .35F : -.35F);
            animate(getDeltaMovement().horizontalDistanceSqr() > .0001 ? "walk" : "idle");
        }
        if (recovery > 0) { recovery--; return; }
        if (target.isFallFlying() || target.getAbilities().flying) { beginGrab(target, true); return; }
        if (action() == Action.CHASE) return;
        switch (random.nextInt(3)) {
            case 0 -> beginGrab(target, false);
            case 1 -> { action(Action.LASER); GuidingLaser.fire(this, target, true); }
            default -> { crystalCenter = target.position(); action(Action.CRYSTALS); }
        }
    }
    private void beginGrab(Player player, boolean fast) {
        action(Action.GRAB);
        activeGrab = GuidingGrab.launch(this, player, fast);
        if (activeGrab == null) finishAction();
    }
    private void pull(Player player) {
        player.setDeltaMovement(pullVelocity(player)); player.hurtMarked = true; player.fallDistance = 0;
    }
    private void updateCrystals() {
        var server = (ServerLevel)level();
        if (actionTicks <= 20 && actionTicks % 2 == 0) {
            for (int i = 0; i < 32; i++) {
                double angle = i * Math.PI / 16;
                server.sendParticles(RED, crystalCenter.x + Math.cos(angle) * 4, crystalCenter.y + .15, crystalCenter.z + Math.sin(angle) * 4,
                        1, 0, .25, 0, .02);
            }
        }
        if (actionTicks >= 20 && actionTicks < 60) {
            for (int edge = 0; edge < 5; edge++) {
                double a = edge * Math.PI * 4 / 5, b = (edge + 1) * Math.PI * 4 / 5;
                var start = crystalCenter.add(Math.cos(a) * 4, .6, Math.sin(a) * 4);
                var end = crystalCenter.add(Math.cos(b) * 4, .6, Math.sin(b) * 4);
                var point = start.lerp(end, (actionTicks % 10) / 10.0);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, point.x, point.y, point.z, 3, .08, .25, .08, .04);
            }
            if ((actionTicks - 20) % 20 == 0) {
                var bounds = new AABB(crystalCenter.add(-4, -1, -4), crystalCenter.add(4, 4, 4));
                for (var victim : server.getEntitiesOfClass(Player.class, bounds, this::valid)) {
                    var offset = victim.position().subtract(crystalCenter);
                    if (offset.x * offset.x + offset.z * offset.z <= 16) victim.hurt(damageSources().mobAttack(this), 50);
                }
            }
        }
        if (actionTicks >= 60) finishAction();
    }
    private void finishAction() { action(Action.WANDER); recovery = 40; }
    @Override public boolean hurt(DamageSource source, float amount) {
        var battle = battle();
        if (action() == Action.ABSORB || !secondPhase() && (guardMask != 15 || battle == null || !battle.standing().isEmpty())) return false;
        boolean damaged = super.hurt(source, amount);
        if (damaged && isAlive() && !secondPhase()) restartAnimation("hit");
        return damaged;
    }
    public Vec3 laserOrigin() { return position().add(new Vec3(0, 35 / 16.0, 35 / 16.0).yRot((float)Math.toRadians(-getYRot()))); }
    @Override public boolean isPushable() { return secondPhase() && action() != Action.ABSORB; }
    @Override public boolean canBeCollidedWith() { return action() != Action.ABSORB && super.canBeCollidedWith(); }
    @Override public boolean isPickable() { return action() != Action.ABSORB && super.isPickable(); }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (!dead || !(level() instanceof ServerLevel server)) return;
        restartAnimation(secondPhase() ? "death" : "shell_break");
        if (!secondPhase() && !transitioned) {
            if (encounter == null) initialize();
            var next = LyyEntities.ENDLESS_DEMAND.get().create(server);
            if (next != null) {
                next.moveTo(position()); next.encounter = encounter; next.targetId = targetId; next.gate = gate;
                next.setYRot(getYRot()); next.yBodyRot = next.yHeadRot = getYRot();
                if (server.addFreshEntity(next)) {
                    transitioned = true;
                    GuidingEncounter.get(server).transition(encounter, next.getUUID(), GuidingEncounter.Phase.ABSORB);
                    next.initialize();
                    if (gate != null && server.getBlockEntity(gate) instanceof ImaginaryGateBlockEntity block) block.replaceActiveBoss(getUUID(), next.getUUID());
                }
            }
        }
        if (!transitioned) GuidingEncounter.get(server).end(encounter);
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !transitioned && encounter != null && level() instanceof ServerLevel server) {
            var battle = battle(); GuidingEncounter.get(server).end(encounter);
            if (battle != null) for (var id : List.copyOf(battle.guards())) if (server.getEntity(id) instanceof GuidingGuard guard) guard.discard();
        }
        bar.removeAllPlayers(); super.remove(reason);
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bar.removePlayer(player); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (encounter != null) tag.putUUID("Encounter", encounter);
        if (targetId != null) tag.putUUID("Target", targetId);
        if (gate != null) tag.putLong("Gate", gate.asLong());
        tag.putBoolean("Initialized", initialized); tag.putBoolean("Transitioned", transitioned);
        tag.putInt("GuardMask", guardMask);
        tag.putString("Action", action().name()); tag.putInt("ActionTicks", actionTicks); tag.putInt("Recovery", recovery);
        tag.putDouble("CrystalX", crystalCenter.x); tag.putDouble("CrystalY", crystalCenter.y); tag.putDouble("CrystalZ", crystalCenter.z);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); encounter = tag.hasUUID("Encounter") ? tag.getUUID("Encounter") : null;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null; gate = tag.contains("Gate") ? BlockPos.of(tag.getLong("Gate")) : null;
        initialized = tag.getBoolean("Initialized"); transitioned = tag.getBoolean("Transitioned");
        guardMask = tag.getInt("GuardMask");
        if (tag.contains("Action")) entityData.set(ACTION, Action.valueOf(tag.getString("Action")).ordinal());
        actionTicks = tag.getInt("ActionTicks"); recovery = tag.getInt("Recovery"); noPhysics = action() == Action.ABSORB;
        crystalCenter = new Vec3(tag.getDouble("CrystalX"), tag.getDouble("CrystalY"), tag.getDouble("CrystalZ"));
        // A transient capture is not resumed across a boss reload. Any late projectile
        // has an obsolete token and retires rather than capturing for the next action.
        activeGrab = null; missingGrabTicks = 0;
        if (grabbing() || action() == Action.PULL || action() == Action.DEVOUR) finishAction();
    }
}
