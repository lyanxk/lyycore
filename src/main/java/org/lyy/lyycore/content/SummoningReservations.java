package org.lyy.lyycore.content;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

    public static SummoningReservations get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "lyycore_summoning_reservations");
    }
    public UUID active(BlockPos origin) { return byOrigin.get(origin); }
    /** Summon entry points check occupancy; phase changes replace the current body. */
    public void reserve(BlockPos origin, UUID boss) {
        byOrigin.put(origin.immutable(), boss);
        setDirty();
    }
    public void retire(UUID boss) {
        if (byOrigin.values().remove(boss)) setDirty();
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || entity.getRemovalReason() == null
                || !entity.getRemovalReason().shouldDestroy()) return;
        if (entity instanceof ImaginaryGuardian || entity instanceof LifeRevel
                || entity instanceof GuidingBoss || entity instanceof LifeSovereign)
            get(level).retire(entity.getUUID());
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
            data.byOrigin.put(BlockPos.of(row.getLong("Origin")), row.getUUID("Boss"));
        }
        return data;
    }
}
