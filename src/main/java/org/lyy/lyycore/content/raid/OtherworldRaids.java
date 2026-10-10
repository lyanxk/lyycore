package org.lyy.lyycore.content.raid;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.lyy.lyycore.content.entity.CrystalTroop;
import org.lyy.lyycore.registry.LyyEntities;

/** Level-owned wave state survives altar/chunk unload. Only confirmed deaths retire raiders. */
@EventBusSubscriber(modid = "lyycore")
public final class OtherworldRaids extends SavedData {
    private static final int[] RECON = {10, 20, 30, 20};
    private final Map<UUID, Raid> raids = new LinkedHashMap<>();
    private static final class Raid {
        final UUID id; final BlockPos origin; final Set<UUID> members = new HashSet<>();
        final net.minecraft.server.level.ServerBossEvent bar = new net.minecraft.server.level.ServerBossEvent(
                net.minecraft.network.chat.Component.translatable("raid.lyycore.otherworld"),
                net.minecraft.world.BossEvent.BossBarColor.PURPLE, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        int wave, spawned, delay = 40;
        Raid(UUID id, BlockPos origin) { this.id = id; this.origin = origin; }
        int count() { return RECON[wave] + (wave == 3 ? 20 : 0); }
    }
    public static OtherworldRaids get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(OtherworldRaids::new, OtherworldRaids::load), "lyycore_otherworld_raids");
    }
    public boolean active(BlockPos pos) { return raids.values().stream().anyMatch(r -> r.origin.equals(pos)); }
    public boolean contains(UUID id) { return raids.containsKey(id); }
    public boolean start(BlockPos pos) {
        if (active(pos)) return false;
        var raid = new Raid(UUID.randomUUID(), pos.immutable()); raids.put(raid.id, raid); setDirty(); return true;
    }
    @SubscribeEvent public static void died(LivingDeathEvent e) {
        if (e.getEntity() instanceof CrystalTroop troop && troop.level() instanceof ServerLevel server && troop.raidId() != null) {
            var data = get(server); var raid = data.raids.get(troop.raidId());
            if (raid != null && raid.members.remove(troop.getUUID())) data.setDirty();
        }
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post e) {
        if (e.getLevel() instanceof ServerLevel level && level.getGameTime() % 20 == 0) get(level).tick(level);
    }
    private void tick(ServerLevel level) {
        if (raids.isEmpty()) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) { raids.values().forEach(r -> r.bar.removeAllPlayers()); raids.clear(); setDirty(); return; }
        var iterator = raids.values().iterator();
        while (iterator.hasNext()) {
            var raid = iterator.next();
            var viewers = level.players().stream().filter(p -> p.isAlive() && p.distanceToSqr(raid.origin.getCenter()) < 96*96).toList();
            for (var old : List.copyOf(raid.bar.getPlayers())) if (!viewers.contains(old)) raid.bar.removePlayer(old);
            viewers.forEach(raid.bar::addPlayer);
            raid.bar.setName(net.minecraft.network.chat.Component.translatable("raid.lyycore.wave", raid.wave + 1));
            raid.bar.setProgress((raid.members.size() + raid.count() - raid.spawned) / (float)raid.count());
            if (!level.hasChunkAt(raid.origin) || level.getNearestPlayer(raid.origin.getX(), raid.origin.getY(), raid.origin.getZ(), 96, true) == null) continue;
            if (raid.delay > 0) { raid.delay -= 20; setDirty(); continue; }
            if (raid.spawned == raid.count() && raid.members.isEmpty()) {
                if (++raid.wave == RECON.length) { raid.bar.removeAllPlayers(); iterator.remove(); setDirty(); continue; }
                raid.spawned = 0; raid.delay = 100; setDirty(); continue;
            }
            // Failed/blocked spawn attempts resume next second, without marking the wave complete.
            for (int attempts = 0; attempts < 80 && raid.spawned < raid.count(); attempts++) {
                double angle = level.random.nextDouble() * Math.PI * 2, radius = 24 + level.random.nextInt(17);
                var column = raid.origin.offset((int)(Math.cos(angle)*radius), 0, (int)(Math.sin(angle)*radius));
                if (!level.hasChunkAt(column)) continue;
                var pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).above(2);
                var type = raid.spawned < RECON[raid.wave] ? LyyEntities.RECON_CRYSTAL.get() : LyyEntities.ASSAULT_CRYSTAL.get();
                if (!level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)
                        || !level.noCollision(type.getDimensions().makeBoundingBox(pos.getBottomCenter()))) continue;
                var troop = type.create(level); if (troop == null) continue;
                troop.moveTo(pos.getBottomCenter()); troop.joinRaid(raid.id);
                if (level.addFreshEntity(troop)) { raid.members.add(troop.getUUID()); raid.spawned++; setDirty(); }
            }
        }
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider r) {
        var list = new ListTag();
        for (var raid : raids.values()) {
            var row = new CompoundTag(); row.putUUID("Id", raid.id); row.putLong("Origin", raid.origin.asLong());
            row.putInt("Wave", raid.wave); row.putInt("Spawned", raid.spawned); row.putInt("Delay", raid.delay);
            var members = new ListTag(); raid.members.forEach(id -> members.add(NbtUtils.createUUID(id))); row.put("Members", members); list.add(row);
        }
        tag.put("Raids", list); return tag;
    }
    private static OtherworldRaids load(CompoundTag tag, HolderLookup.Provider r) {
        var data = new OtherworldRaids();
        for (var value : tag.getList("Raids", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)value;
            if (!row.hasUUID("Id") || row.getInt("Wave") < 0 || row.getInt("Wave") >= 4) continue;
            var raid = new Raid(row.getUUID("Id"), BlockPos.of(row.getLong("Origin"))); raid.wave = row.getInt("Wave");
            raid.spawned = Math.clamp(row.getInt("Spawned"), 0, raid.count()); raid.delay = Math.max(0, row.getInt("Delay"));
            for (var member : row.getList("Members", Tag.TAG_INT_ARRAY)) raid.members.add(NbtUtils.loadUUID(member));
            data.raids.put(raid.id, raid);
        }
        return data;
    }
}
