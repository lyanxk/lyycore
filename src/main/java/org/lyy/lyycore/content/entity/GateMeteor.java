package org.lyy.lyycore.content.entity;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lyy.lyycore.content.blocks.ImaginaryGateBlock;

/** A saved, fixed diagonal approach; only the gate footprint is changed on impact. */
public final class GateMeteor extends Entity {
    private static final EntityDataAccessor<Vector3f> FLIGHT_DIRECTION =
            SynchedEntityData.defineId(GateMeteor.class, EntityDataSerializers.VECTOR3);
    private BlockPos gate = BlockPos.ZERO;
    private Vec3 start = Vec3.ZERO;
    private int elapsed;
    public GateMeteor(EntityType<? extends GateMeteor> type, Level level) { super(type, level); }
    public void aim(BlockPos gate) {
        this.gate = gate.immutable();
        double height = Math.max(4, Math.min(40, level().getMaxBuildHeight()-gate.getY()-3));
        int x = random.nextBoolean() ? 1 : -1, z = random.nextBoolean() ? 1 : -1;
        start = gate.getCenter().add(x*height/Math.sqrt(2), height, z*height/Math.sqrt(2));
        setPos(start);
        syncFlightDirection();
    }
    public Vec3 flightDirection() { return new Vec3(entityData.get(FLIGHT_DIRECTION)); }
    private void syncFlightDirection() {
        entityData.set(FLIGHT_DIRECTION, gate.getCenter().subtract(start).normalize().toVector3f());
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FLIGHT_DIRECTION, new Vector3f(0, -1, 0));
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (!server.hasChunkAt(gate)) return;
        elapsed++;
        setPos(start.lerp(gate.getCenter(), Math.min(1, elapsed/40.0)));
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 10, 0.6, 0.6, 0.6, 0.04);
        if (elapsed < 40) return;
        var state = server.getBlockState(gate);
        if (state.getBlock() instanceof ImaginaryGateBlock && ImaginaryGateBlock.isController(state)) {
            var facing = state.getValue(ImaginaryGateBlock.FACING);
            server.removeBlock(gate, false);
            for (int column = 0; column < 5; column++) {
                var pos = ImaginaryGateBlock.partPos(gate, facing, column, 0);
                if (server.getBlockState(pos).isAir()) server.setBlock(pos, Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
            }
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, gate.getX()+0.5, gate.getY()+1, gate.getZ()+0.5, 1, 0, 0, 0, 0);
            server.playSound(null, gate, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 2, 0.7F);
        }
        discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("Gate", gate.asLong()); tag.putDouble("StartX", start.x); tag.putDouble("StartY", start.y); tag.putDouble("StartZ", start.z); tag.putInt("Elapsed", elapsed);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        gate = BlockPos.of(tag.getLong("Gate")); start = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ")); elapsed = tag.getInt("Elapsed");
        syncFlightDirection();
    }
}
