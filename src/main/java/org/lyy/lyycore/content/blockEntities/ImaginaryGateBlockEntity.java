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
import net.minecraft.world.entity.LivingEntity;
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
import org.lyy.lyycore.content.blocks.ImaginaryGateBlock;
import org.lyy.lyycore.content.entity.ImaginaryGuardian;
import org.lyy.lyycore.content.entity.LifeRevel;
import org.lyy.lyycore.content.menu.ImaginaryGateMenu;
import org.lyy.lyycore.registry.*;
import java.util.UUID;

public class ImaginaryGateBlockEntity extends BlockEntity implements MenuProvider {
    public static final int READY = 0, WRONG_DIMENSION = 1, PEACEFUL = 2, OCCUPIED = 3, BLOCKED = 4;
    private int status;
    private UUID activeBoss;
    private UUID offeringPlayer;
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
        else if (level != null && !level.isClientSide && acceptsOffering(stack)) {
            offeringPlayer = player.getUUID();
        }
    }

    protected EntityType<? extends Mob> summonType(ItemStack offering) {
        return offering.is(LyyItems.CRYSTAL_BLOCK.get()) ? LyyEntities.IMAGINARY_GUARDIAN.get() : null;
    }
    public boolean acceptsOffering(ItemStack stack) { return summonType(stack) != null; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryGateBlockEntity gate) {
        if (level.getGameTime() % 10 != 0) return;
        ServerLevel server = (ServerLevel) level;
        gate.status = READY;
        if (level.dimension() != Level.OVERWORLD) gate.status = WRONG_DIMENSION;
        else if (level.getDifficulty() == Difficulty.PEACEFUL) gate.status = PEACEFUL;
        else if (gate.activeBoss != null && server.getEntity(gate.activeBoss) instanceof LivingEntity boss && boss.isAlive())
            gate.status = OCCUPIED;
        var type = gate.summonType(gate.items.getStackInSlot(0));
        if (gate.status != READY || type == null) return;
        Vec3 spawn = Vec3.atBottomCenterOf(pos.relative(state.getValue(ImaginaryGateBlock.FACING), 3));
        if (!level.hasChunkAt(BlockPos.containing(spawn))
                || !level.noCollision(type.getDimensions().makeBoundingBox(spawn))) {
            gate.status = BLOCKED;
            return;
        }
        Player summoner = level.getNearestPlayer(spawn.x, spawn.y, spawn.z, 12, false);
        if (summoner == null || !summoner.isAlive()) return;
        var boss = type.create(level);
        if (boss == null) return;
        boss.moveTo(spawn.x, spawn.y, spawn.z, state.getValue(ImaginaryGateBlock.FACING).toYRot(), 0);
        if (boss instanceof ImaginaryGuardian guardian) guardian.beginSummoning(summoner);
        if (boss instanceof LifeRevel revel) revel.beginSummoning(summoner);
        if (server.addFreshEntity(boss)) {
            // Credit the player who supplied the offering, not a nearby spectator.
            var contributor = gate.offeringPlayer == null ? null : server.getServer().getPlayerList().getPlayer(gate.offeringPlayer);
            if (contributor != null) CriteriaTriggers.SUMMONED_ENTITY.trigger(contributor, boss);
            gate.activeBoss = boss.getUUID();
            gate.items.extractItem(0, 1, false);
            gate.status = OCCUPIED;
            gate.setChanged();
        }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.imaginary_gate"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ImaginaryGateMenu(id, inventory, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        if (activeBoss != null) tag.putUUID("Guardian", activeBoss);
        if (offeringPlayer != null) tag.putUUID("OfferingPlayer", offeringPlayer);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        activeBoss = tag.hasUUID("Guardian") ? tag.getUUID("Guardian") : null;
        offeringPlayer = tag.hasUUID("OfferingPlayer") ? tag.getUUID("OfferingPlayer") : null;
    }
}
