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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.content.GateSummoning;
import org.lyy.lyycore.content.raid.OtherworldRaids;
import org.lyy.lyycore.registry.*;

public final class SummoningAltarBlockEntity extends SummoningPedestalBlockEntity {
    private UUID activeBoss;
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
    public void summon(ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) return;
        if (items.getStackInSlot(0).is(LyyItems.FRIENDLY_PROOF.get())) { summonLife(player, server); return; }
        String error = null;
        if (level.dimension() != Level.OVERWORLD) error = "dimension";
        else if (level.getDifficulty() == Difficulty.PEACEFUL) error = "peaceful";
        else if (org.lyy.lyycore.content.entity.sovereign.LifeEncounters.get(server).occupied(worldPosition)
                || activeBoss != null && server.getEntity(activeBoss) instanceof LivingEntity boss && boss.isAlive()) error = "occupied";
        var type = GateSummoning.type(items.getStackInSlot(0), true);
        if (error == null && type == null) error = "offering";
        if (error != null) { player.displayClientMessage(Component.translatable("message.lyycore.altar." + error), true); return; }
        var boss = type.create(level);
        if (boss == null) return;
        if (!positionSummon(boss, player)) return;
        GateSummoning.begin(boss, player, worldPosition);
        if (server.addFreshEntity(boss)) {
            activeBoss = boss.getUUID(); items.extractItem(0, 1, false);
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, boss); setChanged();
        }
    }
    private void summonLife(ServerPlayer player, ServerLevel server) {
        var stands = pedestals();
        var battles = org.lyy.lyycore.content.entity.sovereign.LifeEncounters.get(server);
        String error = null;
        if (level.getDifficulty() == Difficulty.PEACEFUL) error = "peaceful";
        else if (stands.size() != 8) error = "layout";
        else if (battles.occupied(worldPosition) || activeBoss != null && server.getEntity(activeBoss) instanceof LivingEntity b && b.isAlive()) error = "occupied";
        else if (stands.stream().filter(p -> p.items.getStackInSlot(0).is(LyyItems.IMAGINARY_STEEL_BLOCK.get())).count() != 4
                || stands.stream().filter(p -> p.items.getStackInSlot(0).is(LyyItems.CRYSTAL_BLOCK.get())).count() != 4) error = "life_materials";
        if (error != null) { player.displayClientMessage(Component.translatable("message.lyycore.altar."+error), true); return; }
        var boss = LyyEntities.LIFE_DEFENDER.get().create(server); if (boss == null) return;
        if (!positionSummon(boss, player)) return;
        if (server.addFreshEntity(boss)) {
            boss.begin(worldPosition); activeBoss = boss.getUUID();
            items.extractItem(0, 1, false); stands.forEach(p -> p.items.extractItem(0, 1, false));
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, boss); setChanged();
        }
    }
    private boolean positionSummon(LivingEntity boss, ServerPlayer player) {
        // An altar summons above its surface, unlike a gate which summons in front of its opening.
        Vec3 spawn = Vec3.atBottomCenterOf(worldPosition.above());
        boss.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180, 0);
        // Use the actual entity box: some bosses have a rectangular footprint.
        if (level.noCollision(boss)) return true;
        player.displayClientMessage(Component.translatable("message.lyycore.altar.blocked"), true);
        return false;
    }
    public void replaceActiveBoss(UUID old, UUID next) { if (old.equals(activeBoss)) { activeBoss = next; setChanged(); } }
    public void darknessTick() {
        if (!(level instanceof ServerLevel server) || level.getDifficulty() == Difficulty.PEACEFUL) return;
        var pedestals = pedestals();
        if (pedestals.size() != 8 || OtherworldRaids.get(server).active(worldPosition)) return;
        var empty = pedestals.stream().filter(p -> p.items.getStackInSlot(0).isEmpty()).toList();
        if (!empty.isEmpty() && level.getMaxLocalRawBrightness(worldPosition) == 0 && level.random.nextInt(5) == 0)
            empty.get(level.random.nextInt(empty.size())).items.setStackInSlot(0, new ItemStack(LyyItems.IMAGINARY_CRYSTAL.get()));
        if (pedestals.stream().allMatch(p -> p.items.getStackInSlot(0).is(LyyItems.IMAGINARY_CRYSTAL.get()))
                && OtherworldRaids.get(server).start(worldPosition))
            pedestals.forEach(p -> p.items.extractItem(0, 1, false));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { super.saveAdditional(tag, r); if (activeBoss != null) tag.putUUID("Boss", activeBoss); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { super.loadAdditional(tag, r); activeBoss = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null; }
}
