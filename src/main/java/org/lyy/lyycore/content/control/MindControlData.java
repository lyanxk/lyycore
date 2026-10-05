package org.lyy.lyycore.content.control;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One active beacon per player, plus attributed, single-attempt execution requests. */
public final class MindControlData extends SavedData {
    public enum Mode { WANDER, ATTACK, GATHER }
    public static final class Binding {
        public final UUID id, owner;
        public final GlobalPos beacon;
        private Mode mode = Mode.WANDER;
        private final Set<UUID> members = new HashSet<>();
        /** Read-only live view; membership changes and persistence are owned by this data store. */
        public final Set<UUID> mobs = Collections.unmodifiableSet(members);
        public Mode mode() { return mode; }
        Binding(UUID id, UUID owner, GlobalPos beacon) { this.id = id; this.owner = owner; this.beacon = beacon; }
    }
    public record Execution(UUID mob, UUID owner, UUID binding, boolean failed) { }
    private static final Factory<MindControlData> FACTORY = new Factory<>(MindControlData::new, MindControlData::load);
    private final Map<UUID, Binding> owners = new HashMap<>();
    private final Map<UUID, Execution> executions = new HashMap<>();
    // Older saves stored target UUIDs only. Migrate before rebinding overwrites entity metadata.
    private final Set<UUID> legacyExecutions = new HashSet<>();
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
    public boolean addMob(Binding binding, UUID mob) {
        if (owners.get(binding.owner) != binding || !binding.members.add(mob)) return false;
        setDirty(); return true;
    }
    public void removeMob(Binding binding, UUID mob) {
        if (owners.get(binding.owner) == binding && binding.members.remove(mob)) setDirty();
    }
    public boolean setMode(Binding binding, Mode mode) {
        if (owners.get(binding.owner) != binding || binding.mode == mode) return false;
        binding.mode = Objects.requireNonNull(mode); setDirty(); return true;
    }
    /** Retain control until death is confirmed. Only a fresh explicit command retries failure. */
    public void execute(Binding binding) {
        if (owners.get(binding.owner) != binding) return;
        boolean changed = false;
        for (var mob : binding.members) {
            var previous = executions.get(mob);
            if (previous == null || previous.failed) {
                executions.put(mob, new Execution(mob, binding.owner, binding.id, false)); changed = true;
            }
        }
        if (changed) setDirty();
    }
    public Execution execution(UUID mob) { return executions.get(mob); }
    public void resolveLegacyExecution(UUID mob, UUID owner, UUID binding) {
        if (owner == null || binding == null || !legacyExecutions.remove(mob)) return;
        executions.putIfAbsent(mob, new Execution(mob, owner, binding, false)); setDirty();
    }
    public boolean hasLegacyExecution(UUID mob) { return legacyExecutions.contains(mob); }
    /** Consume the attempt before damage hooks, preventing reentrant or automatic retries. */
    public Execution beginExecution(UUID mob) {
        var request = executions.get(mob);
        if (request == null || request.failed) return null;
        var attempted = new Execution(request.mob, request.owner, request.binding, true);
        executions.put(mob, attempted); setDirty(); return attempted;
    }
    public void finishExecution(Execution attempted, boolean died) {
        // A damage callback may have replaced the command; do not acknowledge a different one.
        if (executions.get(attempted.mob) != attempted || !died) return;
        executions.remove(attempted.mob);
        var binding = owners.get(attempted.owner);
        if (binding != null && binding.id.equals(attempted.binding)) binding.members.remove(attempted.mob);
        setDirty();
    }
    public void forgetExecution(UUID mob) {
        boolean changed = executions.remove(mob) != null;
        changed |= legacyExecutions.remove(mob);
        if (changed) setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var bindings = new ListTag();
        for (var binding : owners.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Id", binding.id); entry.putUUID("Owner", binding.owner);
            entry.putString("Dimension", binding.beacon.dimension().location().toString()); entry.putLong("Pos", binding.beacon.pos().asLong());
            entry.putString("Mode", binding.mode.name());
            var mobs = new ListTag(); binding.members.forEach(id -> mobs.add(NbtUtils.createUUID(id))); entry.put("Mobs", mobs);
            bindings.add(entry);
        }
        tag.put("Bindings", bindings);
        var pending = new ListTag();
        for (var request : executions.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Mob", request.mob); entry.putUUID("Owner", request.owner); entry.putUUID("Binding", request.binding);
            entry.putBoolean("Failed", request.failed); pending.add(entry);
        }
        tag.put("ExecutionsV2", pending);
        var legacy = new ListTag(); legacyExecutions.forEach(id -> legacy.add(NbtUtils.createUUID(id))); tag.put("Executions", legacy);
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
            for (var mob : entry.getList("Mobs", Tag.TAG_INT_ARRAY)) binding.members.add(NbtUtils.loadUUID(mob));
            data.owners.put(binding.owner, binding);
        }
        for (var raw : tag.getList("ExecutionsV2", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag)raw;
            if (!entry.hasUUID("Mob") || !entry.hasUUID("Owner") || !entry.hasUUID("Binding")) continue;
            var request = new Execution(entry.getUUID("Mob"), entry.getUUID("Owner"), entry.getUUID("Binding"), entry.getBoolean("Failed"));
            data.executions.put(request.mob, request);
        }
        for (var mob : tag.getList("Executions", Tag.TAG_INT_ARRAY)) data.legacyExecutions.add(NbtUtils.loadUUID(mob));
        return data;
    }
}
