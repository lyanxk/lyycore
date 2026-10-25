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
import org.lyy.lyycore.energy.EnergyReceiver;
import org.lyy.lyycore.energy.EnergyTarget;
import org.lyy.lyycore.energy.CoordinateEnergySource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Generates an independent energy budget at each configured, already-loaded destination. */
public class TargetedEnergySourceBlockEntity extends BlockEntity implements CoordinateEnergySource {
    private final int maxTargets, iePerTick, fePerTick;
    @Override public int maxTargets() { return maxTargets; }
    private final List<Target> targets = new ArrayList<>();

    public TargetedEnergySourceBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int maxTargets, int iePerTick, int fePerTick) {
        super(type, pos, state); this.maxTargets = maxTargets; this.iePerTick = iePerTick; this.fePerTick = fePerTick;
    }
    public List<GlobalPos> targets() { return targets.stream().map(target -> target.connection).toList(); }
    @Override public int getTargetCount() { return targets.size(); }

    @Override public BindingResult bindTarget(GlobalPos connection) {
        var receiver = resolveReceiver(connection);
        for (var target : targets) {
            if (target.connection.equals(connection) || resolveReceiver(target.connection).equals(receiver))
                return BindingResult.ALREADY_BOUND;
        }
        if (targets.size() >= maxTargets) return BindingResult.LIMIT_REACHED;
        targets.add(new Target(connection));
        setChanged();
        return BindingResult.ADDED;
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
        private final EnergyTarget energy = new EnergyTarget(() -> !isRemoved());
        private Target(GlobalPos connection) { this.connection = connection; }
        private void clearCaches() { energy.clear(); }
        private boolean supply(ServerLevel destination, BlockPos receiver) {
            return energy.supply(destination, receiver, iePerTick, fePerTick);
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
            saved.add(GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target.connection).getOrThrow());
        tag.put("Targets", saved);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        clearTargets();
        for (var value : tag.getList("Targets", Tag.TAG_COMPOUND))
            targets.add(new Target(GlobalPos.CODEC.parse(NbtOps.INSTANCE, value).getOrThrow()));
    }
}
