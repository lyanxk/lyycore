package org.lyy.lyycore.content.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Fast cosmetic ricochets; the dome applies one independent area hit per bounce. */
public class SonnetVolley extends Entity {
    public static final int BOUNCES = 32;
    public static final int BOUNCES_PER_TICK = 4;
    public static final float DAMAGE_PER_HIT = 20;
    public static final int FLIGHT_TICKS = (BOUNCES + BOUNCES_PER_TICK - 1) / BOUNCES_PER_TICK;
    public static final int TRAIL_TICKS = 3;
    private static final EntityDataAccessor<Vector3f> CENTER = SynchedEntityData.defineId(SonnetVolley.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> START = SynchedEntityData.defineId(SonnetVolley.class, EntityDataSerializers.VECTOR3);
    private UUID domeId;
    private UUID ownerId;
    private int age;
    private List<Vec3> cachedPath;
    private UUID cachedPathId;
    private Vec3 cachedCenter;
    private Vec3 cachedStart;
    public SonnetVolley(EntityType<? extends SonnetVolley> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CENTER, new Vector3f()); builder.define(START, new Vector3f());
    }
    public void prepare(SonnetDome dome, Player player) {
        domeId = dome.getUUID(); ownerId = player.getUUID();
        setPos(player.getEyePosition().add(player.getLookAngle().scale(0.6)));
        entityData.set(CENTER, dome.position().toVector3f());
        entityData.set(START, position().toVector3f());
    }
    public int getAge() { return age; }
    public List<Vec3> path() {
        Vec3 start = new Vec3(entityData.get(START));
        Vec3 center = new Vec3(entityData.get(CENTER));
        // Entity data can arrive after construction. Rebuild only when its inputs change,
        // never once per rendered frame; client and server still use the same UUID seed.
        if (cachedPath != null && getUUID().equals(cachedPathId)
                && start.equals(cachedStart) && center.equals(cachedCenter)) return cachedPath;
        var points = new ArrayList<Vec3>(BOUNCES + 1);
        points.add(start);
        var random = new java.util.Random(getUUID().getLeastSignificantBits());
        double angle = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < BOUNCES; i++) {
            angle += Math.PI * (0.72 + random.nextDouble() * 0.5);
            double height = 0.15 + random.nextDouble() * 0.7;
            double horizontal = Math.sqrt(1 - height * height);
            points.add(center.add(Math.cos(angle) * horizontal * SonnetDome.RADIUS,
                    height * SonnetDome.HEIGHT, Math.sin(angle) * horizontal * SonnetDome.RADIUS));
        }
        cachedPathId = getUUID(); cachedCenter = center; cachedStart = start;
        cachedPath = List.copyOf(points);
        return cachedPath;
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && age < FLIGHT_TICKS) {
            var server = (ServerLevel) level();
            if (!(server.getEntity(domeId) instanceof SonnetDome dome) || !dome.isActive()
                    || !(server.getPlayerByUUID(ownerId) instanceof Player owner)) { discard(); return; }
            int firstBounce = age * BOUNCES_PER_TICK;
            for (int bounce = firstBounce; bounce < Math.min(BOUNCES, firstBounce + BOUNCES_PER_TICK); bounce++)
                dome.damageEnemies(owner, this);
        }
        age++;
        if (age >= FLIGHT_TICKS + TRAIL_TICKS + 1) discard();
    }
    // Volleys are deliberately transient and not saved by their entity type.
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
}
