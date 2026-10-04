package org.lyy.lyycore.content.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Lifecycle notifications must survive when the owning boss is in an unloaded chunk. */
final class RevelSummons extends SavedData {
    private static final Factory<RevelSummons> FACTORY = new Factory<>(RevelSummons::new, RevelSummons::load);
    private static final class Owner {
        boolean ended, killed;
        final Set<UUID> live = new HashSet<>();
        final Map<UUID, Boolean> removed = new HashMap<>();
    }
    private final Map<UUID, Owner> owners = new HashMap<>();
    static RevelSummons get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_revel_summons");
    }
    void register(UUID boss, Collection<UUID> dancers, Collection<UUID> blinds) {
        var owner = owners.computeIfAbsent(boss, ignored -> new Owner());
        if (owner.ended) return;
        owner.live.addAll(dancers);
        owner.live.addAll(blinds);
        owner.live.removeAll(owner.removed.keySet());
        setDirty();
    }
    void track(UUID boss, UUID minion) {
        var owner = owners.get(boss);
        if (owner != null && !owner.ended && owner.live.add(minion)) setDirty();
    }
    boolean active(UUID boss) { var owner = owners.get(boss); return owner != null && !owner.ended; }
    boolean ended(UUID boss) { var owner = owners.get(boss); return owner != null && owner.ended; }
    boolean killed(UUID boss) { var owner = owners.get(boss); return owner != null && owner.killed; }
    void removed(UUID boss, UUID minion, boolean died, boolean defer) {
        var owner = owners.get(boss);
        if (owner == null) return;
        owner.live.remove(minion);
        if (!owner.ended && defer) owner.removed.merge(minion, died, (before, now) -> before || now);
        if (owner.ended && owner.live.isEmpty()) owners.remove(boss);
        setDirty();
    }
    Map<UUID, Boolean> drain(UUID boss) {
        var owner = owners.get(boss);
        if (owner == null || owner.removed.isEmpty()) return Map.of();
        var result = Map.copyOf(owner.removed);
        owner.removed.clear();
        setDirty();
        return result;
    }
    void end(UUID boss, boolean killed) {
        var owner = owners.get(boss);
        if (owner == null) return;
        owner.ended = true;
        owner.killed = killed;
        owner.removed.clear();
        if (owner.live.isEmpty()) owners.remove(boss);
        setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        owners.forEach((boss, owner) -> {
            var entry = new CompoundTag();
            entry.putUUID("Boss", boss);
            entry.putBoolean("Ended", owner.ended); entry.putBoolean("Killed", owner.killed);
            var live = new ListTag();
            owner.live.forEach(id -> live.add(NbtUtils.createUUID(id)));
            entry.put("Live", live);
            var removed = new ListTag();
            owner.removed.forEach((id, died) -> {
                var removal = new CompoundTag();
                removal.putUUID("Id", id); removal.putBoolean("Died", died); removed.add(removal);
            });
            entry.put("Removed", removed); entries.add(entry);
        });
        tag.put("Owners", entries);
        return tag;
    }
    static RevelSummons load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new RevelSummons();
        for (Tag value : tag.getList("Owners", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            var owner = new Owner();
            owner.ended = entry.getBoolean("Ended"); owner.killed = entry.getBoolean("Killed");
            for (Tag id : entry.getList("Live", Tag.TAG_INT_ARRAY)) owner.live.add(NbtUtils.loadUUID(id));
            for (Tag removal : entry.getList("Removed", Tag.TAG_COMPOUND)) {
                var removed = (CompoundTag) removal;
                owner.removed.put(removed.getUUID("Id"), removed.getBoolean("Died"));
            }
            result.owners.put(entry.getUUID("Boss"), owner);
        }
        return result;
    }
}
