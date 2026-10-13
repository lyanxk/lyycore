package org.lyy.lyycore.content.blockEntities;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import org.lyy.lyycore.content.blocks.MatrixPortBlock;
import org.lyy.lyycore.content.logistics.LogisticsChannels;
import org.lyy.lyycore.content.menu.LogisticsMenu;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** Shared four-slot buffer and channel binding for input, output and collector. */
public final class MatrixPortBlockEntity extends BlockEntity implements MenuProvider {
    private UUID channel;
    public final ItemStackHandler items = new ItemStackHandler(4) {
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() { return items.getSlots(); }
        public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) { return !isOutput(); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isOutput() ? stack : items.insertItem(slot, stack, simulate);
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return items.extractItem(slot, amount, simulate); }
    };
    public MatrixPortBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.MATRIX_PORT.get(), pos, state); }
    public boolean isOutput() { return kind() == MatrixPortBlock.Kind.OUTPUT; }
    private MatrixPortBlock.Kind kind() { return ((MatrixPortBlock) getBlockState().getBlock()).kind(); }
    public UUID channel() { return channel; }
    public void bind(UUID id) {
        if (!(level instanceof ServerLevel server)) return;
        var directory = LogisticsChannels.get(server);
        if (id != null && directory.channel(id) == null) return;
        if (channel != null) directory.detach(channel, this);
        channel = id;
        if (id != null) directory.attach(id, this);
        setChanged();
    }
    public void clearBinding() { channel = null; setChanged(); }
    @Override public void onLoad() {
        super.onLoad();
        if (channel != null && level instanceof ServerLevel server && !LogisticsChannels.get(server).attach(channel, this)) clearBinding();
    }
    private void detach() {
        if (channel != null && level instanceof ServerLevel server) LogisticsChannels.get(server).detach(channel, this);
    }
    @Override public void onChunkUnloaded() { detach(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { detach(); super.setRemoved(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MatrixPortBlockEntity port) {
        if (port.channel != null && LogisticsChannels.get((ServerLevel) level).channel(port.channel) == null) port.clearBinding();
        if (port.isOutput()) port.pushOutput();
        else if (port.kind() == MatrixPortBlock.Kind.COLLECTOR && Math.floorMod(level.getGameTime() + pos.asLong(), 20) == 0) port.collect();
    }
    private void collect() {
        boolean room = false;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            var stack = items.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(64, stack.getMaxStackSize())) { room = true; break; }
        }
        if (!room) return;
        // Seven cells outward, seven across, seven high. The device itself is outside the volume.
        var center = worldPosition.relative(getBlockState().getValue(MatrixPortBlock.FACING), 4);
        var range = new AABB(center).inflate(3);
        for (var entity : level.getEntitiesOfClass(ItemEntity.class, range, e -> e.isAlive() && !e.getItem().isEmpty())) {
            var stack = entity.getItem();
            var remaining = ItemHandlerHelper.insertItemStacked(items, stack, false);
            if (remaining.isEmpty()) entity.discard();
            else if (remaining.getCount() != stack.getCount()) entity.setItem(remaining);
        }
    }
    private void pushOutput() {
        boolean empty = true;
        for (int slot = 0; slot < items.getSlots(); slot++) if (!items.getStackInSlot(slot).isEmpty()) { empty = false; break; }
        if (empty) return;
        for (var direction : Direction.values()) {
            var targetPos = worldPosition.relative(direction);
            if (!level.hasChunkAt(targetPos)) continue;
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, direction.getOpposite());
            if (target == null) continue;
            for (int slot = 0; slot < items.getSlots(); slot++) {
                var stack = items.extractItem(slot, 64, true);
                if (stack.isEmpty()) continue;
                int accepted = stack.getCount() - ItemHandlerHelper.insertItemStacked(target, stack, false).getCount();
                items.extractItem(slot, accepted, false);
            }
        }
    }
    public void dropContents() {
        for (int slot = 0; slot < items.getSlots(); slot++)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.extractItem(slot, 64, false));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        if (channel != null) tag.putUUID("Channel", channel);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        channel = tag.hasUUID("Channel") ? tag.getUUID("Channel") : null;
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new LogisticsMenu(id, inventory, this); }
}
