package org.lyy.lyycore.content.entity.guiding;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEntities;

/** A homing capture point: contact starts the boss's pull and mandatory devour action. */
public final class GuidingGrab extends Entity {
    private UUID bossId, targetId;
    private boolean fast;
    private int age;
    public GuidingGrab(EntityType<? extends GuidingGrab> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    /** Null means that no projectile was admitted to the world. */
    public static UUID launch(GuidingBoss boss, Player target, boolean fast) {
        var grab = LyyEntities.GUIDING_GRAB.get().create(boss.level());
        if (grab == null) return null;
        grab.bossId = boss.getUUID(); grab.targetId = target.getUUID(); grab.fast = fast;
        grab.setPos(boss.getEyePosition());
        return boss.level().addFreshEntity(grab) ? grab.getUUID() : null;
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (bossId == null || targetId == null || !(server.getEntity(bossId) instanceof GuidingBoss boss)
                || !boss.isAlive() || !boss.ownsGrab(getUUID())
                || !(server.getEntity(targetId) instanceof Player target) || !target.isAlive()) { discard(); return; }
        Vec3 start = position(), offset = target.getBoundingBox().getCenter().subtract(start);
        Vec3 end = start.add(offset.normalize().scale(Math.min(offset.length(), fast ? .5 : .1)));
        var bounds = target.getBoundingBox().inflate(.3);
        setPos(end);
        if (bounds.contains(start) || bounds.clip(start, end).isPresent()) {
            boss.captured(target); discard(); return;
        }
        if (!fast && ++age >= 200) { boss.grabMissed(getUUID()); discard(); }
    }
    @Override public void remove(RemovalReason reason) {
        // Permanent removal is an action failure, but ordinary chunk unload is not death.
        if (reason.shouldDestroy() && bossId != null && level() instanceof ServerLevel server
                && server.getEntity(bossId) instanceof GuidingBoss boss) boss.grabMissed(getUUID());
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (bossId != null) tag.putUUID("Boss", bossId);
        if (targetId != null) tag.putUUID("Target", targetId);
        tag.putBoolean("Fast", fast); tag.putInt("Age", age);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        bossId = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        fast = tag.getBoolean("Fast"); age = tag.getInt("Age");
    }
}
