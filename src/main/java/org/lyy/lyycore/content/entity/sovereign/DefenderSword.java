package org.lyy.lyycore.content.entity.sovereign;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEntities;

/** One straight, shield-piercing throw; it belongs to the current defender phase. */
public final class DefenderSword extends Entity {
    public static final float MODEL_SCALE = .9F;
    private static final double SPEED = 5.0 / 20;
    private static final double BLADE_LENGTH = 30.0 / 16 * MODEL_SCALE;
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.parse("lyycore:defender_sword"));
    private UUID owner;
    private int age;

    public DefenderSword(EntityType<? extends DefenderSword> type, Level level) { super(type, level); }

    public static void launch(LifeSovereign boss, ServerPlayer target) {
        var sword = LyyEntities.DEFENDER_SWORD.get().create(boss.level());
        if (sword == null) return;
        sword.owner = boss.getUUID();
        // Right-hand release transform from knight_attack_timing.json at 1.12 seconds.
        Vec3 hand = new Vec3(10.4, 27.046466830481275, -12.094011853898191)
                .scale(MODEL_SCALE / 16).yRot((float)Math.toRadians(180-boss.getYRot()));
        sword.setPos(boss.position().add(hand));
        sword.setDeltaMovement(target.getBoundingBox().getCenter().subtract(sword.position()).normalize().scale(SPEED));
        boss.level().addFreshEntity(sword);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (++age > 200 || owner == null || !(server.getEntity(owner) instanceof LifeSovereign boss)
                || !boss.active() || boss.phase() != LifeSovereign.Phase.DEFENDER) { discard(); return; }

        Vec3 from = position(), velocity = getDeltaMovement(), to = from.add(velocity);
        Vec3 tip = to.add(velocity.normalize().scale(BLADE_LENGTH));
        if (!server.hasChunkAt(BlockPos.containing(tip))) { discard(); return; }
        var block = server.clip(new ClipContext(from, tip, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = block.getLocation();
        ServerPlayer nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var player : boss.battle().players(server)) {
            var bounds = player.getBoundingBox().inflate(.2);
            var contact = bounds.contains(from) ? from : bounds.clip(from, end).orElse(null);
            if (contact != null && from.distanceToSqr(contact) < distance) {
                nearest = player;
                distance = from.distanceToSqr(contact);
            }
        }
        if (nearest != null) {
            nearest.hurt(damageSources().source(DAMAGE, this, boss), 80);
            discard();
        } else if (block.getType() != HitResult.Type.MISS) {
            discard();
        } else {
            setPos(to);
        }
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Age", age);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        age = Math.max(0, tag.getInt("Age"));
    }
}
