package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyCapabilities;

/** Generates energy directly at one configured, already-loaded destination. */
public final class SpatialTransmissionTowerBlockEntity extends BlockEntity {
    private GlobalPos target;
    public SpatialTransmissionTowerBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.SPATIAL_TRANSMISSION_TOWER.get(), pos, state); }
    public GlobalPos target() { return target; }
    public void setTarget(GlobalPos target) { this.target = target; setChanged(); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, SpatialTransmissionTowerBlockEntity tower) {
        if (tower.target == null || !(level instanceof ServerLevel server)) return;
        ServerLevel destination = server.getServer().getLevel(tower.target.dimension());
        if (destination == null || !destination.hasChunkAt(tower.target.pos())) return;
        // Prefer native IE. Probe faces only until the first usable interface; never multiply the budget per side.
        boolean supported = false;
        for (Direction side : Direction.values()) {
            var energy = destination.getCapability(LyyCapabilities.IMAGINARY_ENERGY, tower.target.pos(), side);
            supported |= energy != null;
            if (energy != null && energy.canReceiveImaginaryEnergy()) {
                energy.receiveImaginaryEnergy(10000, false);
                return;
            }
        }
        for (Direction side : Direction.values()) {
            var energy = destination.getCapability(Capabilities.EnergyStorage.BLOCK, tower.target.pos(), side);
            supported |= energy != null;
            if (energy != null && energy.canReceive()) { energy.receiveEnergy(500000, false); return; }
        }
        if (!supported) tower.setTarget(null);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (target != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().ifPresent(value -> tag.put("Target", value));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        target = tag.contains("Target") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Target")).result().orElse(null) : null;
    }
}
