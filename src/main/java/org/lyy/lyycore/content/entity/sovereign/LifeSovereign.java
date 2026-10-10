package org.lyy.lyycore.content.entity.sovereign;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.lyy.lyycore.content.GateSummoning;
import org.lyy.lyycore.content.entity.AnimatedMonster;
import org.lyy.lyycore.registry.*;

/** Three explicit bodies share one encounter; action identity is independent of animation resource names. */
public final class LifeSovereign extends AnimatedMonster {
    private static final int SWORD_RELEASE_TICKS = 23, SWORD_ANIMATION_TICKS = 53, SWORD_COOLDOWN_TICKS = 200;
    public enum Phase { DEFENDER, COCOON, USURPER }
    private enum Action {
        IDLE("idle"), BIRTH("birth"), SLASH("wide_slash"), SWORD("charged_sword_throw"),
        FORM("form"), DORMANT("dormant"), EMERGE("phase_emerge"), DEFEAT("phase_defeat"),
        MISSILE(LifeSpell.Kind.MISSILE), SPIKE(LifeSpell.Kind.SPIKE), METEOR(LifeSpell.Kind.METEOR);
        final String clip;
        final LifeSpell.Kind spell;
        Action(String clip) { this.clip = clip; this.spell = null; }
        Action(LifeSpell.Kind spell) { this.clip = spell.animation; this.spell = spell; }
        static Action forSpell(LifeSpell.Kind kind) {
            return switch (kind) {
                case MISSILE -> MISSILE;
                case SPIKE -> SPIKE;
                case METEOR -> METEOR;
                default -> throw new IllegalArgumentException("Not a casting action: " + kind);
            };
        }
        /** Compatibility is resolved once when loading old saves, never in the AI hot path. */
        static Action fromClip(String clip) {
            for (var value : values()) if (value.clip.equals(clip)) return value;
            return IDLE;
        }
    }
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Map<UUID, Integer> hitCounts = new HashMap<>();
    private UUID encounter, crystal, castingTarget, swordTarget;
    private Action action = Action.IDLE;
    private int swordCooldown;
    private boolean swordReleased;
    private int decisionTicks, dashes, dashTicks, spellTicks, missingCrystalTicks;
    private boolean transitioned, threshold, alternating, spellReleased;
    public LifeSovereign(EntityType<? extends LifeSovereign> type, Level level) {
        super(type, level); setPersistenceRequired(); xpReward = phase() == Phase.USURPER ? 3000 : 0;
    }
    private void action(Action next) {
        action = next;
        if (next == Action.IDLE || next == Action.DORMANT) animate(next.clip);
        else restartAnimation(next.clip);
    }
    // Both gameplay and rendering keep using the same synchronized clock.
    private int elapsedTicks() { return Math.round(animationTime(0) * 20); }
    public Phase phase() {
        return getType() == LyyEntities.LIFE_COCOON.get() ? Phase.COCOON : getType() == LyyEntities.LIFE_USURPER.get() ? Phase.USURPER : Phase.DEFENDER;
    }
    public static AttributeSupplier.Builder attributes(Phase phase) {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, phase == Phase.USURPER ? 12000 : 6000)
                .add(Attributes.ARMOR, 100).add(Attributes.ARMOR_TOUGHNESS, 100)
                .add(Attributes.MOVEMENT_SPEED, .28).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 48);
    }
    public void begin(BlockPos altar) {
        encounter = LifeEncounters.get((ServerLevel)level()).begin((ServerLevel)level(), altar, getUUID());
        if (phase() == Phase.DEFENDER) action(Action.BIRTH);
    }
    public LifeEncounters.Battle battle() { return encounter == null || !(level() instanceof ServerLevel server) ? null : LifeEncounters.get(server).battle(encounter); }
    public boolean active() { var battle = battle(); return isAlive() && battle != null && battle.boss().equals(getUUID()); }
    @Override protected boolean isAlwaysExperienceDropper() { return phase() == Phase.USURPER; }
    @Override public boolean isPushable() { return false; }
    @Override protected AABB makeBoundingBox() {
        if (phase() != Phase.DEFENDER) return super.makeBoundingBox();
        return new AABB(getX()-1, getY(), getZ()-1.5, getX()+1, getY()+3, getZ()+1.5);
    }
    @Override protected void customServerAiStep() {
        var battle = battle();
        if (battle == null) { discard(); return; }
        if (swordCooldown > 0) swordCooldown--;
        var server = (ServerLevel)level();
        var players = battle.players(server);
        var target = players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(target);
        bar.setProgress(getHealth()/getMaxHealth());
        setNoGravity(phase() != Phase.DEFENDER);
        if (phase() != Phase.DEFENDER) { setDeltaMovement(Vec3.ZERO); getNavigation().stop(); }
        if (phase() == Phase.COCOON) { cocoon(server, target); return; }
        if (phase() == Phase.DEFENDER && action == Action.BIRTH && elapsedTicks() < 56) {
            getNavigation().stop(); setDeltaMovement(Vec3.ZERO); return;
        }
        if (action == Action.BIRTH) action(Action.IDLE);
        if (phase() == Phase.USURPER) {
            if (action == Action.EMERGE && elapsedTicks() < 48) return;
            if (!threshold && getHealth() < 4000) applyThreshold(players);
            if (casting(server)) { decisionTicks++; return; }
            action(Action.IDLE);
        }
        if (phase() == Phase.DEFENDER && tickCount%20 == 0) heal(80);
        if (phase() == Phase.DEFENDER && throwingSword(server, players)) return;
        if (target == null) { getNavigation().stop(); return; }
        getLookControl().setLookAt(target, 360, 360);
        var offset = target.position().subtract(position());
        setYRot((float)Math.toDegrees(Math.atan2(-offset.x, offset.z))); yBodyRot = yHeadRot = getYRot();
        if (phase() == Phase.DEFENDER) defender(target, battle);
        else if (++decisionTicks >= 60) {
            decisionTicks = 0;
            var kind = switch (random.nextInt(3)) { case 0 -> LifeSpell.Kind.MISSILE; case 1 -> LifeSpell.Kind.SPIKE; default -> LifeSpell.Kind.METEOR; };
            startCasting(target, kind);
        }
    }
    private void startCasting(ServerPlayer target, LifeSpell.Kind kind) {
        castingTarget = target.getUUID(); spellReleased = kind != LifeSpell.Kind.MISSILE;
        action(Action.forSpell(kind));
        if (spellReleased) LifeSpell.launch(this, target, kind, null);
    }
    private boolean casting(ServerLevel server) {
        var kind = action.spell;
        if (kind == null) return false;
        int elapsed = elapsedTicks();
        if (elapsed >= kind.animationTicks) return false;
        if (!spellReleased && elapsed >= kind.impactTick) {
            spellReleased = true;
            if (castingTarget != null && server.getPlayerByUUID(castingTarget) instanceof ServerPlayer target && target.isAlive())
                LifeSpell.launch(this, target, kind, null);
        }
        return true;
    }
    private void defender(ServerPlayer target, LifeEncounters.Battle battle) {
        decisionTicks++;
        if (action == Action.SLASH && elapsedTicks() < 33) {
            getNavigation().stop();
            if (elapsedTicks() == 12 && distanceToSqr(target) <= 16) target.hurt(damageSources().mobAttack(this), 80);
            return;
        }
        action(Action.IDLE);
        if (distanceToSqr(target) > 4) getNavigation().moveTo(target, 1); else getNavigation().stop();
        if (dashes > 0 && ++dashTicks >= 5) {
            dashTicks = 0; dashes--;
            var offset = target.position().subtract(position());
            var step = new Vec3(Math.signum(offset.x)*random.nextDouble()*Math.min(2, Math.abs(offset.x)),
                    Math.signum(offset.y)*random.nextDouble()*Math.min(2, Math.abs(offset.y)),
                    Math.signum(offset.z)*random.nextDouble()*Math.min(2, Math.abs(offset.z)));
            var next = position().add(step);
            if (battle.bounds().contains(next) && level().noCollision(this, getBoundingBox().move(step))) teleportTo(next.x, next.y, next.z);
        }
        if (decisionTicks >= 40) {
            decisionTicks = 0;
            if (swordCooldown == 0 && random.nextInt(3) == 0) {
                swordTarget = target.getUUID(); swordReleased = false;
                swordCooldown = SWORD_COOLDOWN_TICKS;
                dashes = dashTicks = 0;
                getNavigation().stop();
                action(Action.SWORD);
            } else if (distanceToSqr(target) <= 16 && random.nextBoolean()) action(Action.SLASH);
            else { dashes = 4; dashTicks = 0; }
        }
    }
    private boolean throwingSword(ServerLevel server, List<ServerPlayer> players) {
        if (action != Action.SWORD) return false;
        int elapsed = elapsedTicks();
        if (elapsed >= SWORD_ANIMATION_TICKS) {
            swordTarget = null; action(Action.IDLE); return false;
        }
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        var target = swordTarget == null ? null : server.getPlayerByUUID(swordTarget);
        if (!swordReleased && target != null && target.isAlive() && !target.isSpectator()) {
            var offset = target.position().subtract(position());
            setYRot((float)Math.toDegrees(Math.atan2(-offset.x, offset.z)));
            yBodyRot = yHeadRot = getYRot();
        }
        if (!swordReleased && elapsed >= SWORD_RELEASE_TICKS) {
            swordReleased = true;
            if (target instanceof ServerPlayer player && players.contains(player)) DefenderSword.launch(this, player);
        }
        return true;
    }
    private void cocoon(ServerLevel server, ServerPlayer target) {
        if (action != Action.FORM || elapsedTicks() >= 44) action(Action.DORMANT);
        if (crystal != null && server.getEntity(crystal) != null) missingCrystalTicks = 0;
        else if (target != null && (crystal == null || ++missingCrystalTicks >= 20)) {
            crystal = LifeSpell.launch(this, target, LifeSpell.Kind.CRYSTAL, null); missingCrystalTicks = 0;
        }
        if (target == null || ++spellTicks < 100) return;
        spellTicks = 0; alternating = !alternating;
        if (alternating) {
            for (var direction : net.minecraft.core.Direction.values()) LifeSpell.launch(this, target, LifeSpell.Kind.MISSILE, Vec3.atLowerCornerOf(direction.getNormal()), false);
        } else for (int i = 0; i < 5; i++) LifeSpell.launch(this, target, LifeSpell.Kind.MISSILE,
                target.getEyePosition().subtract(getEyePosition()).normalize().yRot((i-2)*.18F));
    }
    public void crystalFinished(UUID id) { if (id.equals(crystal) && phase() == Phase.COCOON) changePhase(LyyEntities.LIFE_USURPER.get()); }
    private void applyThreshold(List<ServerPlayer> players) {
        threshold = true;
        for (var player : players) {
            float health = player.getMaxHealth();
            if (health >= 16) {
                LifeEncounters.curse(player, 2); player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12000, 1));
            } else if (health > 8) player.setHealth(player.getMaxHealth());
            else {
                LifeEncounters.reduceCurse(player, 2);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12000, 1));
            }
        }
    }
    public boolean magicHit(ServerPlayer target, float amount) {
        boolean hit = target.hurt(damageSources().indirectMagic(this, this), amount);
        if (hit && phase() == Phase.USURPER) {
            int count = hitCounts.getOrDefault(target.getUUID(), 0)+1;
            if (count >= 3) {
                hitCounts.remove(target.getUUID());
                int cooldown = target.invulnerableTime;
                try { target.invulnerableTime = 0; target.hurt(damageSources().indirectMagic(this, this), 60); }
                finally { target.invulnerableTime = cooldown; }
                heal(3000);
            } else hitCounts.put(target.getUUID(), count);
        }
        return hit;
    }
    private void changePhase(EntityType<LifeSovereign> type) {
        var server = (ServerLevel)level(); var battle = battle();
        if (battle == null) return;
        var next = type.create(server); if (next == null) return;
        next.encounter = encounter; next.moveTo(Vec3.atBottomCenterOf(battle.altar.above()));
        next.action(type == LyyEntities.LIFE_COCOON.get() ? Action.FORM : Action.EMERGE);
        if (!server.addFreshEntity(next)) return;
        transitioned = true;
        LifeEncounters.get(server).transition(encounter, next.getUUID());
        GateSummoning.replaceActiveBoss(server, battle.altar, getUUID(), next.getUUID());
        discard();
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (phase() == Phase.COCOON) return false;
        return super.hurt(source, amount);
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (!dead || !(level() instanceof ServerLevel server) || transitioned) return;
        if (phase() == Phase.DEFENDER) { action(Action.DEFEAT); bar.setProgress(0); return; }
        if (phase() != Phase.USURPER) return;
        var battle = battle();
        spawnAtLocation(LyyItems.ENDLESS_EROSION.get());
        if (battle != null && battle.players(server).stream().anyMatch(p -> p.getMaxHealth() <= 8)) spawnAtLocation(LyyItems.WHISPER_OF_THE_PAST.get());
        LifeEncounters.get(server).end(server, encounter, true);
    }
    @Override protected void tickDeath() {
        if (phase() != Phase.DEFENDER) { super.tickDeath(); return; }
        if (++deathTime >= 44 && !level().isClientSide) changePhase(LyyEntities.LIFE_COCOON.get());
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !transitioned && encounter != null && level() instanceof ServerLevel server) LifeEncounters.get(server).end(server, encounter, false);
        bar.removeAllPlayers(); super.remove(reason);
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bar.removePlayer(player); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (encounter != null) tag.putUUID("Encounter", encounter); if (crystal != null) tag.putUUID("Crystal", crystal);
        if (castingTarget != null) tag.putUUID("CastingTarget", castingTarget);
        if (swordTarget != null) tag.putUUID("SwordTarget", swordTarget);
        tag.putString("CombatAction", action.name());
        tag.putInt("SwordCooldown", swordCooldown); tag.putBoolean("SwordReleased", swordReleased);
        tag.putBoolean("SpellReleased", spellReleased);
        tag.putBoolean("Threshold", threshold); tag.putBoolean("Alternating", alternating);
        tag.putInt("DecisionTicks", decisionTicks); tag.putInt("SpellTicks", spellTicks); tag.putInt("Dashes", dashes); tag.putInt("DashTicks", dashTicks);
        var hits = new ListTag(); hitCounts.forEach((id, count) -> { var h = new CompoundTag(); h.putUUID("Player", id); h.putInt("Count", count); hits.add(h); }); tag.put("Hits", hits);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); encounter = tag.hasUUID("Encounter") ? tag.getUUID("Encounter") : null;
        crystal = tag.hasUUID("Crystal") ? tag.getUUID("Crystal") : null;
        castingTarget = tag.hasUUID("CastingTarget") ? tag.getUUID("CastingTarget") : null;
        swordTarget = tag.hasUUID("SwordTarget") ? tag.getUUID("SwordTarget") : null;
        action = Action.fromClip(animation());
        if (tag.contains("CombatAction")) {
            try { action = Action.valueOf(tag.getString("CombatAction")); }
            catch (IllegalArgumentException ignored) { /* Keep the migrated animation state. */ }
        }
        swordCooldown = Math.clamp(tag.getInt("SwordCooldown"), 0, SWORD_COOLDOWN_TICKS);
        swordReleased = tag.getBoolean("SwordReleased");
        spellReleased = tag.getBoolean("SpellReleased");
        threshold = tag.getBoolean("Threshold"); alternating = tag.getBoolean("Alternating");
        decisionTicks = tag.getInt("DecisionTicks"); spellTicks = tag.getInt("SpellTicks"); dashes = tag.getInt("Dashes"); dashTicks = tag.getInt("DashTicks");
        hitCounts.clear(); for (var raw : tag.getList("Hits", Tag.TAG_COMPOUND)) { var h = (CompoundTag)raw; hitCounts.put(h.getUUID("Player"), h.getInt("Count")); }
    }
}
