package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.blocks.ImaginaryGateBlock;
import org.lyy.lyycore.content.entity.ImaginaryGuardian;
import org.lyy.lyycore.content.menu.ImaginaryGateMenu;
import org.lyy.lyycore.registry.*;
import java.util.UUID;

public final class ImaginaryGateBlockEntity extends BlockEntity implements MenuProvider {
    public static final int READY = 0, WRONG_DIMENSION = 1, PEACEFUL = 2, OCCUPIED = 3, BLOCKED = 4;
    private int status;
    private UUID activeGuardian;
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return status; }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 1; }
    };
    public ImaginaryGateBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.IMAGINARY_GATE.get(), pos, state); }
    public ItemStackHandler items() { return items; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryGateBlockEntity gate) {
        if (level.getGameTime() % 10 != 0) return;
        ServerLevel server = (ServerLevel) level;
        gate.status = READY;
        if (level.dimension() != Level.OVERWORLD) gate.status = WRONG_DIMENSION;
        else if (level.getDifficulty() == Difficulty.PEACEFUL) gate.status = PEACEFUL;
        else if (gate.activeGuardian != null && server.getEntity(gate.activeGuardian) instanceof ImaginaryGuardian guardian && guardian.isAlive())
            gate.status = OCCUPIED;
        if (gate.status != READY || !gate.items.getStackInSlot(0).is(LyyBlocks.CRYSTAL_BLOCK.get().asItem())) return;
        Vec3 spawn = Vec3.atBottomCenterOf(pos.relative(state.getValue(ImaginaryGateBlock.FACING), 3));
        if (!level.hasChunkAt(BlockPos.containing(spawn))
                || !level.noCollision(new AABB(spawn.x - 1, spawn.y, spawn.z - 1, spawn.x + 1, spawn.y + 3.5, spawn.z + 1))) {
            gate.status = BLOCKED;
            return;
        }
        Player summoner = level.getNearestPlayer(spawn.x, spawn.y, spawn.z, 12, false);
        if (summoner == null || !summoner.isAlive()) return;
        var guardian = LyyEntities.IMAGINARY_GUARDIAN.get().create(level);
        if (guardian == null) return;
        guardian.moveTo(spawn.x, spawn.y, spawn.z, state.getValue(ImaginaryGateBlock.FACING).toYRot(), 0);
        guardian.beginSummoning(summoner);
        if (server.addFreshEntity(guardian)) {
            gate.activeGuardian = guardian.getUUID();
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
        if (activeGuardian != null) tag.putUUID("Guardian", activeGuardian);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        activeGuardian = tag.hasUUID("Guardian") ? tag.getUUID("Guardian") : null;
    }
}
