package org.lyy.lyycore.content.blockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.content.trial.SpacetimeTrials;
public class SummoningPedestalBlockEntity extends BlockEntity {
    public final ItemStackHandler items = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return locked() ? stack : super.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return locked() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    public boolean locked() { return level instanceof ServerLevel server && SpacetimeTrials.get(server).locked(worldPosition); }
    public SummoningPedestalBlockEntity(BlockPos pos, BlockState state) { this(LyyBlockEntities.SUMMONING_PEDESTAL.get(), pos, state); }
    protected SummoningPedestalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { super.saveAdditional(tag, r); tag.put("Items", items.serializeNBT(r)); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { super.loadAdditional(tag, r); items.deserializeNBT(r, tag.getCompound("Items")); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
