package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.GateMeteorFailures;
import org.lyy.lyycore.content.GateSummoning;
import org.lyy.lyycore.content.SummoningReservations;
import org.lyy.lyycore.content.SummoningRules;
import org.lyy.lyycore.content.blocks.ImaginaryGateBlock;
import org.lyy.lyycore.content.menu.ImaginaryGateMenu;
import org.lyy.lyycore.registry.*;
import java.util.Objects;
import java.util.UUID;

public class ImaginaryGateBlockEntity extends BlockEntity implements MenuProvider {
    public static final int READY = 0, WRONG_DIMENSION = 1, PEACEFUL = 2, OCCUPIED = 3, BLOCKED = 4;
    private int status;
    private UUID activeBoss;
    private boolean reservationMigrated;
    private UUID offeringPlayer;
    private UUID meteor;
    public UUID meteorId() { return meteor; }
    public boolean ownsMeteor(UUID id) { return id.equals(meteor); }
    public void meteorFailed(UUID id) {
        if (ownsMeteor(id)) { meteor = null; setChanged(); }
    }
    private void recoverMeteor(ServerLevel server) {
        if (GateMeteorFailures.get(server).consume(worldPosition, meteor)) { meteor = null; setChanged(); }
    }
    public boolean interceptOpening(net.minecraft.server.level.ServerPlayer player) {
        if (!SummoningRules.allowed(level)) return false;
        recoverMeteor(player.serverLevel());
        if (meteor != null) return true;
        var advancement = player.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.parse("lyycore:progression/too_great"));
        if (advancement == null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) return false;
        var entity = LyyEntities.GATE_METEOR.get().create(level);
        if (entity == null) return false;
        entity.aim(worldPosition);
        if (!level.addFreshEntity(entity) || entity.isRemoved()) return false;
        meteor = entity.getUUID(); setChanged();
        var closed = player.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.parse("lyycore:progression/closed_gate"));
        if (closed != null) player.getAdvancements().award(closed, "meteor");
        return true;
    }
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) { offeringPlayer = null; setChanged(); }
    };
    protected final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return status; }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 1; }
    };
    public ImaginaryGateBlockEntity(BlockPos pos, BlockState state) { this(LyyBlockEntities.IMAGINARY_GATE.get(), pos, state); }
    protected ImaginaryGateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public ItemStackHandler items() { return items; }
    public void setOffering(Player player, ItemStack stack) {
        // Vanilla can write an unchanged stack after clicking a full slot. Keep
        // its contributor, but still replace the stack reference for hotbar swaps.
        boolean unchanged = ItemStack.matches(items.getStackInSlot(0), stack);
        UUID previousContributor = offeringPlayer;
        items.setStackInSlot(0, stack);
        if (unchanged) offeringPlayer = previousContributor;
        else if (level != null && !level.isClientSide && acceptsOffering(stack)) offeringPlayer = player.getUUID();
    }
    protected EntityType<? extends Mob> summonType(ItemStack offering) { return GateSummoning.type(offering, false); }
    public boolean acceptsOffering(ItemStack stack) { return summonType(stack) != null; }
    public void replaceActiveBoss(UUID previous, UUID replacement) {
        if (previous.equals(activeBoss)) { activeBoss = replacement; setChanged(); }
    }
    private boolean occupied(ServerLevel server) {
        var reservations = SummoningReservations.get(server);
        if (!reservationMigrated) reservations.adopt(worldPosition, activeBoss);
        var current = reservations.active(worldPosition);
        if (!reservationMigrated || !Objects.equals(activeBoss, current)) {
            activeBoss = current; reservationMigrated = true; setChanged();
        }
        return activeBoss != null;
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) { occupied(server); recoverMeteor(server); }
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryGateBlockEntity gate) {
        if (level.getGameTime() % 10 != 0) return;
        ServerLevel server = (ServerLevel)level;
        if (gate.meteor != null) {
            gate.recoverMeteor(server);
            if (gate.meteor != null) return;
        }
        gate.status = READY;
        if (!SummoningRules.allowed(level)) gate.status = WRONG_DIMENSION;
        else if (level.getDifficulty() == Difficulty.PEACEFUL) gate.status = PEACEFUL;
        else if (gate.occupied(server)) gate.status = OCCUPIED;
        var type = gate.summonType(gate.items.getStackInSlot(0));
        if (gate.status != READY || type == null) return;
        Vec3 spawn = Vec3.atBottomCenterOf(pos.relative(state.getValue(ImaginaryGateBlock.FACING), 3));
        if (!level.hasChunkAt(BlockPos.containing(spawn)) || !level.noCollision(type.getDimensions().makeBoundingBox(spawn))) {
            gate.status = BLOCKED; return;
        }
        Player summoner = level.getNearestPlayer(spawn.x, spawn.y, spawn.z, 12, false);
        if (summoner == null || !summoner.isAlive()) return;
        var boss = type.create(level);
        if (boss == null) return;
        boss.moveTo(spawn.x, spawn.y, spawn.z, state.getValue(ImaginaryGateBlock.FACING).toYRot(), 0);
        GateSummoning.begin(boss, summoner, pos);
        var reservations = SummoningReservations.get(server);
        if (!reservations.reserve(pos, boss.getUUID())) { gate.status = OCCUPIED; return; }
        if (!server.addFreshEntity(boss)) { reservations.retire(boss.getUUID()); return; }
        // Credit the player who supplied the offering, not a nearby spectator.
        var contributor = gate.offeringPlayer == null ? null : server.getServer().getPlayerList().getPlayer(gate.offeringPlayer);
        if (contributor != null) CriteriaTriggers.SUMMONED_ENTITY.trigger(contributor, boss);
        gate.activeBoss = boss.getUUID();
        gate.items.extractItem(0, 1, false);
        gate.status = OCCUPIED;
        gate.setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.imaginary_gate"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ImaginaryGateMenu(id, inventory, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        if (activeBoss != null) tag.putUUID("Guardian", activeBoss);
        tag.putBoolean("ReservationMigrated", reservationMigrated);
        if (offeringPlayer != null) tag.putUUID("OfferingPlayer", offeringPlayer);
        if (meteor != null) tag.putUUID("Meteor", meteor);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        activeBoss = tag.hasUUID("Guardian") ? tag.getUUID("Guardian") : null;
        reservationMigrated = tag.getBoolean("ReservationMigrated");
        offeringPlayer = tag.hasUUID("OfferingPlayer") ? tag.getUUID("OfferingPlayer") : null;
        meteor = tag.hasUUID("Meteor") ? tag.getUUID("Meteor") : null;
    }
}
