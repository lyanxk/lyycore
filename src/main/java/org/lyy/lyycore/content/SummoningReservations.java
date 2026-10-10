package org.lyy.lyycore.content;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.lyy.lyycore.content.entity.ImaginaryGuardian;
import org.lyy.lyycore.content.entity.LifeRevel;
import org.lyy.lyycore.content.entity.guiding.GuidingBoss;
import org.lyy.lyycore.content.entity.sovereign.LifeSovereign;

/** Summon occupancy outlives both the summoning block and the boss's loaded chunk. */
@EventBusSubscriber(modid = "lyycore")
public final class SummoningReservations extends SavedData {
    private static final Factory<SummoningReservations> FACTORY =
            new Factory<>(SummoningReservations::new, SummoningReservations::load);
    private final Map<BlockPos, UUID> byOrigin = new HashMap<>();
    private final Map<UUID, BlockPos> byBoss = new HashMap<>();

    public static SummoningReservations get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_summoning_reservations");
    }
    public UUID active(BlockPos origin) { return byOrigin.get(origin); }
    /** A legacy UUID with unknown whereabouts stays occupied; absence from loaded entities is not death. */
    public void adopt(BlockPos origin, UUID legacy) {
        if (legacy != null && !byOrigin.containsKey(origin)) reserve(origin, legacy);
    }
    public boolean reserve(BlockPos origin, UUID boss) {
        var existing = byOrigin.get(origin);
        if (existing != null) return existing.equals(boss);
        if (byBoss.containsKey(boss)) return false;
        var immutable = origin.immutable();
        byOrigin.put(immutable, boss); byBoss.put(boss, immutable); setDirty(); return true;
    }
    public void replace(BlockPos origin, UUID previous, UUID next) {
        var existing = byOrigin.get(origin);
        if (existing != null && !existing.equals(previous)) return;
        retire(previous);
        reserve(origin, next);
    }
    public void retire(UUID boss) {
        var origin = byBoss.remove(boss);
        if (origin != null) { byOrigin.remove(origin, boss); setDirty(); }
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || entity.getRemovalReason() == null
                || !entity.getRemovalReason().shouldDestroy()) return;
        if (entity instanceof ImaginaryGuardian || entity instanceof LifeRevel
                || entity instanceof GuidingBoss || entity instanceof LifeSovereign)
            get(level).retire(entity.getUUID());
    }
    /** Explicit operator override for old saves with no record of whether an absent UUID died. */
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lyy").then(Commands.literal("summons")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear_unloaded").then(Commands.argument("origin", BlockPosArgument.blockPos())
                        .executes(context -> {
                            var source = context.getSource(); var level = source.getLevel();
                            var origin = BlockPosArgument.getBlockPos(context, "origin");
                            var data = get(level); var boss = data.active(origin);
                            if (boss == null) {
                                source.sendFailure(Component.literal("该坐标没有召唤占用记录。")); return 0;
                            }
                            if (level.getEntity(boss) != null) {
                                source.sendFailure(Component.literal("对应首领仍已加载，请先正常结束战斗。")); return 0;
                            }
                            data.retire(boss);
                            source.sendSuccess(() -> Component.literal("已解除召唤占用；未删除可能仍在卸载区块中的首领。"), true);
                            return 1;
                        })))));
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        byOrigin.forEach((origin, boss) -> {
            var row = new CompoundTag(); row.putLong("Origin", origin.asLong()); row.putUUID("Boss", boss); list.add(row);
        });
        tag.put("Reservations", list); return tag;
    }
    private static SummoningReservations load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new SummoningReservations();
        for (var raw : tag.getList("Reservations", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)raw;
            if (!row.hasUUID("Boss")) continue;
            var origin = BlockPos.of(row.getLong("Origin")); var boss = row.getUUID("Boss");
            if (!data.byOrigin.containsKey(origin) && !data.byBoss.containsKey(boss)) {
                data.byOrigin.put(origin, boss); data.byBoss.put(boss, origin);
            }
        }
        return data;
    }
}
