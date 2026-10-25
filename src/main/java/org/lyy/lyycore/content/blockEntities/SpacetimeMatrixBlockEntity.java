package org.lyy.lyycore.content.blockEntities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.blocks.SpacetimeMatrixBlock;
import org.lyy.lyycore.content.energy.PowerChannels;
import org.lyy.lyycore.content.menu.SpacetimeMatrixMenu;
import org.lyy.lyycore.energy.EnergyReceiver;
import org.lyy.lyycore.energy.EnergyTarget;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyCapabilities;
import org.lyy.lyycore.registry.LyyItems;

public final class SpacetimeMatrixBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CHARGE_SLOTS = 6, UPGRADE_SLOT = 6, MAX_POWER = 10;
    private int power = 1;
    private UUID channelId;
    private final Map<GlobalPos, EnergyTarget> targets = new HashMap<>();

    public final ItemStackHandler items = new ItemStackHandler(7) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == UPGRADE_SLOT ? stack.is(LyyItems.SPACETIME_SHARD.get()) : chargeable(stack);
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    public SpacetimeMatrixBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.SPACETIME_MATRIX.get(), pos, state);
    }

    public int power() { return level != null && level.isClientSide ? getBlockState().getValue(SpacetimeMatrixBlock.POWER) : power; }
    public int stage() { return power() == MAX_POWER ? 3 : power() >= 5 ? 2 : 1; }
    public int iePerTick() { return power() * power() * 10_000_000; }
    public int fePerTick() { return power() * power() * 21_474_836; }
    public UUID channelId() { return channelId; }
    public GlobalPos address() { return GlobalPos.of(level.dimension(), worldPosition); }

    public PowerChannels.Channel ensureChannel(ServerPlayer owner) {
        if (!(level instanceof ServerLevel server)) return null;
        var directory = PowerChannels.get(server);
        var channel = directory.channel(channelId);
        if (channel != null && channel.source.equals(address())) return channel;
        if (owner.isSpectator()) return null;
        channel = directory.create(address(), owner.getUUID());
        channelId = channel.id;
        setChanged();
        return channel;
    }

    public static boolean chargeable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var ie = stack.getCapability(LyyCapabilities.IMAGINARY_ENERGY_ITEM);
        if (ie != null && ie.canReceiveImaginaryEnergy()) return true;
        var fe = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return fe != null && fe.canReceive();
    }

    /** Each item receives its own full budget. Native IE gets the first opportunity. */
    public boolean chargeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var ie = stack.getCapability(LyyCapabilities.IMAGINARY_ENERGY_ITEM);
        if (ie != null && ie.canReceiveImaginaryEnergy() && ie.receiveImaginaryEnergy(iePerTick(), false) > 0) return true;
        var fe = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return fe != null && fe.canReceive() && fe.receiveEnergy(fePerTick(), false) > 0;
    }

    public void restorePower(int power) {
        this.power = Math.clamp(power, 1, MAX_POWER);
        updateStage();
        setChanged();
    }

    private void upgrade() {
        if (power >= MAX_POWER) return;
        var stack = items.getStackInSlot(UPGRADE_SLOT);
        if (!stack.is(LyyItems.SPACETIME_SHARD.get())) return;
        int count = Math.min(MAX_POWER - power, stack.getCount());
        items.extractItem(UPGRADE_SLOT, count, false);
        restorePower(power + count);
    }

    private void updateStage() {
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof SpacetimeMatrixBlock block)) return;
        for (int part = 0; part < block.width() * block.width(); part++) {
            var pos = block.cellPos(worldPosition, part);
            var state = level.getBlockState(pos);
            if (state.is(block) && state.getValue(SpacetimeMatrixBlock.POWER) != power)
                level.setBlock(pos, state.setValue(SpacetimeMatrixBlock.POWER, power), Block.UPDATE_CLIENTS);
        }
    }

    private void supplyTargets(ServerLevel server) {
        var directory = PowerChannels.get(server);
        var channel = directory.channel(channelId);
        if (channel == null || !channel.source.equals(address())) { clearTargets(); return; }
        var connections = channel.targets();
        targets.entrySet().removeIf(entry -> {
            if (connections.contains(entry.getKey())) return false;
            entry.getValue().clear();
            return true;
        });
        var supplied = new HashSet<GlobalPos>();
        for (var connection : connections) {
            var destination = server.getServer().getLevel(connection.dimension());
            var cached = targets.get(connection);
            if (destination == null || !destination.hasChunkAt(connection.pos())) {
                if (cached != null) cached.clear();
                continue;
            }
            var receiver = EnergyReceiver.resolve(destination, connection.pos());
            if (!receiver.loaded(destination)) {
                if (cached != null) cached.clear();
                continue;
            }
            if (!supplied.add(GlobalPos.of(connection.dimension(), receiver.controller()))) continue;
            var target = targets.computeIfAbsent(connection, ignored -> new EnergyTarget(() -> !isRemoved()));
            if (!target.supply(destination, receiver.input(), iePerTick(), fePerTick())) {
                target.clear();
                targets.remove(connection);
                directory.removeInvalidTarget(channelId, connection);
            }
        }
    }

    /** Actual block dismantling calls this; chunk unloading only clears transient caches. */
    public void release() {
        if (level instanceof ServerLevel server) PowerChannels.get(server).removeMatrix(address());
        clearTargets();
    }

    private void clearTargets() { targets.values().forEach(EnergyTarget::clear); targets.clear(); }
    @Override public void setRemoved() { clearTargets(); super.setRemoved(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpacetimeMatrixBlockEntity matrix) {
        if (!(level instanceof ServerLevel server)) return;
        matrix.upgrade();
        boolean changed = false;
        for (int slot = 0; slot < CHARGE_SLOTS; slot++) changed |= matrix.chargeItem(matrix.items.getStackInSlot(slot));
        if (changed) matrix.setChanged();
        matrix.supplyTargets(server);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Power", power);
        if (channelId != null) tag.putUUID("Channel", channelId);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = Math.clamp(tag.getInt("Power"), 1, MAX_POWER);
        channelId = tag.hasUUID("Channel") ? tag.getUUID("Channel") : null;
        items.deserializeNBT(registries, tag.getCompound("Items"));
        clearTargets();
    }

    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.spacetime_matrix"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (player instanceof ServerPlayer owner && !owner.isSpectator()) ensureChannel(owner);
        return new SpacetimeMatrixMenu(id, inventory, this);
    }
    @Override public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }
}
