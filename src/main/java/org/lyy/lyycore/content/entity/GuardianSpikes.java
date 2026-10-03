package org.lyy.lyycore.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class GuardianSpikes extends Entity {
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(GuardianSpikes.class, EntityDataSerializers.INT);
    private UUID owner;
    public GuardianSpikes(EntityType<? extends GuardianSpikes> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(DELAY, 20); }
    public void configure(ImaginaryGuardian guardian, int delay) { owner = guardian.getUUID(); entityData.set(DELAY, delay); }
    public int delay() { return entityData.get(DELAY); }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        Entity attacker = owner == null ? null : server.getEntity(owner);
        if (!(attacker instanceof ImaginaryGuardian guardian) || !guardian.isAlive()) { discard(); return; }
        if (tickCount == delay()) {
            playSound(SoundEvents.AMETHYST_CLUSTER_PLACE, 1, 0.6F);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3, 0, 0.3),
                    entity -> entity.isAlive() && entity != guardian && !guardian.isAlliedTo(entity))) {
                target.hurt(damageSources().indirectMagic(this, guardian), 10);
            }
        }
        if (tickCount > delay() + 20) discard();
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
