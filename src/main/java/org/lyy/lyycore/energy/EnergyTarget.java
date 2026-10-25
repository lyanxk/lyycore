package org.lyy.lyycore.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.registry.LyyCapabilities;

/** Cached capability views of one receiver, with one independent budget across all faces. */
public final class EnergyTarget {
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final BooleanSupplier valid;
    private final List<BlockCapabilityCache<ImaginaryEnergy, Direction>> ieCaches = new ArrayList<>();
    private final List<BlockCapabilityCache<IEnergyStorage, Direction>> feCaches = new ArrayList<>();
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    private ServerLevel cachedLevel;
    private BlockPos cachedReceiver;
    private int generation;

    public EnergyTarget(BooleanSupplier valid) { this.valid = valid; }

    public void clear() {
        generation++;
        cachedLevel = null;
        cachedReceiver = null;
        ieCaches.clear();
        feCaches.clear();
        visited.clear();
    }

    private void prepare(ServerLevel level, BlockPos receiver) {
        if (cachedLevel == level && receiver.equals(cachedReceiver)) return;
        clear();
        cachedLevel = level;
        cachedReceiver = receiver.immutable();
        int currentGeneration = generation;
        for (Direction side : SIDES) {
            ieCaches.add(BlockCapabilityCache.create(LyyCapabilities.IMAGINARY_ENERGY, level, receiver, side,
                    () -> valid.getAsBoolean() && generation == currentGeneration, () -> {}));
            feCaches.add(BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, receiver, side,
                    () -> valid.getAsBoolean() && generation == currentGeneration, () -> {}));
        }
    }

    /** False means a loaded receiver no longer exposes IE or FE and can be unbound. */
    public boolean supply(ServerLevel level, BlockPos receiver, int iePerTick, int fePerTick) {
        prepare(level, receiver);
        boolean supported = false;
        int remaining = iePerTick;
        visited.clear();
        for (var cache : ieCaches) {
            var energy = cache.getCapability();
            supported |= energy != null;
            if (energy != null && visited.add(energy) && energy.canReceiveImaginaryEnergy()) {
                remaining -= Math.clamp(energy.receiveImaginaryEnergy(remaining, false), 0, remaining);
                if (remaining == 0) return true;
            }
        }
        if (remaining < iePerTick) return true;
        remaining = fePerTick;
        visited.clear();
        for (var cache : feCaches) {
            var energy = cache.getCapability();
            supported |= energy != null;
            if (energy != null && visited.add(energy) && energy.canReceive()) {
                remaining -= Math.clamp(energy.receiveEnergy(remaining, false), 0, remaining);
                if (remaining == 0) return true;
            }
        }
        return supported;
    }
}
