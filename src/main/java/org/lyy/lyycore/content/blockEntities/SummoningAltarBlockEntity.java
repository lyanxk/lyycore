package org.lyy.lyycore.content.blockEntities;

import java.util.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.GateSummoning;
import org.lyy.lyycore.content.SummoningReservations;
import org.lyy.lyycore.content.SummoningRules;
import org.lyy.lyycore.content.raid.OtherworldRaids;
import org.lyy.lyycore.registry.*;

public final class SummoningAltarBlockEntity extends SummoningPedestalBlockEntity {
    private UUID activeBoss;
    private boolean reservationMigrated;
    public SummoningAltarBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.SUMMONING_ALTAR.get(), pos, state); }
    private List<SummoningPedestalBlockEntity> pedestals() {
        var result = new ArrayList<SummoningPedestalBlockEntity>();
        for (int x = -2; x <= 2; x += 2) for (int z = -2; z <= 2; z += 2) {
            if (x == 0 && z == 0) continue;
            var p = worldPosition.offset(x, 0, z);
            if (!level.hasChunkAt(p) || !level.getBlockState(p).is(LyyBlocks.SUMMONING_PEDESTAL.get())
                    || !(level.getBlockEntity(p) instanceof SummoningPedestalBlockEntity pedestal)) return List.of();
            result.add(pedestal);
        }
        return result;
    }
    private boolean occupied(ServerLevel server) {
        var reservations = SummoningReservations.get(server);
        if (!reservationMigrated) reservations.adopt(worldPosition, activeBoss);
        var current = reservations.active(worldPosition);
        if (!reservationMigrated || !Objects.equals(activeBoss, current)) {
            activeBoss = current; reservationMigrated = true; setChanged();
        }
        return activeBoss != null || org.lyy.lyycore.content.entity.sovereign.LifeEncounters.get(server).occupied(worldPosition);
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) occupied(server);
    }
    public void summon(ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) return;
        String error = null;
        if (!SummoningRules.allowed(level)) error = "dimension";
        else if (level.getDifficulty() == Difficulty.PEACEFUL) error = "peaceful";
        else if (occupied(server)) error = "occupied";
        if (error != null) { player.displayClientMessage(Component.translatable("message.lyycore.altar."+error), true); return; }
        // Dimension and occupancy rules apply equally to ordinary offerings and the life ritual.
        if (items.getStackInSlot(0).is(LyyItems.FRIENDLY_PROOF.get())) { summonLife(player, server); return; }
        var type = GateSummoning.type(items.getStackInSlot(0), true);
        if (type == null) { player.displayClientMessage(Component.translatable("message.lyycore.altar.offering"), true); return; }
        var boss = type.create(level);
        if (boss == null || !positionSummon(boss, player)) return;
        GateSummoning.begin(boss, player, worldPosition);
        var reservations = SummoningReservations.get(server);
        if (!reservations.reserve(worldPosition, boss.getUUID())) return;
        if (!server.addFreshEntity(boss)) { reservations.retire(boss.getUUID()); return; }
        activeBoss = boss.getUUID(); items.extractItem(0, 1, false);
        CriteriaTriggers.SUMMONED_ENTITY.trigger(player, boss); setChanged();
    }
    private void summonLife(ServerPlayer player, ServerLevel server) {
        var stands = pedestals();
        String error = null;
        if (stands.size() != 8) error = "layout";
        else if (stands.stream().filter(p -> p.items.getStackInSlot(0).is(LyyItems.IMAGINARY_STEEL_BLOCK.get())).count() != 4
                || stands.stream().filter(p -> p.items.getStackInSlot(0).is(LyyItems.CRYSTAL_BLOCK.get())).count() != 4) error = "life_materials";
        if (error != null) { player.displayClientMessage(Component.translatable("message.lyycore.altar."+error), true); return; }
        var boss = LyyEntities.LIFE_DEFENDER.get().create(server);
        if (boss == null || !positionSummon(boss, player)) return;
        var reservations = SummoningReservations.get(server);
        if (!reservations.reserve(worldPosition, boss.getUUID())) return;
        if (!server.addFreshEntity(boss)) { reservations.retire(boss.getUUID()); return; }
        boss.begin(worldPosition); activeBoss = boss.getUUID();
        items.extractItem(0, 1, false); stands.forEach(p -> p.items.extractItem(0, 1, false));
        CriteriaTriggers.SUMMONED_ENTITY.trigger(player, boss); setChanged();
    }
    private boolean positionSummon(LivingEntity boss, ServerPlayer player) {
        // An altar summons above its surface, unlike a gate which summons in front of its opening.
        Vec3 spawn = Vec3.atBottomCenterOf(worldPosition.above());
        boss.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180, 0);
        if (level.noCollision(boss)) return true;
        player.displayClientMessage(Component.translatable("message.lyycore.altar.blocked"), true);
        return false;
    }
    public void replaceActiveBoss(UUID old, UUID next) { if (old.equals(activeBoss)) { activeBoss = next; setChanged(); } }
    public void darknessTick() {
        if (!(level instanceof ServerLevel server) || !SummoningRules.allowed(level) || level.getDifficulty() == Difficulty.PEACEFUL) return;
        var pedestals = pedestals();
        var raids = OtherworldRaids.get(server);
        if (pedestals.size() != 8 || raids.active(worldPosition)) return;
        var empty = pedestals.stream().filter(p -> p.items.getStackInSlot(0).isEmpty()).toList();
        if (!empty.isEmpty() && level.getMaxLocalRawBrightness(worldPosition) <= 4 && level.random.nextInt(5) == 0)
            empty.get(level.random.nextInt(empty.size())).items.setStackInSlot(0, new ItemStack(LyyItems.IMAGINARY_CRYSTAL.get()));
        if (pedestals.stream().allMatch(p -> p.items.getStackInSlot(0).is(LyyItems.IMAGINARY_CRYSTAL.get())) && raids.start(server, worldPosition))
            pedestals.forEach(p -> p.items.extractItem(0, 1, false));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r);
        if (activeBoss != null) tag.putUUID("Boss", activeBoss);
        tag.putBoolean("ReservationMigrated", reservationMigrated);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r);
        activeBoss = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
        reservationMigrated = tag.getBoolean("ReservationMigrated");
    }
}
