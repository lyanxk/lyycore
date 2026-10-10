package org.lyy.lyycore.content;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;

/** Failure receipts survive an unloaded controller; absence of a loaded meteor is never treated as failure. */
@EventBusSubscriber(modid = "lyycore")
public final class GateMeteorFailures extends SavedData {
    private static final Factory<GateMeteorFailures> FACTORY = new Factory<>(GateMeteorFailures::new, GateMeteorFailures::load);
    private final Map<BlockPos, Set<UUID>> failures = new HashMap<>();

    public static GateMeteorFailures get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_gate_meteor_failures");
    }
    public static void record(ServerLevel level, BlockPos origin, UUID meteor) {
        if (level.hasChunkAt(origin)) {
            if (level.getBlockEntity(origin) instanceof ImaginaryGateBlockEntity gate) gate.meteorFailed(meteor);
            return; // A removed/replaced controller no longer needs recovery.
        }
        var data = get(level);
        if (data.failures.computeIfAbsent(origin.immutable(), ignored -> new HashSet<>()).add(meteor)) data.setDirty();
    }
    public boolean consume(BlockPos origin, UUID meteor) {
        var receipts = failures.remove(origin);
        if (receipts == null) return false;
        setDirty();
        // Retire obsolete receipts at the same coordinate without clearing a newer mission.
        return meteor != null && receipts.contains(meteor);
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        failures.forEach((origin, ids) -> ids.forEach(id -> {
            var row = new CompoundTag(); row.putLong("Origin", origin.asLong()); row.putUUID("Meteor", id); list.add(row);
        }));
        tag.put("Failures", list); return tag;
    }
    private static GateMeteorFailures load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new GateMeteorFailures();
        for (var raw : tag.getList("Failures", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)raw;
            if (row.hasUUID("Meteor")) data.failures.computeIfAbsent(BlockPos.of(row.getLong("Origin")), ignored -> new HashSet<>()).add(row.getUUID("Meteor"));
        }
        return data;
    }
    /** Old versions did not record failed missions. Operators may explicitly cancel an unresolved one. */
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lyy").then(Commands.literal("summons")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear_meteor").then(Commands.argument("origin", BlockPosArgument.blockPos())
                        .executes(context -> {
                            var source = context.getSource(); var level = source.getLevel();
                            var origin = BlockPosArgument.getLoadedBlockPos(context, "origin");
                            if (!(level.getBlockEntity(origin) instanceof ImaginaryGateBlockEntity gate) || gate.meteorId() == null) {
                                source.sendFailure(Component.literal("该坐标没有待完成的关门陨石流程。")); return 0;
                            }
                            var meteor = gate.meteorId();
                            if (level.getEntity(meteor) != null) {
                                source.sendFailure(Component.literal("对应陨石仍已加载，请先正常结束或移除它。")); return 0;
                            }
                            gate.meteorFailed(meteor);
                            get(level).consume(origin, meteor);
                            source.sendSuccess(() -> Component.literal("已取消旧陨石流程；旧实体即使重新加载也不能撞击这座门。"), true);
                            return 1;
                        })))));
    }
}
