package org.lyy.lyycore.content.entity.guiding;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Encounter membership survives separately loaded boss and guard chunks. */
public final class GuidingEncounter extends SavedData {
    public enum Phase { LIGHT, ABSORB, DEMAND, ENDED }
    public static final class Battle {
        private UUID boss, target;
        private Phase phase = Phase.LIGHT;
        private final Set<UUID> guards = new HashSet<>(), standing = new HashSet<>();
        private final Set<UUID> guardView = Collections.unmodifiableSet(guards);
        private final Set<UUID> standingView = Collections.unmodifiableSet(standing);
        public UUID boss() { return boss; }
        public UUID target() { return target; }
        public Phase phase() { return phase; }
        public Set<UUID> guards() { return guardView; }
        public Set<UUID> standing() { return standingView; }
    }
    private static final Factory<GuidingEncounter> FACTORY = new Factory<>(GuidingEncounter::new, GuidingEncounter::load);
    private final Map<UUID, Battle> battles = new HashMap<>();
    public static GuidingEncounter get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_guiding_encounters");
    }
    public Battle battle(UUID id) { return battles.get(id); }
    public Battle create(UUID id, UUID boss, UUID target) {
        var battle = new Battle(); battle.boss = boss; battle.target = target;
        battles.put(id, battle); setDirty(); return battle;
    }
    public void setTarget(UUID id, UUID target) {
        var battle = battles.get(id);
        if (battle != null && !java.util.Objects.equals(battle.target, target)) {
            battle.target = target; setDirty();
        }
    }
    public void transition(UUID id, UUID boss, Phase phase) {
        var battle = battles.get(id);
        if (battle != null && (!java.util.Objects.equals(battle.boss, boss) || battle.phase != phase)) {
            battle.boss = boss; battle.phase = phase; setDirty();
        }
    }
    public void addGuard(UUID id, UUID guard) {
        var battle = battles.get(id);
        if (battle == null) return;
        boolean changed = battle.guards.add(guard);
        changed |= battle.standing.add(guard);
        if (changed) setDirty();
    }
    public void setStanding(UUID id, UUID guard, boolean value) {
        var battle = battles.get(id);
        if (battle == null || !battle.guards.contains(guard)) return;
        if (value ? battle.standing.add(guard) : battle.standing.remove(guard)) setDirty();
    }
    public void end(UUID id) {
        var battle = battles.get(id);
        if (battle == null) return;
        battle.phase = Phase.ENDED;
        if (battle.guards.isEmpty()) battles.remove(id);
        setDirty();
    }
    public void removeGuard(UUID id, UUID guard) {
        var battle = battles.get(id);
        if (battle == null) return;
        boolean changed = battle.guards.remove(guard);
        changed |= battle.standing.remove(guard);
        if (battle.phase == Phase.ENDED && battle.guards.isEmpty()) { battles.remove(id); changed = true; }
        if (changed) setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        battles.forEach((id, battle) -> {
            var entry = new CompoundTag(); entry.putUUID("Id", id); entry.putUUID("Boss", battle.boss);
            if (battle.target != null) entry.putUUID("Target", battle.target);
            entry.putString("Phase", battle.phase.name());
            entry.put("Guards", ids(battle.guards)); entry.put("Standing", ids(battle.standing)); list.add(entry);
        });
        tag.put("Battles", list); return tag;
    }
    private static ListTag ids(Set<UUID> ids) {
        var list = new ListTag(); ids.forEach(id -> list.add(NbtUtils.createUUID(id))); return list;
    }
    private static GuidingEncounter load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new GuidingEncounter();
        for (var raw : tag.getList("Battles", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag)raw; var battle = new Battle();
            battle.boss = entry.getUUID("Boss");
            battle.target = entry.hasUUID("Target") ? entry.getUUID("Target") : null;
            battle.phase = Phase.valueOf(entry.getString("Phase"));
            for (var id : entry.getList("Guards", Tag.TAG_INT_ARRAY)) battle.guards.add(NbtUtils.loadUUID(id));
            for (var id : entry.getList("Standing", Tag.TAG_INT_ARRAY)) battle.standing.add(NbtUtils.loadUUID(id));
            battle.standing.retainAll(battle.guards);
            data.battles.put(entry.getUUID("Id"), battle);
        }
        return data;
    }
}
