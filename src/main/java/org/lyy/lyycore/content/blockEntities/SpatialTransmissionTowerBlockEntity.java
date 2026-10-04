package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.energy.ImaginaryEnergy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyCapabilities;

/** Generates energy directly at one configured, already-loaded destination. */
public final class SpatialTransmissionTowerBlockEntity extends BlockEntity {
    private GlobalPos target;
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final List<BlockCapabilityCache<ImaginaryEnergy, Direction>> ieCaches = new ArrayList<>();
    private final List<BlockCapabilityCache<IEnergyStorage, Direction>> feCaches = new ArrayList<>();
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    private ServerLevel cachedLevel;
    private int cacheGeneration;
    public SpatialTransmissionTowerBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.SPATIAL_TRANSMISSION_TOWER.get(), pos, state); }
    public GlobalPos target() { return target; }
    public void setTarget(GlobalPos target) { this.target = target; clearCaches(); setChanged(); }
    private void clearCaches() {
        cacheGeneration++;
        cachedLevel = null;
        ieCaches.clear(); feCaches.clear(); visited.clear();
    }
    private void prepareCaches(ServerLevel destination) {
        if (cachedLevel == destination) return;
        clearCaches();
        cachedLevel = destination;
        int generation = cacheGeneration;
        for (Direction side : SIDES) {
            ieCaches.add(BlockCapabilityCache.create(LyyCapabilities.IMAGINARY_ENERGY, destination, target.pos(), side,
                    () -> !isRemoved() && cacheGeneration == generation, () -> {}));
            feCaches.add(BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, destination, target.pos(), side,
                    () -> !isRemoved() && cacheGeneration == generation, () -> {}));
        }
    }
    @Override public void setRemoved() { clearCaches(); super.setRemoved(); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, SpatialTransmissionTowerBlockEntity tower) {
        if (tower.target == null || !(level instanceof ServerLevel server)) return;
        ServerLevel destination = server.getServer().getLevel(tower.target.dimension());
        if (destination == null || !destination.hasChunkAt(tower.target.pos())) return;
        tower.prepareCaches(destination);
        // All faces (and the unsided interface) share one budget. A full face is not a successful transfer.
        boolean supported = false;
        int remaining = 10000;
        tower.visited.clear();
        for (var cache : tower.ieCaches) {
            var energy = cache.getCapability();
            supported |= energy != null;
            if (energy != null && tower.visited.add(energy) && energy.canReceiveImaginaryEnergy()) {
                remaining -= Math.clamp(energy.receiveImaginaryEnergy(remaining, false), 0, remaining);
                if (remaining == 0) return;
            }
        }
        // Do not generate both IE and FE in the same tick; FE is the fallback when native IE accepted nothing.
        if (remaining < 10000) return;
        remaining = 500000;
        tower.visited.clear();
        for (var cache : tower.feCaches) {
            var energy = cache.getCapability();
            supported |= energy != null;
            if (energy != null && tower.visited.add(energy) && energy.canReceive()) {
                remaining -= Math.clamp(energy.receiveEnergy(remaining, false), 0, remaining);
                if (remaining == 0) return;
            }
        }
        if (!supported) tower.setTarget(null);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (target != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().ifPresent(value -> tag.put("Target", value));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        clearCaches();
        target = tag.contains("Target") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Target")).result().orElse(null) : null;
    }
}
