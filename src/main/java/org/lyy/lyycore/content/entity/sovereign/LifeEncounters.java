package org.lyy.lyycore.content.entity.sovereign;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.lyy.lyycore.content.SummoningRules;
import org.lyy.lyycore.registry.LyyEffects;

/** One persistent battle survives all three bodies; only this store mutates battle records. */
@EventBusSubscriber(modid = "lyycore")
public final class LifeEncounters extends SavedData {
    public static final class Battle {
        public final BlockPos altar;
        public final int initialPlayers;
        private UUID boss;
        private int deaths, curseTicks;
        private final AABB bounds;
        private final Set<UUID> members = new HashSet<>();
        Battle(BlockPos altar, UUID boss, int initialPlayers) {
            this.altar = altar.immutable(); this.boss = boss; this.initialPlayers = initialPlayers;
            this.bounds = new AABB(altar.getCenter().add(-12.5, -7.5, -12.5), altar.getCenter().add(12.5, 7.5, 12.5));
        }
        public UUID boss() { return boss; }
        public AABB bounds() { return bounds; }
        public List<ServerPlayer> players(ServerLevel level) {
            return level.players().stream().filter(p -> eligible(p) && bounds.contains(p.position())).toList();
        }
    }
    private static final Factory<LifeEncounters> FACTORY = new Factory<>(LifeEncounters::new, LifeEncounters::load);
    private final Map<UUID, Battle> battles = new HashMap<>();
    private final Set<UUID> clearOnLogin = new HashSet<>();
    public static LifeEncounters get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_life_encounters"); }
    public Battle battle(UUID id) { return battles.get(id); }
    public boolean occupied(BlockPos altar) { return battles.values().stream().anyMatch(b -> b.altar.equals(altar)); }
    public UUID begin(ServerLevel level, BlockPos altar, UUID boss) {
        if (!SummoningRules.allowed(level)) return null;
        var initial = new Battle(altar, boss, 0);
        var players = initial.players(level);
        var battle = new Battle(altar, boss, players.size());
        for (var player : players) battle.members.add(player.getUUID());
        battles.put(boss, battle); setDirty(); return boss;
    }
    public void transition(UUID id, UUID next) {
        var battle = battles.get(id);
        if (battle != null && !battle.boss.equals(next)) { battle.boss = next; setDirty(); }
    }
    private void recordDeath(ServerPlayer player) {
        for (var battle : battles.values()) if (battle.members.contains(player.getUUID()) && battle.bounds.intersects(player.getBoundingBox())) {
            battle.deaths++; setDirty();
        }
    }
    private void registerMember(Battle battle, ServerPlayer player) {
        if (battle.bounds.contains(player.position()) && battle.members.add(player.getUUID())) setDirty();
    }
    private void advanceCurse(ServerLevel level, Battle battle) {
        if (++battle.curseTicks >= 900) {
            battle.curseTicks = 0;
            for (var player : battle.players(level)) curse(player, 1);
        }
        setDirty();
    }
    public void end(ServerLevel level, UUID id, boolean victory) {
        var battle = battles.remove(id);
        if (battle == null) return;
        if (victory) for (var uuid : battle.members) {
            var player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) player.removeEffect(LyyEffects.LIFE_CURSE);
            else clearOnLogin.add(uuid);
        }
        setDirty();
    }
    private static boolean eligible(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && !p.isCreative(); }
    public static void curse(ServerPlayer player, int layers) {
        var current = player.getEffect(LyyEffects.LIFE_CURSE);
        int amplifier = (current == null ? -1 : current.getAmplifier())+layers;
        boolean lethal = player.getMaxHealth()-2*layers <= 0;
        player.addEffect(new MobEffectInstance(LyyEffects.LIFE_CURSE, -1, amplifier));
        if (lethal) player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        else if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    public static void reduceCurse(ServerPlayer player, int layers) {
        var current = player.getEffect(LyyEffects.LIFE_CURSE);
        if (current == null || layers <= 0) return;
        int remaining = current.getAmplifier() + 1 - layers;
        player.removeEffect(LyyEffects.LIFE_CURSE);
        if (remaining > 0) player.addEffect(new MobEffectInstance(LyyEffects.LIFE_CURSE,
                current.getDuration(), remaining - 1, current.isAmbient(), current.isVisible(), current.showIcon()));
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var data = get(level);
        for (var entry : List.copyOf(data.battles.entrySet())) {
            var battle = entry.getValue(); var bounds = battle.bounds;
            if (battle.deaths > battle.initialPlayers) {
                if (level.getEntity(battle.boss) instanceof LifeSovereign boss) boss.discard();
                data.end(level, entry.getKey(), false); continue;
            }
            boolean populated = false;
            for (var player : level.players()) {
                if (!eligible(player)) continue;
                data.registerMember(battle, player);
                if (!battle.members.contains(player.getUUID())) continue;
                populated = true;
                // Clamp the full player body. This also catches pearls, chorus fruit and commands.
                double half = player.getBbWidth()/2;
                var clamped = new Vec3(Math.clamp(player.getX(), bounds.minX+half, bounds.maxX-half),
                        Math.clamp(player.getY(), bounds.minY, bounds.maxY-player.getBbHeight()),
                        Math.clamp(player.getZ(), bounds.minZ+half, bounds.maxZ-half));
                if (clamped.distanceToSqr(player.position()) > .0001) {
                    player.teleportTo(clamped.x, clamped.y, clamped.z); player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
                }
            }
            if (populated) data.advanceCurse(level, battle);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.removeEffect(LyyEffects.LIFE_CURSE);
        get(player.serverLevel()).recordDeath(player);
    }
    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDimension().equals(player.level().dimension())) return;
        if (get(player.serverLevel()).battles.values().stream().anyMatch(b -> b.members.contains(player.getUUID()))) event.setCanceled(true);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (var level : player.server.getAllLevels()) {
            var data = get(level);
            if (data.clearOnLogin.remove(player.getUUID())) { player.removeEffect(LyyEffects.LIFE_CURSE); data.setDirty(); }
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (var level : player.server.getAllLevels()) for (var battle : get(level).battles.values()) {
            if (battle.deaths > battle.initialPlayers || !battle.members.contains(player.getUUID())) continue;
            var spawn = battle.altar.getCenter().add(5, 1, 5);
            player.teleportTo(level, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot()); return;
        }
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        battles.forEach((id, battle) -> {
            var b = new CompoundTag(); b.putUUID("Id", id); b.putUUID("Boss", battle.boss); b.putLong("Altar", battle.altar.asLong());
            b.putInt("Players", battle.initialPlayers); b.putInt("Deaths", battle.deaths); b.putInt("CurseTicks", battle.curseTicks);
            var members = new ListTag(); battle.members.forEach(uuid -> members.add(NbtUtils.createUUID(uuid))); b.put("Members", members); list.add(b);
        });
        tag.put("Battles", list);
        var clear = new ListTag(); clearOnLogin.forEach(uuid -> clear.add(NbtUtils.createUUID(uuid))); tag.put("Clear", clear); return tag;
    }
    private static LifeEncounters load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new LifeEncounters();
        for (var raw : tag.getList("Battles", Tag.TAG_COMPOUND)) {
            var b = (CompoundTag)raw; var battle = new Battle(BlockPos.of(b.getLong("Altar")), b.getUUID("Boss"), b.getInt("Players"));
            battle.deaths = b.getInt("Deaths"); battle.curseTicks = b.getInt("CurseTicks");
            for (var uuid : b.getList("Members", Tag.TAG_INT_ARRAY)) battle.members.add(NbtUtils.loadUUID(uuid));
            data.battles.put(b.getUUID("Id"), battle);
        }
        for (var uuid : tag.getList("Clear", Tag.TAG_INT_ARRAY)) data.clearOnLogin.add(NbtUtils.loadUUID(uuid));
        return data;
    }
}
