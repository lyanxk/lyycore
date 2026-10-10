package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lyy.lyycore.energy.ImaginaryEnergy;
import org.lyy.lyycore.energy.EnergyReceiver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.lyy.lyycore.registry.LyyCapabilities;

/** Generates an independent energy budget at each configured, already-loaded destination. */
public class TargetedEnergySourceBlockEntity extends BlockEntity {
    private final int maxTargets, iePerTick, fePerTick;
    public int maxTargets() { return maxTargets; }
    public enum BindingResult { ADDED, REMOVED, LIMIT_REACHED }
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final List<Target> targets = new ArrayList<>();

    public TargetedEnergySourceBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int maxTargets, int iePerTick, int fePerTick) {
        super(type, pos, state); this.maxTargets = maxTargets; this.iePerTick = iePerTick; this.fePerTick = fePerTick;
    }
    public List<GlobalPos> targets() { return targets.stream().map(target -> target.connection).toList(); }
    public int getTargetCount() { return targets.size(); }

    /** Retained for callers that explicitly replace the configuration with one destination. */
    public GlobalPos target() { return targets.isEmpty() ? null : targets.getFirst().connection; }
    public void setTarget(GlobalPos target) {
        clearTargets();
        if (target != null) addTarget(target);
        setChanged();
    }

    public boolean addTarget(GlobalPos connection) {
        if (targets.size() >= maxTargets) return false;
        var receiver = resolveReceiver(connection);
        for (var target : targets) {
            if (target.connection.equals(connection) || resolveReceiver(target.connection).equals(receiver)) return false;
        }
        targets.add(new Target(connection));
        setChanged();
        return true;
    }

    public boolean removeTarget(GlobalPos connection) {
        var receiver = resolveReceiver(connection);
        boolean removed = false;
        var iterator = targets.iterator();
        while (iterator.hasNext()) {
            var target = iterator.next();
            if (target.connection.equals(connection) || resolveReceiver(target.connection).equals(receiver)) {
                target.clearCaches();
                iterator.remove();
                removed = true;
            }
        }
        if (removed) setChanged();
        return removed;
    }

    public BindingResult toggleTarget(GlobalPos connection) {
        // Removal remains available even when every target slot is occupied.
        if (removeTarget(connection)) return BindingResult.REMOVED;
        return addTarget(connection) ? BindingResult.ADDED : BindingResult.LIMIT_REACHED;
    }

    private GlobalPos resolveReceiver(GlobalPos connection) {
        if (level instanceof ServerLevel server) {
            var destination = server.getServer().getLevel(connection.dimension());
            if (destination != null && destination.hasChunkAt(connection.pos()))
                return GlobalPos.of(connection.dimension(), EnergyReceiver.controller(destination, connection.pos()));
        }
        return connection;
    }

    private void clearTargets() {
        targets.forEach(Target::clearCaches);
        targets.clear();
    }

    private final class Target {
        private final GlobalPos connection;
        private final List<BlockCapabilityCache<ImaginaryEnergy, Direction>> ieCaches = new ArrayList<>();
        private final List<BlockCapabilityCache<IEnergyStorage, Direction>> feCaches = new ArrayList<>();
        private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        private ServerLevel cachedLevel;
        private BlockPos cachedReceiver;
        private int cacheGeneration;

        private Target(GlobalPos connection) { this.connection = connection; }
        private void clearCaches() {
            cacheGeneration++;
            cachedLevel = null;
            cachedReceiver = null;
            ieCaches.clear(); feCaches.clear(); visited.clear();
        }
        private void prepareCaches(ServerLevel destination, BlockPos receiver) {
            if (cachedLevel == destination && receiver.equals(cachedReceiver)) return;
            clearCaches();
            cachedLevel = destination;
            cachedReceiver = receiver.immutable();
            int generation = cacheGeneration;
            for (Direction side : SIDES) {
                ieCaches.add(BlockCapabilityCache.create(LyyCapabilities.IMAGINARY_ENERGY, destination, receiver, side,
                        () -> !isRemoved() && cacheGeneration == generation, () -> {}));
                feCaches.add(BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, destination, receiver, side,
                        () -> !isRemoved() && cacheGeneration == generation, () -> {}));
            }
        }

        /** Returns false only when a loaded destination no longer exposes either energy capability. */
        private boolean supply(ServerLevel destination, BlockPos receiver) {
            prepareCaches(destination, receiver);
            // All faces of this receiver share one budget; repeated capability instances count once.
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
            // Each receiver uses FE only when its native IE interfaces accepted nothing.
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

    @Override public void setRemoved() { targets.forEach(Target::clearCaches); super.setRemoved(); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, TargetedEnergySourceBlockEntity tower) {
        if (!(level instanceof ServerLevel server)) return;
        var supplied = new HashSet<GlobalPos>();
        var iterator = tower.targets.iterator();
        while (iterator.hasNext()) {
            var target = iterator.next();
            var destination = server.getServer().getLevel(target.connection.dimension());
            if (destination == null || !destination.hasChunkAt(target.connection.pos())) continue;
            var receiver = EnergyReceiver.resolve(destination, target.connection.pos());
            if (!receiver.loaded(destination)) { target.clearCaches(); continue; }
            if (!supplied.add(GlobalPos.of(target.connection.dimension(), receiver.controller()))) continue;
            if (!target.supply(destination, receiver.input())) {
                target.clearCaches();
                iterator.remove();
                tower.setChanged();
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var saved = new ListTag();
        for (var target : targets)
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target.connection).result().ifPresent(saved::add);
        tag.remove("Target");
        tag.put("Targets", saved);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        clearTargets();
        var loaded = new HashSet<GlobalPos>();
        if (tag.contains("Targets", Tag.TAG_LIST)) {
            for (var value : tag.getList("Targets", Tag.TAG_COMPOUND))
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, value).result().ifPresent(connection -> {
                    if (targets.size() < maxTargets && loaded.add(connection)) targets.add(new Target(connection));
                });
        } else if (tag.contains("Target")) {
            GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Target")).result()
                    .ifPresent(connection -> targets.add(new Target(connection)));
        }
    }
}
