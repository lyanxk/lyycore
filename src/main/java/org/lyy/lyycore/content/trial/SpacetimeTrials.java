package org.lyy.lyycore.content.trial;

import java.util.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.lyy.lyycore.content.SummoningReservations;
import org.lyy.lyycore.content.blockEntities.SummoningPedestalBlockEntity;
import org.lyy.lyycore.content.skills.GuardSkill;
import org.lyy.lyycore.registry.*;

/** Owns trial membership and settlement. Saved records are cleanup receipts, not resumable battles. */
@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeTrials extends SavedData {
    private static final Factory<SpacetimeTrials> FACTORY = new Factory<>(SpacetimeTrials::new, SpacetimeTrials::load);
    private static final ResourceKey<DamageType> STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("lyycore:trial_strike"));
    private static final int[][] STANDS = {{-2,-2}, {0,-2}, {2,-2}, {2,0}, {2,2}, {0,2}, {-2,2}, {-2,0}};
    private final Map<UUID, Battle> battles = new HashMap<>();
    private final Map<BlockPos, Battle> locked = new HashMap<>();
    private final Set<UUID> clearOnLogin = new HashSet<>(), retireOnLoad = new HashSet<>();

    private static final class Member {
        int layers = 1;
        double counter;
        long decayAt, boundUntil, boundAt;
        MobEffectInstance previousSlow;
        Vec3 returnPosition;
        Member(long now, Vec3 position) { decayAt = now + 2400; returnPosition = position; }
    }

    public static final class Battle {
        public final UUID boss;
        public final BlockPos altar;
        public final AABB bounds;
        private final Map<UUID, Member> members = new LinkedHashMap<>();
        private final Map<BlockPos, Item> offerings;
        // Effects register once; ending a trial does not search all world entities.
        final Set<TrialAttack> attacks = new HashSet<>();
        private boolean aerial;
        private long playersAt = Long.MIN_VALUE;
        private List<ServerPlayer> onlinePlayers = List.of();
        Battle(UUID boss, BlockPos altar, Map<BlockPos, Item> offerings) {
            this.boss = boss; this.altar = altar.immutable(); this.offerings = offerings;
            bounds = arena(altar);
        }
        public List<ServerPlayer> players(ServerLevel level) {
            if (playersAt != level.getGameTime()) {
                playersAt = level.getGameTime();
                onlinePlayers = members.keySet().stream().map(level.getServer().getPlayerList()::getPlayer)
                        .filter(p -> p != null && p.level() == level && eligible(p)).toList();
            }
            return onlinePlayers;
        }
        boolean attacksFinished(long now) { return attacks.isEmpty() && members.values().stream().noneMatch(m -> m.boundUntil > now); }
        public boolean aerial(ServerLevel level) {
            if (!aerial && players(level).stream().anyMatch(p -> p.getAbilities().flying)) {
                aerial = true;
                message(level, "aerial");
            }
            return aerial;
        }
        void message(ServerLevel level, String key) {
            for (var id : members.keySet()) {
                var player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) player.sendSystemMessage(Component.translatable("message.lyycore.trial." + key));
            }
        }
        boolean materialsPresent(ServerLevel level) {
            return offerings.entrySet().stream().allMatch(e -> level.hasChunkAt(e.getKey())
                    && level.getBlockEntity(e.getKey()) instanceof SummoningPedestalBlockEntity stand
                    && stand.items.getStackInSlot(0).is(e.getValue()));
        }
    }

    public static SpacetimeTrials get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_spacetime_trials");
    }
    public Battle battle(UUID boss) { return battles.get(boss); }
    public boolean locked(BlockPos pos) { return locked.containsKey(pos); }
    private static boolean eligible(ServerPlayer player) { return player.isAlive() && !player.isCreative() && !player.isSpectator(); }
    private static AABB arena(BlockPos altar) {
        return new AABB(altar.getCenter().add(-24.5, -12.5, -24.5), altar.getCenter().add(24.5, 12.5, 24.5));
    }

    public void begin(ServerLevel level, ServerPlayer initiator, BlockPos altar) {
        clearRestartRecords(level);
        var area = arena(altar);
        var players = level.players().stream().filter(p -> eligible(p) && area.contains(p.position())).toList();
        if (players.isEmpty()) { tell(initiator, "no_players"); return; }
        if (battles.values().stream().anyMatch(b -> b.bounds.intersects(area))) { tell(initiator, "overlap"); return; }
        var offerings = offerings(level, altar);
        if (offerings == null) { tell(initiator, "materials"); return; }
        var boss = LyyEntities.TRIAL_GUIDE.get().create(level);
        boss.begin(altar, players.size(), initiator.getYRot() + 180);
        if (!level.noCollision(boss)) { tell(initiator, "blocked"); return; }
        var reservations = SummoningReservations.get(level);
        reservations.reserve(altar, boss.getUUID());
        var battle = new Battle(boss.getUUID(), altar, offerings);
        for (var player : players) battle.members.put(player.getUUID(), new Member(level.getGameTime(), player.position()));
        battles.put(boss.getUUID(), battle);
        offerings.keySet().forEach(pos -> locked.put(pos, battle));
        if (!level.addFreshEntity(boss)) {
            battles.remove(boss.getUUID()); offerings.keySet().forEach(locked::remove); reservations.retire(boss.getUUID()); return;
        }
        for (var player : players) {
            clearOnLogin.remove(player.getUUID());
            applyLock(player, battle.members.get(player.getUUID()), level.getGameTime());
        }
        setDirty();
        CriteriaTriggers.SUMMONED_ENTITY.trigger(initiator, boss);
    }

    /** Clockwise alternation accepts either material at the first corner. */
    private static Map<BlockPos, Item> offerings(ServerLevel level, BlockPos altar) {
        if (!(level.getBlockEntity(altar) instanceof SummoningPedestalBlockEntity center)
                || !center.items.getStackInSlot(0).is(LyyItems.TIME_KNOCKING_STONE)) return null;
        var result = new LinkedHashMap<BlockPos, Item>();
        result.put(altar.immutable(), LyyItems.TIME_KNOCKING_STONE.get());
        Item first = null;
        for (int i = 0; i < STANDS.length; i++) {
            var pos = altar.offset(STANDS[i][0], 0, STANDS[i][1]);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(LyyBlocks.SUMMONING_PEDESTAL)
                    || !(level.getBlockEntity(pos) instanceof SummoningPedestalBlockEntity stand)) return null;
            var item = stand.items.getStackInSlot(0).getItem();
            if (item != LyyItems.SIXFOLD_ALLOY_BLOCK.get() && item != LyyItems.ENERGY_CORE.get()) return null;
            if (i == 0) first = item;
            if ((i % 2 == 0) != (item == first)) return null;
            result.put(pos, item);
        }
        return result;
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.lyycore.trial." + key), true);
    }

    /** Independent counter settlement must run even when vanilla would reject a repeated hurt. */
    public boolean hit(ServerLevel level, UUID bossId, ServerPlayer player, double multiplier, boolean bind) {
        var battle = battles.get(bossId);
        if (battle == null || !eligible(player)) return false;
        var member = battle.members.get(player.getUUID());
        if (member == null || !(level.getEntity(bossId) instanceof TrialGuide boss)) return false;
        var source = boss.damageSources().source(STRIKE, boss);
        if (GuardSkill.tryPerfectGuard(player, source)) return false;
        int invulnerability = player.invulnerableTime;
        player.invulnerableTime = 0;
        try { player.hurt(source, 1); }
        finally { player.invulnerableTime = Math.max(invulnerability, player.invulnerableTime); }
        member.counter += boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * multiplier;
        int added = (int)(member.counter / 100);
        if (added > 0) {
            if (!player.hasEffect(LyyEffects.SPACETIME_LOCK)) { end(level, bossId, false); return false; }
            member.counter -= added * 100;
            member.layers += added;
            member.decayAt = level.getGameTime() + 2400;
            if (member.layers >= 20) { end(level, bossId, false); return false; }
            applyLock(player, member, level.getGameTime());
        }
        if (bind && player.isAlive()) {
            if (member.boundUntil <= level.getGameTime()) {
                var slow = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                member.previousSlow = slow == null ? null : new MobEffectInstance(slow);
                member.boundAt = level.getGameTime();
            }
            member.boundUntil = level.getGameTime() + 160;
            player.addEffect(new MobEffectInstance(LyyEffects.TRIAL_BINDING, 160, 0, false, false, true));
        }
        return true;
    }

    private static void applyLock(ServerPlayer player, Member member, long now) {
        // One extra tick keeps our deadline ahead of vanilla's effect expiration.
        player.removeEffect(LyyEffects.SPACETIME_LOCK);
        player.addEffect(new MobEffectInstance(LyyEffects.SPACETIME_LOCK,
                member.layers == 1 ? -1 : (int)Math.max(2, member.decayAt - now + 1), member.layers - 1, false, false, true));
    }

    /** Removes the battle before touching inventories, so callbacks cannot settle it twice. */
    public boolean end(ServerLevel level, UUID id, boolean victory) {
        var battle = battles.remove(id);
        if (battle == null) return false;
        victory &= battle.materialsPresent(level);
        battle.offerings.keySet().forEach(locked::remove);
        if (victory) for (var pos : battle.offerings.keySet())
            ((SummoningPedestalBlockEntity)level.getBlockEntity(pos)).items.extractItem(0, 1, false);
        for (var attack : List.copyOf(battle.attacks)) attack.discard();
        battle.members.forEach((uuid, member) -> {
            var player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) clearOnLogin.add(uuid);
            else clear(player, member, level.getGameTime());
        });
        battle.message(level, victory ? "success" : "failed");
        if (level.getEntity(id) instanceof TrialGuide boss) boss.depart();
        else SummoningReservations.get(level).retire(id);
        setDirty();
        return victory;
    }
    public void broken(ServerLevel level, BlockPos pos) {
        var battle = locked.get(pos);
        if (battle != null) end(level, battle.boss, false);
    }

    private static void clear(ServerPlayer player, Member member, long now) {
        player.removeEffect(LyyEffects.SPACETIME_LOCK);
        clearBinding(player, member, now);
    }
    private static void clearBinding(ServerPlayer player, Member member, long now) {
        player.removeEffect(LyyEffects.TRIAL_BINDING);
        var slow = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() == 255 && slow.getDuration() <= 2) player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (member != null && member.previousSlow != null) {
            var old = member.previousSlow;
            int duration = old.isInfiniteDuration() ? -1 : old.getDuration() - (int)(now - member.boundAt);
            if (old.isInfiniteDuration() || duration > 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    duration, old.getAmplifier(), old.isAmbient(), old.isVisible(), old.showIcon()));
            member.previousSlow = null;
        }
    }

    private void tick(ServerLevel level, Battle battle) {
        if (!(level.getEntity(battle.boss) instanceof TrialGuide boss) || boss.isRemoved()
                || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            end(level, battle.boss, false); return;
        }
        if (level.getGameTime() % 20 == 0 && !battle.materialsPresent(level)) {
            end(level, battle.boss, false); return;
        }
        int alive = 0;
        long now = level.getGameTime();
        for (var entry : battle.members.entrySet()) {
            var player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) continue;
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.level() != level || !battle.bounds.contains(player.position())) {
                end(level, battle.boss, false); return;
            }
            alive++;
            var member = entry.getValue();
            int extraTime = org.lyy.lyycore.content.equipment.SpacetimeAccelerationEffect.extraDurationTicks(player);
            // These effects use encounter deadlines rather than their displayed potion duration.
            var lock = player.getEffect(LyyEffects.SPACETIME_LOCK);
            if (extraTime > 0 && member.layers > 1 && lock != null) {
                member.decayAt -= extraTime;
                lock.duration = (int)Math.max(2, member.decayAt - now + 1);
            }
            if (member.layers > 1 && now >= member.decayAt && player.hasEffect(LyyEffects.SPACETIME_LOCK)) {
                member.layers--; member.decayAt = now + 2400; applyLock(player, member, now);
            }
            if (now < member.boundUntil) member.boundUntil -= extraTime;
            if (now < member.boundUntil) {
                // A short vanilla effect cannot outlive this trial after a crash or disconnect.
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 255, false, false, false));
                if (!player.hasEffect(LyyEffects.TRIAL_BINDING)) player.addEffect(new MobEffectInstance(LyyEffects.TRIAL_BINDING,
                        (int)(member.boundUntil-now), 0, false, false, true));
                else if (extraTime > 0) player.getEffect(LyyEffects.TRIAL_BINDING).duration = (int)(member.boundUntil - now);
            } else if (member.boundUntil != 0) {
                member.boundUntil = 0;
                clearBinding(player, member, now);
            }
            if (player.onGround() && battle.bounds.deflate(2).contains(player.position())) member.returnPosition = player.position();
            pushBoundary(player, battle.bounds, true);
        }
        if (alive == 0) { end(level, battle.boss, false); return; }
        for (var player : level.players()) if (eligible(player) && !battle.members.containsKey(player.getUUID())
                && battle.bounds.inflate(1).contains(player.position())) pushBoundary(player, battle.bounds, false);
    }
    private static void pushBoundary(ServerPlayer player, AABB box, boolean inward) {
        var p = player.position();
        double[] distances = {p.x-box.minX, box.maxX-p.x, p.y-box.minY, box.maxY-p.y, p.z-box.minZ, box.maxZ-p.z};
        int nearest = 0;
        for (int i=1; i<6; i++) if ((inward || i!=2 && i!=3) && distances[i] < distances[nearest]) nearest = i;
        if (inward && distances[nearest] > 2) return;
        if (!inward && box.contains(p)) {
            // A teleport can bypass the pushing band, and an arena wall can block a shove.
            // Put intruders back beyond the nearest horizontal face before applying the push.
            Vec3 exit = switch (nearest) {
                case 0 -> new Vec3(box.minX-1,p.y,p.z); case 1 -> new Vec3(box.maxX+1,p.y,p.z);
                case 4 -> new Vec3(p.x,p.y,box.minZ-1); default -> new Vec3(p.x,p.y,box.maxZ+1);
            };
            for (int y=0; y<=4; y++) {
                Vec3 destination = exit.add(0,y,0);
                if (player.level().noCollision(player, player.getBoundingBox().move(destination.subtract(p)))) {
                    player.teleportTo(destination.x,destination.y,destination.z); break;
                }
            }
        }
        double force = inward ? 1.2 : -1.5;
        Vec3 normal = switch (nearest) {
            case 0 -> new Vec3(force,0,0); case 1 -> new Vec3(-force,0,0);
            case 2 -> new Vec3(0,force,0); case 3 -> new Vec3(0,-force,0);
            case 4 -> new Vec3(0,0,force); default -> new Vec3(0,0,-force);
        };
        player.setDeltaMovement(player.getDeltaMovement().scale(.25).add(normal));
        player.hurtMarked = true; player.fallDistance = 0;
    }

    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var data = get(level);
        data.clearRestartRecords(level);
        for (var battle : List.copyOf(data.battles.values())) data.tick(level, battle);
    }
    private void clearRestartRecords(ServerLevel level) {
        if (retireOnLoad.isEmpty()) return;
        retireOnLoad.forEach(SummoningReservations.get(level)::retire);
        retireOnLoad.clear(); setDirty();
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) restore(player, false);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) restore(player, true);
    }
    private static void restore(ServerPlayer player, boolean respawn) {
        for (var level : player.server.getAllLevels()) {
            var data = get(level);
            if (data.clearOnLogin.remove(player.getUUID())) { clear(player, null, level.getGameTime()); data.setDirty(); }
            for (var battle : data.battles.values()) {
                var member = battle.members.get(player.getUUID());
                if (member == null) continue;
                var pos = member.returnPosition;
                player.teleportTo(level, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
                // Reconnecting must not conceal a deliberately removed negative effect.
                if (respawn || player.hasEffect(LyyEffects.SPACETIME_LOCK)) applyLock(player, member, level.getGameTime());
                battle.playersAt = Long.MIN_VALUE;
                if (member.boundUntil > level.getGameTime()) player.addEffect(new MobEffectInstance(LyyEffects.TRIAL_BINDING,
                        (int)(member.boundUntil-level.getGameTime()), 0, false, false, true));
            }
        }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var bosses = new HashSet<>(retireOnLoad);
        bosses.addAll(battles.keySet());
        var players = new HashSet<>(clearOnLogin);
        battles.values().forEach(b -> players.addAll(b.members.keySet()));
        tag.put("Bosses", uuids(bosses)); tag.put("Players", uuids(players));
        return tag;
    }
    private static ListTag uuids(Collection<UUID> values) {
        var list = new ListTag();
        values.forEach(id -> list.add(NbtUtils.createUUID(id)));
        return list;
    }
    private static SpacetimeTrials load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new SpacetimeTrials();
        for (var value : tag.getList("Bosses", Tag.TAG_INT_ARRAY)) data.retireOnLoad.add(NbtUtils.loadUUID(value));
        for (var value : tag.getList("Players", Tag.TAG_INT_ARRAY)) data.clearOnLogin.add(NbtUtils.loadUUID(value));
        return data;
    }
}
