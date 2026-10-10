package org.lyy.lyycore.content.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.SpecialDamage;

/** The single delayed event resolves all sixteen hits, then the explosion animation finishes. */
public final class HailFlower extends Entity {
    private static final EntityDataAccessor<Long> CREATED = SynchedEntityData.defineId(HailFlower.class, EntityDataSerializers.LONG);
    private UUID owner;
    private boolean exploded;
    public HailFlower(EntityType<? extends HailFlower> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(CREATED, 0L); }
    public void prepare(Player player) { owner = player.getUUID(); setPos(player.position()); entityData.set(CREATED, level().getGameTime()); }
    public float age(float partial) { return level().getGameTime() - entityData.get(CREATED) + partial; }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!exploded && age(0) >= 20) {
            exploded = true;
            var player = owner == null ? null : level().getPlayerByUUID(owner);
            if (player != null) for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3),
                    entity -> entity != player && entity.isAlive() && entity.distanceToSqr(this) <= 9))
                SpecialDamage.burst(player, this, target, SpecialDamage.Element.ICE, 10, 16);
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 1.2F);
        }
        if (age(0) >= 48) discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
}
