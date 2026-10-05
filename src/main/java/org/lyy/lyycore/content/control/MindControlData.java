package org.lyy.lyycore.content.control;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** One active beacon per player, plus deferred executions for unloaded controlled monsters. */
public final class MindControlData extends SavedData {
    public enum Mode { WANDER, ATTACK, GATHER }
    public static final class Binding {
        public final UUID id, owner;
        public final GlobalPos beacon;
        public Mode mode = Mode.WANDER;
        public final Set<UUID> mobs = new HashSet<>();
        Binding(UUID id, UUID owner, GlobalPos beacon) { this.id = id; this.owner = owner; this.beacon = beacon; }
    }
    private static final Factory<MindControlData> FACTORY = new Factory<>(MindControlData::new, MindControlData::load);
    private final Map<UUID, Binding> owners = new HashMap<>();
    private final Set<UUID> executions = new HashSet<>();
    public static MindControlData get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "lyycore_mind_control"); }
    public Binding active(UUID owner) { return owners.get(owner); }
    public Binding activate(UUID owner, GlobalPos beacon) {
        var previous = owners.get(owner);
        if (previous != null && previous.beacon.equals(beacon)) return previous;
        var binding = new Binding(UUID.randomUUID(), owner, beacon);
        owners.put(owner, binding); setDirty(); return binding;
    }
    public void release(UUID owner, GlobalPos beacon) {
        var binding = owners.get(owner);
        if (binding != null && binding.beacon.equals(beacon)) { owners.remove(owner); setDirty(); }
    }
    public void execute(Binding binding) {
        executions.addAll(binding.mobs); binding.mobs.clear(); setDirty();
    }
    public boolean takeExecution(UUID mob) {
        if (!executions.remove(mob)) return false;
        setDirty(); return true;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var bindings = new ListTag();
        for (var binding : owners.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Id", binding.id); entry.putUUID("Owner", binding.owner);
            entry.putString("Dimension", binding.beacon.dimension().location().toString()); entry.putLong("Pos", binding.beacon.pos().asLong());
            entry.putString("Mode", binding.mode.name());
            var mobs = new ListTag(); binding.mobs.forEach(id -> mobs.add(NbtUtils.createUUID(id))); entry.put("Mobs", mobs);
            bindings.add(entry);
        }
        tag.put("Bindings", bindings);
        var pending = new ListTag(); executions.forEach(id -> pending.add(NbtUtils.createUUID(id))); tag.put("Executions", pending);
        return tag;
    }
    private static MindControlData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new MindControlData();
        for (var raw : tag.getList("Bindings", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag)raw;
            var dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension == null || !entry.hasUUID("Owner") || !entry.hasUUID("Id")) continue;
            var binding = new Binding(entry.getUUID("Id"), entry.getUUID("Owner"), GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("Pos"))));
            try { binding.mode = Mode.valueOf(entry.getString("Mode")); } catch (IllegalArgumentException ignored) { }
            for (var mob : entry.getList("Mobs", Tag.TAG_INT_ARRAY)) binding.mobs.add(NbtUtils.loadUUID(mob));
            data.owners.put(binding.owner, binding);
        }
        for (var mob : tag.getList("Executions", Tag.TAG_INT_ARRAY)) data.executions.add(NbtUtils.loadUUID(mob));
        return data;
    }
}
