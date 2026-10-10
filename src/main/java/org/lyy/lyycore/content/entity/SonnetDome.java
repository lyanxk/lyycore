package org.lyy.lyycore.content.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.item.SonnetBowItem;
import org.lyy.lyycore.registry.LyyEntities;

/** Lifetime and firing cadence are authoritative here, never on the item/client. */
public class SonnetDome extends Entity {
    public static final int DURATION = 300;
    public static final double RADIUS = 24;
    public static final double HEIGHT = RADIUS * 10.8 / 12.0;
    private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(SonnetDome.class, EntityDataSerializers.BOOLEAN);
    private UUID ownerId;
    private long opensAt;
    private long expiresAt;
    private long nextShotAt;

    public SonnetDome(EntityType<? extends SonnetDome> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(OPEN, false); }

    public void prepare(Player player) {
        ownerId = player.getUUID();
        setPos(player.position());
        opensAt = level().getGameTime() + 10;
        expiresAt = opensAt + DURATION;
    }

    public boolean isOpen() { return entityData.get(OPEN); }
    public boolean isValid() { return !isRemoved() && level().getGameTime() < expiresAt; }
    public boolean isActive() { return isValid() && isOpen(); }
    public boolean belongsTo(Player player) { return player.getUUID().equals(ownerId); }

    public void open() {
        if (level().isClientSide || isOpen() || level().getGameTime() >= expiresAt) return;
        entityData.set(OPEN, true);
        Player player = level().getPlayerByUUID(ownerId);
        if (player != null) {
            player.stopUsingItem();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof SonnetBowItem) SonnetBowItem.refreshBinding(stack, (ServerLevel) level());
            }
        }
    }

    public boolean recall(Player player) {
        if (level().isClientSide || !belongsTo(player) || isRemoved()) return false;
        // Revert every stack bound to this dome before removing the entity lookup.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (SonnetBowItem.resolveDome(stack, (ServerLevel) level()) == this) SonnetBowItem.revert(stack);
        }
        player.stopUsingItem();
        player.getCooldowns().removeCooldown(org.lyy.lyycore.registry.LyyItems.WHISPER_OF_THE_PAST.get());
        player.getPersistentData().remove("SonnetUltimateUntil");
        player.getPersistentData().remove("SonnetActiveDome");
        entityData.set(OPEN, false);
        discard();
        return true;
    }

    public boolean fire(Player player) {
        if (!isActive() || !belongsTo(player) || level().getGameTime() < nextShotAt) return false;
        SonnetVolley volley = LyyEntities.SONNET_VOLLEY.get().create(level());
        if (volley == null) return false;
        volley.prepare(this, player);
        if (!level().addFreshEntity(volley)) return false;
        nextShotAt = level().getGameTime() + SonnetBowItem.CRYSTAL_SHOT_INTERVAL;
        level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1, 1.6F);
        return true;
    }

    public boolean contains(Vec3 point) {
        Vec3 local = point.subtract(position());
        return local.y >= -1 && (local.x * local.x + local.z * local.z) / (RADIUS * RADIUS)
                + local.y * local.y / (HEIGHT * HEIGHT) <= 1;
    }

    public static boolean isEnemy(LivingEntity target, Player owner) {
        if (!target.isAlive() || target == owner || target.isAlliedTo(owner) || owner.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        return target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == owner
                || owner.getLastHurtByMob() == target || target.getLastHurtByMob() == owner;
    }

    public void damageEnemies(Player owner, Entity projectile) {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(position().add(-RADIUS, -1, -RADIUS), position().add(RADIUS, RADIUS, RADIUS)),
                target -> contains(target.position()) && isEnemy(target, owner))) {
            // Each bounce is a separate physical hit. Vanilla hurt cooldown must not merge the independent hits.
            int previous = target.invulnerableTime;
            target.invulnerableTime = 0;
            try { org.lyy.lyycore.content.SpecialDamage.hit(owner, projectile, target, org.lyy.lyycore.content.SpecialDamage.Element.PHYSICAL, SonnetVolley.DAMAGE_PER_HIT); }
            finally { target.invulnerableTime = previous; }
        }
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (level().getGameTime() >= expiresAt) { discard(); return; }
            if (!isOpen() && level().getGameTime() >= opensAt) open();
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putLong("OpensAt", opensAt);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putLong("NextShotAt", nextShotAt);
        tag.putBoolean("Open", isOpen());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        opensAt = tag.getLong("OpensAt"); expiresAt = tag.getLong("ExpiresAt"); nextShotAt = tag.getLong("NextShotAt");
        entityData.set(OPEN, tag.getBoolean("Open"));
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
}
