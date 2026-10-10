package org.lyy.lyycore.content.entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.lyy.lyycore.content.EnderCompanions;
import org.lyy.lyycore.content.CombatDamage;
import org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity;

/** Short-lived attack actor. Long-lived owner, mode and production state belong to the nest. */
public final class ImaginaryDragon extends Entity {
    public enum Action { CHARGE, FALL, IDLE, BREATH, TELEPORT_IN, TELEPORT_OUT }
    public static final int APPEAR_TICKS = 72, DEPART_TICKS = 56;
    public static final int BREATH_WINDUP_TICKS = 60, BREATH_DAMAGE_TICKS = 100, BREATH_RECOVERY_TICKS = 20;
    // Authored clip: ascent/charge 0–6s, fire 6–9s, recovery 9–10s.
    private static final float BREATH_FIRE_START_SECONDS = 6, BREATH_FIRE_END_SECONDS = 9, BREATH_END_SECONDS = 10;
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(ImaginaryDragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> STARTED = SynchedEntityData.defineId(ImaginaryDragon.class, EntityDataSerializers.LONG);
    private UUID owner;
    private BlockPos nest = BlockPos.ZERO;
    private Vec3 destination = Vec3.ZERO;
    private Vec3 arrival = Vec3.ZERO;
    private float arrivalYaw;
    private Action nextAction = Action.IDLE;
    private boolean dismissing;
    private final Set<UUID> hit = new HashSet<>();
    public ImaginaryDragon(EntityType<? extends ImaginaryDragon> type, Level level) { super(type, level); setInvulnerable(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(ACTION, 0); b.define(STARTED, 0L); }
    public Action action() { return Action.values()[entityData.get(ACTION)]; }
    public float actionTicks(float partial) { return level().getGameTime() - entityData.get(STARTED) + partial; }
    private void action(Action value) { entityData.set(ACTION, value.ordinal()); entityData.set(STARTED, level().getGameTime()); }
    public boolean busy() { return action() != Action.IDLE; }
    public void begin(ServerPlayer player, BlockPos nest, LivingEntity target, boolean breath) {
        boolean alreadyPresent = owner != null;
        owner = player.getUUID(); this.nest = nest.immutable();
        hit.clear();
        if (breath) {
            arrival = player.position();
            arrivalYaw = player.getYRot();
            nextAction = Action.BREATH;
        } else {
            prepareCharge(player, target);
        }
        if (alreadyPresent) depart(); else appear();
    }
    private void prepareCharge(ServerPlayer player, LivingEntity target) {
        var area = ImaginaryDragonNestBlockEntity.area(player);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight()/2, 0);
        Vec3 center = new Vec3(Math.clamp(targetCenter.x, area.minX+0.01, area.maxX-0.01),
                Math.clamp(targetCenter.y, area.minY, area.maxY), Math.clamp(targetCenter.z, area.minZ+0.01, area.maxZ-0.01));
        double angle = random.nextDouble()*Math.PI*2;
        Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double back = edgeDistance(area, center, direction.scale(-1));
        double forward = edgeDistance(area, center, direction);
        arrival = center.subtract(direction.scale(back)); destination = center.add(direction.scale(forward));
        arrivalYaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        nextAction = Action.CHARGE;
    }
    private void appear() {
        setPos(arrival); setYRot(arrivalYaw); setDeltaMovement(Vec3.ZERO);
        action(Action.TELEPORT_IN);
    }
    private void depart() {
        // The ascent is authored on the model. Start the departure at its visible position,
        // rather than snapping the dragon back down to the attack's ground-level anchor.
        if (action() == Action.BREATH) {
            float seconds = breathAnimationSeconds(actionTicks(0));
            setPos(position().add(breathOffset(seconds).yRot((float)Math.toRadians(180-getYRot()))));
            setYRot(getYRot() - 720*smooth(seconds, 0.6F, 4));
        }
        setDeltaMovement(Vec3.ZERO);
        action(Action.TELEPORT_OUT);
    }
    public void dismiss() {
        if (dismissing) return;
        dismissing = true;
        // Finish emerging before retreating; never pop a half-visible dragon out of existence.
        if (action() != Action.TELEPORT_IN && action() != Action.TELEPORT_OUT) depart();
    }
    /** Share one timeline with damage: 3s ascent/charge, 5s fire, 1s recovery. */
    public static float breathAnimationSeconds(float ticks) {
        if (ticks <= BREATH_WINDUP_TICKS)
            return Math.max(0, ticks)*BREATH_FIRE_START_SECONDS/BREATH_WINDUP_TICKS;
        float fireTicks = ticks-BREATH_WINDUP_TICKS;
        if (fireTicks <= BREATH_DAMAGE_TICKS)
            return BREATH_FIRE_START_SECONDS + fireTicks*(BREATH_FIRE_END_SECONDS-BREATH_FIRE_START_SECONDS)/BREATH_DAMAGE_TICKS;
        float recovery = Math.min(1, (fireTicks-BREATH_DAMAGE_TICKS)/BREATH_RECOVERY_TICKS);
        return BREATH_FIRE_END_SECONDS + recovery*(BREATH_END_SECONDS-BREATH_FIRE_END_SECONDS);
    }
    private static float smooth(float time, float start, float end) {
        float t = Math.clamp((time-start)/(end-start), 0, 1);
        return t*t*(3-2*t);
    }
    private static Vec3 breathOffset(float seconds) {
        // breath_motion path from the supplied breath_ultimate clip, in model units.
        if (seconds <= 4) {
            float rise = smooth(seconds, 0.6F, 4);
            double angle = Math.PI*4*rise;
            return new Vec3(88*(Math.cos(angle)-1), 448*rise, -88*Math.sin(angle)).scale(1.0/16);
        }
        if (seconds < 4.8F) {
            float brace = smooth(seconds, 4, 4.8F);
            return new Vec3(0, 448+8*Math.sin(Math.PI*brace), -30*brace).scale(1.0/16);
        }
        float recovery = smooth(seconds, 9, 10);
        return new Vec3(0, 448+2*Math.sin((seconds-4.8)*Math.PI*1.4)*(1-recovery)-8*recovery,
                -30-80*recovery).scale(1.0/16);
    }
    private static double edgeDistance(AABB box, Vec3 p, Vec3 direction) {
        double x = Math.abs(direction.x) < 1e-8 ? Double.POSITIVE_INFINITY : ((direction.x > 0 ? box.maxX : box.minX)-p.x)/direction.x;
        double z = Math.abs(direction.z) < 1e-8 ? Double.POSITIVE_INFINITY : ((direction.z > 0 ? box.maxZ : box.minZ)-p.z)/direction.z;
        return Math.max(0, Math.min(x, z));
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        var player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (!dismissing && (player == null || !player.isAlive() || player.level() != level() || !EnderCompanions.evolved(player)
                || !server.hasChunkAt(nest) || !(server.getBlockEntity(nest) instanceof ImaginaryDragonNestBlockEntity home) || !home.owns(this))) dismiss();
        switch (action()) {
            case TELEPORT_IN -> {
                if (actionTicks(0) >= APPEAR_TICKS) {
                    if (dismissing) depart(); else action(nextAction);
                }
            }
            case TELEPORT_OUT -> {
                if (actionTicks(0) >= DEPART_TICKS) {
                    if (dismissing) discard(); else appear();
                }
            }
            case CHARGE -> charge(server, player);
            case BREATH -> breath(server, player);
            case FALL -> {
                setDeltaMovement(getDeltaMovement().add(0, -0.08, 0).scale(0.98)); move(MoverType.SELF, getDeltaMovement());
                if (onGround()) { setDeltaMovement(Vec3.ZERO); action(Action.IDLE); }
                if (getY() < level().getMinBuildHeight()-16 || actionTicks(0) > 400) discard();
            }
            case IDLE -> { }
        }
    }
    private void charge(ServerLevel server, ServerPlayer player) {
        Vec3 from = position();
        double distance = from.distanceTo(destination);
        Vec3 step = destination.subtract(from).normalize().scale(Math.min(1.5, distance));
        Vec3 to = from.add(step);
        setDeltaMovement(step);
        for (var target : server.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(2), e -> ImaginaryDragonNestBlockEntity.hostile(e, player))) {
            var bounds = target.getBoundingBox().inflate(1.5);
            if (!hit.contains(target.getUUID()) && (bounds.contains(from) || bounds.clip(from, to).isPresent())) {
                hit.add(target.getUUID()); CombatDamage.hit(player, target, 100);
            }
        }
        setPos(to); hasImpulse = true;
        if (distance > 1.5) return;
        for (int attempt = 0; attempt < 48; attempt++) {
            Vec3 spawn = new Vec3(player.getX()+random.nextDouble()*20-10, 100, player.getZ()+random.nextDouble()*20-10);
            if (!server.hasChunkAt(BlockPos.containing(spawn)) || !server.getWorldBorder().isWithinBounds(BlockPos.containing(spawn))
                    || server.isOutsideBuildHeight(BlockPos.containing(spawn)) || !server.noCollision(getType().getDimensions().makeBoundingBox(spawn))) continue;
            arrival = spawn; arrivalYaw = getYRot(); nextAction = Action.FALL; depart(); return;
        }
        dismiss();
    }
    private void breath(ServerLevel server, ServerPlayer player) {
        int elapsed = (int)actionTicks(0);
        int fireTicks = elapsed-BREATH_WINDUP_TICKS;
        // Hit immediately when fire starts, then once per second (five hits total).
        if (fireTicks >= 0 && fireTicks < BREATH_DAMAGE_TICKS && fireTicks % 20 == 0) {
            var area = ImaginaryDragonNestBlockEntity.area(player);
            for (var target : server.getEntitiesOfClass(LivingEntity.class, area, e -> ImaginaryDragonNestBlockEntity.hostile(e, player)))
                CombatDamage.hit(player, target, server.damageSources().indirectMagic(this, player), 50);
            server.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY()+1, player.getZ(), 160, 10, 8, 10, 0.03);
        }
        if (elapsed >= BREATH_WINDUP_TICKS+BREATH_DAMAGE_TICKS+BREATH_RECOVERY_TICKS) dismiss();
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
