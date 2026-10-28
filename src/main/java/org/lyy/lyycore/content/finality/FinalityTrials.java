package org.lyy.lyycore.content.finality;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.*;

import org.lyy.lyycore.content.blockEntities.SummoningPedestalBlockEntity;
import org.lyy.lyycore.content.research.ResearchProgress;
import org.lyy.lyycore.registry.LyyItems;

import java.util.*;

/**
 * One server-wide encounter. Persistence records settlement and return obligations, not a resumable
 * fight.
 */
@EventBusSubscriber(modid = "lyycore")
public final class FinalityTrials extends SavedData {
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, FinalityContent.id("finality_arena"));
    private static final Factory<FinalityTrials> FACTORY =
            new Factory<>(FinalityTrials::new, FinalityTrials::load);
    private static final int[][] STANDS = {
        {-2, -2}, {0, -2}, {2, -2}, {2, 0}, {2, 2}, {0, 2}, {-2, 2}, {-2, 0}
    };
    private static final TicketController TICKETS =
            new TicketController(
                    FinalityContent.id("finality_trial"),
                    (level, helper) ->
                            helper.getBlockTickets().keySet().forEach(helper::removeAllTickets));
    private GlobalPos altar;
    private final Map<UUID, Integer> members = new LinkedHashMap<>();
    private final Set<UUID> returnOnLogin = new HashSet<>();
    final Set<FinalitySummon> summons = new HashSet<>();
    final Set<FinalityWither> withers = new HashSet<>();
    final Set<FinalityProjectile> attacks = new HashSet<>();
    private final Set<Entity> witherProjectiles = new HashSet<>();
    private int progress, age, emptyTicks;
    private boolean interrupted;
    private List<ServerPlayer> players = List.of();

    public static FinalityTrials get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, "lyycore_finality_trial");
    }

    public boolean active() {
        return altar != null;
    }

    public List<ServerPlayer> players() {
        return players;
    }

    public int interval() {
        return progress >= 70 ? 40 : 60;
    }

    public boolean locked(ServerLevel level, BlockPos pos) {
        return altar != null
                && level.dimension().equals(altar.dimension())
                && (pos.equals(altar.pos())
                        || Arrays.stream(STANDS)
                                .anyMatch(d -> pos.equals(altar.pos().offset(d[0], 0, d[1]))));
    }

    public void begin(ServerPlayer initiator, BlockPos pos) {
        var source = initiator.serverLevel();
        if (active()) {
            if (members.containsKey(initiator.getUUID())) enter(initiator);
            else
                initiator.displayClientMessage(
                        Component.translatable("message.lyycore.finality.busy"), true);
            return;
        }
        if (!ResearchProgress.completed(
                initiator, FinalityContent.id("research/cross_spacetime"))) {
            initiator.displayClientMessage(
                    Component.translatable("message.lyycore.finality.research"), true);
            return;
        }
        var arena = source.getServer().getLevel(DIMENSION);
        if (arena == null) return;
        var center = source.getBlockEntity(pos);
        if (!(center instanceof SummoningPedestalBlockEntity main)
                || !main.items.getStackInSlot(0).is(LyyItems.SPACETIME_CRYSTAL.get())) return;
        var stands =
                Arrays.stream(STANDS)
                        .map(d -> source.getBlockEntity(pos.offset(d[0], 0, d[1])))
                        .toList();
        boolean valid = false;
        for (int offset = 0; offset < 2; offset++) {
            boolean alternating = true;
            for (int i = 0; i < 8; i++)
                alternating &=
                        stands.get(i) instanceof SummoningPedestalBlockEntity stand
                                && stand.getBlockState()
                                        .is(
                                                org.lyy.lyycore.registry.LyyBlocks
                                                        .SUMMONING_PEDESTAL
                                                        .get())
                                && stand.items
                                        .getStackInSlot(0)
                                        .is(
                                                ((i + offset) % 2 == 0
                                                                ? LyyItems.SPACETIME_ALLOY_BLOCK
                                                                : LyyItems.ENERGY_CORE)
                                                        .get());
            valid |= alternating;
        }
        if (!valid) {
            initiator.displayClientMessage(
                    Component.translatable("message.lyycore.finality.materials"), true);
            return;
        }
        var entrants =
                source.players().stream()
                        .filter(
                                p ->
                                        p.isAlive()
                                                && !p.isSpectator()
                                                && p.distanceToSqr(Vec3.atCenterOf(pos)) <= 32 * 32)
                        .toList();
        if (entrants.isEmpty()) return;
        altar = GlobalPos.of(source.dimension(), pos.immutable());
        progress = age = emptyTicks = 0;
        for (var p : entrants) members.put(p.getUUID(), 1);
        tickets(source, true);
        arenaTickets(arena, true);
        setDirty();
        for (var p : entrants) {
            applyMark(p, 1);
            enter(p);
        }
        refreshPlayers(arena);
        replenish(arena, 8, 8);
    }

    private void enter(ServerPlayer player) {
        var level = player.server.getLevel(DIMENSION);
        if (level != null) player.teleportTo(level, 0.5, 1, 0.5, player.getYRot(), 0);
    }

    private void tickets(ServerLevel source, boolean add) {
        if (altar == null) return;
        for (int x = (altar.pos().getX() - 2) >> 4; x <= (altar.pos().getX() + 2) >> 4; x++)
            for (int z = (altar.pos().getZ() - 2) >> 4; z <= (altar.pos().getZ() + 2) >> 4; z++)
                TICKETS.forceChunk(source, altar.pos(), x, z, add, true);
    }

    private static void arenaTickets(ServerLevel arena, boolean add) {
        for (int x = -3; x <= 2; x++)
            for (int z = -3; z <= 2; z++) TICKETS.forceChunk(arena, BlockPos.ZERO, x, z, add, true);
    }

    private void refreshPlayers(ServerLevel arena) {
        players =
                members.keySet().stream()
                        .map(arena.getServer().getPlayerList()::getPlayer)
                        .filter(
                                p ->
                                        p != null
                                                && p.isAlive()
                                                && !p.isSpectator()
                                                && p.level() == arena)
                        .toList();
    }

    private void tick(ServerLevel overworld) {
        if (interrupted) {
            interrupted = false;
            finish(overworld, false);
        }
        if (!active()) return;
        var arena = overworld.getServer().getLevel(DIMENSION);
        if (arena == null) {
            finish(overworld, false);
            return;
        }
        refreshPlayers(arena);
        if (players.isEmpty()) {
            if (++emptyTicks >= 100) finish(overworld, false);
            return;
        }
        emptyTicks = 0;
        age++;
        if (age % 240 == 0)
            arena.playSound(
                    null,
                    0,
                    18,
                    0,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                    net.minecraft.sounds.SoundSource.AMBIENT,
                    .35F,
                    .65F);
        for (var p : arena.players()) constrain(p);
        summons.removeIf(Entity::isRemoved);
        withers.removeIf(Entity::isRemoved);
        attacks.removeIf(Entity::isRemoved);
        witherProjectiles.removeIf(Entity::isRemoved);
        summons.forEach(FinalityTrials::constrain);
        withers.forEach(FinalityTrials::constrain);
        if (age % 40 == 0)
            replenish(arena, progress >= 70 ? 5 : 4, progress >= 70 ? 16 : progress >= 35 ? 12 : 8);
        for (var p : players) if (age % 4 == 0) FinalityNetwork.status(p, progress, layers(p));
    }

    private void replenish(ServerLevel level, int amount, int cap) {
        int[] count = new int[5];
        for (var s : summons)
            if (s.isAlive() && s.kind() != FinalitySummon.Kind.STAKE) count[s.kind().ordinal()]++;
        for (int i = 0; i < amount && Arrays.stream(count).sum() < cap; i++) {
            int choice = -1;
            for (int k : progress >= 35 ? new int[] {0, 1, 2, 3, 4} : new int[] {0, 2, 4})
                if (count[k] < 4 && (choice < 0 || count[k] < count[choice])) choice = k;
            if (choice < 0) break;
            double angle = level.random.nextDouble() * Math.PI * 2;
            var entity = FinalityContent.SUMMON.get().create(level);
            entity.setup(FinalitySummon.Kind.values()[choice]);
            entity.setPos(Math.cos(angle) * 22, 3 + level.random.nextInt(9), Math.sin(angle) * 22);
            summons.add(entity);
            level.addFreshEntity(entity);
            count[choice]++;
        }
    }

    public void killed(FinalitySummon summon) {
        if (!active() || !summons.remove(summon)) return;
        progress += summon.kind().progress;
        if (progress >= 100) finish((ServerLevel) summon.level(), true);
    }

    public void hit(ServerPlayer player, Entity attacker) {
        if (!active() || !members.containsKey(player.getUUID())) return;
        addMark(player);
        if (active())
            player.hurt(
                    player.damageSources()
                            .indirectMagic(
                                    attacker,
                                    attacker instanceof FinalityProjectile projectile
                                            ? projectile.owner()
                                            : attacker),
                    1);
    }

    public void addMark(ServerPlayer player) {
        int layers = layers(player);
        if (layers == 0 || layers + 1 >= 100) {
            finish(player.serverLevel(), false);
            return;
        }
        applyMark(player, layers + 1);
        members.put(player.getUUID(), layers + 1);
    }

    public static int layers(Player player) {
        var effect = player.getEffect(FinalityContent.MARK);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private static void applyMark(ServerPlayer player, int layers) {
        player.forceAddEffect(
                new MobEffectInstance(
                        FinalityContent.MARK,
                        layers == 1 ? -1 : 1600,
                        layers - 1,
                        false,
                        false,
                        true),
                null);
    }

    public void finish(ServerLevel current, boolean success) {
        if (altar == null) return;
        var source = current.getServer().getLevel(altar.dimension());
        GlobalPos origin = altar;
        if (source != null) tickets(source, false);
        var arena = current.getServer().getLevel(DIMENSION);
        if (arena != null) arenaTickets(arena, false);
        altar = null;
        for (var entity : witherProjectiles) entity.discard();
        witherProjectiles.clear();
        for (var entity : new ArrayList<>(attacks)) entity.discard();
        attacks.clear();
        for (var entity : new ArrayList<>(summons)) entity.discard();
        summons.clear();
        for (var entity : new ArrayList<>(withers)) entity.discard();
        withers.clear();
        if (success && source != null) {
            consume(source, origin.pos());
            for (var d : STANDS) consume(source, origin.pos().offset(d[0], 0, d[1]));
            net.minecraft.world.Containers.dropItemStack(
                    source,
                    origin.pos().getX() + .5,
                    origin.pos().getY() + 1,
                    origin.pos().getZ() + .5,
                    new ItemStack(FinalityContent.ECHO.get(), 2));
        }
        for (var id : members.keySet()) {
            var p = current.getServer().getPlayerList().getPlayer(id);
            if (p == null) returnOnLogin.add(id);
            else {
                clearPlayer(p);
                p.sendSystemMessage(
                        Component.translatable(
                                success
                                        ? "message.lyycore.finality.success"
                                        : "message.lyycore.finality.failure"));
            }
        }
        members.clear();
        players = List.of();
        setDirty();
    }

    private static void consume(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SummoningPedestalBlockEntity stand)
            stand.items.extractItem(0, 1, false);
    }

    private static void clearPlayer(ServerPlayer player) {
        player.removeEffect(FinalityContent.MARK);
        FinalityNetwork.status(player, -1, 0);
        if (!player.level().dimension().equals(DIMENSION)) return;
        var destination =
                player.findRespawnPositionAndUseSpawnBlock(
                        true, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
        var position = destination.pos();
        player.teleportTo(
                destination.newLevel(),
                position.x,
                position.y,
                position.z,
                destination.yRot(),
                destination.xRot());
    }

    public static Vec3 clamp(Vec3 pos, double margin) {
        return clamp(pos, margin, margin);
    }

    private static Vec3 clamp(Vec3 pos, double horizontalMargin, double height) {
        double r = Math.hypot(pos.x, pos.z),
                limit = 42 - horizontalMargin,
                scale = r > limit ? limit / r : 1;
        return new Vec3(pos.x * scale, Math.clamp(pos.y, 0, 60 - height), pos.z * scale);
    }

    private static void constrain(Entity entity) {
        Vec3 allowed = clamp(entity.position(), entity.getBbWidth() / 2, entity.getBbHeight());
        if (allowed.distanceToSqr(entity.position()) > 1e-8) {
            if (entity instanceof ServerPlayer p)
                p.connection.teleport(allowed.x, allowed.y, allowed.z, p.getYRot(), p.getXRot());
            else entity.setPos(allowed);
            Vec3 velocity = entity.getDeltaMovement(),
                    future =
                            clamp(
                                    allowed.add(velocity),
                                    entity.getBbWidth() / 2,
                                    entity.getBbHeight());
            entity.setDeltaMovement(future.subtract(allowed));
            entity.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void projectile(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity()
                        instanceof net.minecraft.world.entity.projectile.WitherSkull skull)) return;
        var data = skull.getPersistentData();
        if (skull.getOwner() instanceof FinalityWither) data.putBoolean("FinalityAttack", true);
        if (!data.getBoolean("FinalityAttack")) return;
        var trial = get(level);
        if (trial.active()) trial.witherProjectiles.add(skull);
        else event.setCanceled(true);
    }

    @SubscribeEvent
    public static void teleport(net.neoforged.neoforge.event.entity.EntityTeleportEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !level.dimension().equals(DIMENSION)
                || !get(level).active()) return;
        var point =
                clamp(
                        event.getTarget(),
                        event.getEntity().getBbWidth() / 2,
                        event.getEntity().getBbHeight());
        event.setTargetX(point.x);
        event.setTargetY(point.y);
        event.setTargetZ(point.z);
    }

    public void broken(ServerLevel level, BlockPos pos) {
        if (locked(level, pos)) finish(level, false);
    }

    @SubscribeEvent
    public static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD)
            get(level).tick(level);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void decay(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var trials = get(p.serverLevel());
        if (!trials.active() || !trials.members.containsKey(p.getUUID())) return;
        var effect = p.getEffect(FinalityContent.MARK);
        if (effect != null && effect.getAmplifier() > 0 && effect.getDuration() <= 1) {
            applyMark(p, effect.getAmplifier());
            trials.members.put(p.getUUID(), effect.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            var data = get(p.serverLevel());
            if (data.returnOnLogin.remove(p.getUUID())) {
                clearPlayer(p);
                data.setDirty();
            }
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            var data = get(p.serverLevel());
            if (data.active() && data.members.containsKey(p.getUUID()))
                applyMark(p, data.members.get(p.getUUID()));
        }
    }

    @SubscribeEvent
    public static void breakAltar(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && get(level).locked(level, event.getPos())) event.setCanceled(true);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (altar != null)
            tag.put("Altar", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, altar).getOrThrow());
        ListTag pending = new ListTag();
        var ids = new HashSet<>(returnOnLogin);
        ids.addAll(members.keySet());
        for (var id : ids) pending.add(NbtUtils.createUUID(id));
        tag.put("Returns", pending);
        return tag;
    }

    private static FinalityTrials load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new FinalityTrials();
        if (tag.contains("Altar"))
            result.altar = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Altar")).getOrThrow();
        for (var id : tag.getList("Returns", Tag.TAG_INT_ARRAY))
            result.returnOnLogin.add(NbtUtils.loadUUID(id));
        result.interrupted = result.altar != null;
        return result;
    }
}
