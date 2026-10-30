package org.lyy.lyycore.content.finality;

import net.minecraft.core.Holder;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class FinalitySummon extends Monster {
    public enum Kind {
        STONE(500000, 3),
        HEART(800000, 5),
        CRYSTAL(300000, 2),
        SWORD(700000, 4),
        BOOK(400000, 3),
        STAKE(100000, 0);
        public final int health, progress;

        Kind(int health, int progress) {
            this.health = health;
            this.progress = progress;
        }
    }

    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(FinalitySummon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_PLAYER =
            SynchedEntityData.defineId(FinalitySummon.class, EntityDataSerializers.INT);
    private UUID parent, target;
    private int lastAttack;

    public FinalitySummon(EntityType<? extends FinalitySummon> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10000)
                .add(Attributes.SCALE, 2)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.ARMOR_TOUGHNESS, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public double getAttributeValue(Holder<Attribute> attribute) {
        return attribute.equals(Attributes.MAX_HEALTH)
                ? getAttributeBaseValue(attribute)
                : super.getAttributeValue(attribute);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KIND, 0);
        b.define(TARGET_PLAYER, -1);
    }

    public Kind kind() {
        return Kind.values()[entityData.get(KIND)];
    }

    public int targetPlayerId() {
        return entityData.get(TARGET_PLAYER);
    }

    private void facePlayer(ServerPlayer player) {
        entityData.set(TARGET_PLAYER, player.getId());
    }

    public void setup(Kind kind) {
        entityData.set(KIND, kind.ordinal());
        getAttribute(Attributes.SCALE).setBaseValue(kind == Kind.STAKE ? 4 : 2);
        setCustomName(
                net.minecraft.network.chat.Component.translatable(
                        "entity.lyycore.finality."
                                + kind.name().toLowerCase(java.util.Locale.ROOT)));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.health);
        setHealth(getMaxHealth());
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        var trial = FinalityTrials.get(level);
        if (!trial.active()) {
            discard();
            return;
        }
        if (kind() == Kind.STAKE) {
            stake(level, trial);
            return;
        }
        var targets = trial.players();
        if (targets.isEmpty()) return;
        if (!targets.contains(level.getEntity(targetPlayerId()))) {
            var closest = targets.getFirst();
            for (var player : targets)
                if (distanceToSqr(player) < distanceToSqr(closest)) closest = player;
            facePlayer(closest);
        }
        if (kind() == Kind.HEART) {
            if (tickCount % 20 == 0) {
                for (var s : trial.summons) if (s != this && s.isAlive()) s.heal(30000);
                for (var w : trial.withers) if (w.isAlive()) w.heal(30000);
            }
            if (tickCount % 160 == 0) {
                var p = targets.get(random.nextInt(targets.size()));
                facePlayer(p);
                boolean exists =
                        trial.summons.stream()
                                .anyMatch(
                                        s ->
                                                getUUID().equals(s.parent)
                                                        && p.getUUID().equals(s.target));
                if (!exists) {
                    var stake = FinalityContent.SUMMON.get().create(level);
                    stake.setup(Kind.STAKE);
                    stake.parent = getUUID();
                    stake.target = p.getUUID();
                    stake.facePlayer(p);
                    double angle = random.nextDouble() * Math.PI * 2,
                            distance = 4 + random.nextDouble() * 2;
                    stake.setPos(
                            FinalityTrials.clamp(
                                    p.position()
                                            .add(
                                                    Math.cos(angle) * distance,
                                                    0,
                                                    Math.sin(angle) * distance),
                                    1.5));
                    trial.summons.add(stake);
                    level.addFreshEntity(stake);
                }
            }
        } else if (tickCount - lastAttack >= trial.interval()) {
            lastAttack = tickCount;
            var p = targets.get(random.nextInt(targets.size()));
            facePlayer(p);
            FinalityProjectile.cast(level, this, p);
        }
        if (kind() == Kind.SWORD
                && tickCount % 200 == 0
                && trial.withers.stream().filter(Entity::isAlive).count() < 8) {
            var wither = FinalityContent.WITHER.get().create(level);
            wither.setPos(FinalityTrials.clamp(position().add(0, 4, 0), 4));
            wither.setHealth(wither.getMaxHealth());
            trial.withers.add(wither);
            level.addFreshEntity(wither);
        }
    }

    private void stake(ServerLevel level, FinalityTrials trial) {
        if (parent == null
                || !(level.getEntity(parent) instanceof FinalitySummon heart)
                || !heart.isAlive()) {
            discard();
            return;
        }
        var p = target == null ? null : level.getServer().getPlayerList().getPlayer(target);
        if (p == null || p.level() != level || !p.isAlive()) return;
        facePlayer(p);
        Vec3 delta = position().subtract(p.position());
        double distance = delta.length();
        if (distance > 8) {
            p.setDeltaMovement(
                    p.getDeltaMovement().add(delta.scale(.005 * (distance - 8) / distance)));
            p.hurtMarked = true;
        }
        if (distance > 12) trial.addMark(p);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isDeadOrDying() && level() instanceof ServerLevel level)
            FinalityTrials.get(level).killed(this);
    }

    @Override
    public void dropAllDeathLoot(ServerLevel level, DamageSource source) {}
}
