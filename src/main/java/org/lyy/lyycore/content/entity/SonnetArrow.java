package org.lyy.lyycore.content.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lyy.lyycore.registry.LyyEffects;

public class SonnetArrow extends Arrow {
    public static final int LIFETIME = 160;
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(SonnetArrow.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(1, 0.48F, 0.78F), 0.5F);
    private final Set<UUID> hitEntities = new HashSet<>();
    private long expiresAt;
    private UUID pendingDome;

    public SonnetArrow(EntityType<? extends SonnetArrow> type, Level level) {
        super(type, level);
        expiresAt = level.getGameTime() + LIFETIME;
        pickup = Pickup.DISALLOWED;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGED, false);
    }

    public void setCharged(boolean charged) { entityData.set(CHARGED, charged); setNoGravity(charged); }
    public boolean isCharged() { return entityData.get(CHARGED); }

    public void launchDome(SonnetDome dome) {
        pendingDome = dome.getUUID();
        expiresAt = level().getGameTime() + 10;
        setCharged(true);
        setNoPhysics(true);
    }

    @Override public byte getPierceLevel() { return isCharged() && !isNoPhysics() ? Byte.MAX_VALUE : super.getPierceLevel(); }
    @Override protected boolean canHitEntity(Entity target) {
        return !isNoPhysics() && !hitEntities.contains(target.getUUID()) && super.canHitEntity(target);
    }

    @Override protected ProjectileDeflection hitTargetOrDeflectSelf(net.minecraft.world.phys.HitResult hit) {
        if (isCharged() && hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity) {
            onHitEntity(entityHit);
            return ProjectileDeflection.NONE;
        }
        return super.hitTargetOrDeflectSelf(hit);
    }

    @Override protected void onHitEntity(EntityHitResult result) {
        if (!isCharged()) {
            if (getOwner() instanceof net.minecraft.world.entity.player.Player) physicalHit(this, result, this::doPostHurtEffects, this::doKnockback);
            else super.onHitEntity(result);
            return;
        }
        Entity target = result.getEntity();
        hitEntities.add(target.getUUID());
        if (!level().isClientSide && target instanceof LivingEntity living) {
            if (getOwner() instanceof net.minecraft.world.entity.player.Player player)
                org.lyy.lyycore.content.SpecialDamage.hit(player, this, living, org.lyy.lyycore.content.SpecialDamage.Element.PHYSICAL, 20);
            else living.hurt(damageSources().arrow(this, getOwner() == null ? this : getOwner()), 20);
            living.addEffect(new MobEffectInstance(LyyEffects.CRYSTALLIZATION, 60), getOwner());
        }
    }

    @Override public void tick() {
        if (!level().isClientSide && pendingDome != null &&
                !(((ServerLevel) level()).getEntity(pendingDome) instanceof SonnetDome dome && dome.isValid())) {
            discard();
            return;
        }
        if (!level().isClientSide && level().getGameTime() >= expiresAt) {
            if (pendingDome != null && ((ServerLevel) level()).getEntity(pendingDome) instanceof SonnetDome dome) dome.open();
            discard();
            return;
        }
        Vec3 velocity = getDeltaMovement();
        super.tick();
        if (isCharged() && !inGround && !isRemoved()) setDeltaMovement(velocity);
        if (!inGround) sparkle(this);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("SonnetExpires", expiresAt);
        tag.putBoolean("SonnetCharged", isCharged());
        if (pendingDome != null) tag.putUUID("PendingDome", pendingDome);
        var hits = new net.minecraft.nbt.ListTag();
        for (UUID id : hitEntities) hits.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("SonnetHits", hits);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        expiresAt = tag.contains("SonnetExpires") ? tag.getLong("SonnetExpires") : level().getGameTime() + LIFETIME;
        setCharged(tag.getBoolean("SonnetCharged"));
        pendingDome = tag.hasUUID("PendingDome") ? tag.getUUID("PendingDome") : null;
        hitEntities.clear();
        for (var entry : tag.getList("SonnetHits", 8)) hitEntities.add(UUID.fromString(entry.getAsString()));
        pickup = Pickup.DISALLOWED;
    }

    /** Preserve normal arrow flight, impact knockback, enchantments and tipped/spectral effects. */
    private static void physicalHit(AbstractArrow arrow, EntityHitResult result,
                                    java.util.function.Consumer<LivingEntity> effects,
                                    java.util.function.BiConsumer<LivingEntity, net.minecraft.world.damagesource.DamageSource> knockback) {
        if (!(arrow.getOwner() instanceof net.minecraft.world.entity.player.Player owner) || arrow.level().isClientSide) return;
        Entity target = result.getEntity();
        var element = org.lyy.lyycore.content.SpecialDamage.Element.PHYSICAL;
        var source = org.lyy.lyycore.content.SpecialDamage.source(owner, arrow, element);
        float base = (float)arrow.getBaseDamage();
        if (arrow.getWeaponItem() != null && arrow.level() instanceof ServerLevel server)
            base = net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(server, arrow.getWeaponItem(), target, source, base);
        int damage = net.minecraft.util.Mth.ceil(Math.clamp(arrow.getDeltaMovement().length() * base, 0, Integer.MAX_VALUE));
        if (arrow.isCritArrow()) damage = (int)Math.min(Integer.MAX_VALUE, (long)damage + owner.getRandom().nextInt(damage / 2 + 2));
        owner.setLastHurtMob(target);
        int fire = target.getRemainingFireTicks();
        if (arrow.isOnFire() && target.getType() != EntityType.ENDERMAN) target.igniteForSeconds(5);
        if (target.hurt(source, org.lyy.lyycore.content.SpecialDamage.amount(owner, element, damage))) {
            if (target instanceof LivingEntity living) {
                living.setArrowCount(living.getArrowCount() + 1);
                knockback.accept(living, source);
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel)arrow.level(), living, source, arrow.getWeaponItem());
                effects.accept(living);
                if (living != owner && living instanceof net.minecraft.world.entity.player.Player && owner instanceof net.minecraft.server.level.ServerPlayer server && !arrow.isSilent())
                    server.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0));
            }
            arrow.playSound(net.minecraft.sounds.SoundEvents.ARROW_HIT, 1, 1.2F);
            arrow.discard();
        } else {
            target.setRemainingFireTicks(fire);
            arrow.deflect(ProjectileDeflection.REVERSE, target, owner, false);
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(.2));
            if (arrow.getDeltaMovement().lengthSqr() < 1.0E-7) arrow.discard();
        }
    }

    private static void sparkle(AbstractArrow arrow) {
        if (arrow.level().isClientSide && !arrow.isRemoved() && arrow.getDeltaMovement().lengthSqr() > 0.01)
            arrow.level().addParticle(DUST, arrow.getX(), arrow.getY(), arrow.getZ(), 0, 0, 0);
    }

    public static class Spectral extends SpectralArrow {
        private long expiresAt;
        @Override protected void onHitEntity(EntityHitResult result) {
            if (getOwner() instanceof net.minecraft.world.entity.player.Player) physicalHit(this, result, this::doPostHurtEffects, this::doKnockback);
            else super.onHitEntity(result);
        }
        public Spectral(EntityType<? extends Spectral> type, Level level) {
            super(type, level);
            expiresAt = level.getGameTime() + LIFETIME;
            pickup = Pickup.DISALLOWED;
        }
        @Override public void tick() {
            if (!level().isClientSide && level().getGameTime() >= expiresAt) { discard(); return; }
            super.tick();
            if (!inGround) sparkle(this);
        }
        @Override public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putLong("SonnetExpires", expiresAt);
        }
        @Override public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            expiresAt = tag.contains("SonnetExpires") ? tag.getLong("SonnetExpires") : level().getGameTime() + LIFETIME;
            pickup = Pickup.DISALLOWED;
        }
    }
}
