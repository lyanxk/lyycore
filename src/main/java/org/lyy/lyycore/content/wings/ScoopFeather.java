package org.lyy.lyycore.content.wings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.CombatDamage;

/** One representative feather; the client renders all 48 feathers with the original animation. */
public final class ScoopFeather extends Entity {
    public static final float SIZE = 1.5F;
    private ServerPlayer owner;
    private WingsScoop.Attack attack;
    private int path, previousAge;

    public ScoopFeather(EntityType<? extends ScoopFeather> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    void prepare(ServerPlayer owner, WingsScoop.Attack attack, int path) {
        this.owner = owner;
        this.attack = attack;
        this.path = path;
        moveCenter(ScoopPaths.position(path, 0, owner.position(), owner.getYRot()));
    }

    private void moveCenter(Vec3 center) { setPos(center.x, center.y - SIZE / 2, center.z); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (owner == null || attack == null || owner.isRemoved() || !WingsScoop.current(owner, attack)) { discard(); return; }
        int age = owner.tickCount - attack.started();
        if (age >= WingsScoop.DURATION) { discard(); return; }
        if (age <= previousAge) return;
        Vec3 start = getBoundingBox().getCenter();
        Vec3 end = ScoopPaths.position(path, age, owner.position(), owner.getYRot());
        if (age >= 2 && age <= 33) strike(start, end);
        moveCenter(end);
        previousAge = age;
    }

    private void strike(Vec3 start, Vec3 end) {
        double radius = SIZE / 2;
        for (var target : level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(radius),
                entity -> entity != owner && entity.isAlive() && !entity.isSpectator())) {
            if (attack.hit().contains(target.getUUID())) continue;
            var bounds = target.getBoundingBox().inflate(radius);
            if (!bounds.contains(start) && !bounds.contains(end) && bounds.clip(start, end).isEmpty()) continue;
            attack.hit().add(target.getUUID());
            if (CombatDamage.hit(owner, target, 40) && target.isAlive()) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, 1, 0));
                target.hurtMarked = true;
            }
        }
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
}
