package org.lyy.lyycore.content.finality;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.*;

import org.lyy.lyycore.content.ItemProduction;
import org.lyy.lyycore.content.ChestLidAnimation;
import org.lyy.lyycore.energy.*;

public final class GenesisBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 1_000_000_000, COST = 10_000;
    public final ImaginaryEnergyStorage energy =
            new ImaginaryEnergyStorage(CAPACITY, CAPACITY, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe =
            new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);
    public final ItemStackHandler template =
            new ItemStackHandler(1) {
                @Override
                protected void onContentsChanged(int slot) {
                    useGhost = false;
                    setChanged();
                }
            };
    private ItemStack ghost = ItemStack.EMPTY;
    private boolean useGhost;
    public final ChestLidAnimation lidAnimation;
    private final java.util.Set<java.util.UUID> viewers = new java.util.HashSet<>();

    public GenesisBlockEntity(BlockPos pos, BlockState state) {
        super(FinalityContent.GENESIS_ENTITY.get(), pos, state);
        lidAnimation = new ChestLidAnimation(13, 10, 20, state.getValue(GenesisBlock.OPEN));
    }

    public void startOpen(Player player) {
        if (level == null || level.isClientSide || player.isSpectator()) return;
        viewers.add(player.getUUID());
        updateOpenState();
    }

    public void stopOpen(Player player) {
        if (level == null || level.isClientSide) return;
        viewers.remove(player.getUUID());
        updateOpenState();
    }

    private void updateOpenState() {
        boolean open = !viewers.isEmpty();
        if (!isRemoved() && getBlockState().getValue(GenesisBlock.OPEN) != open)
            level.setBlock(worldPosition, getBlockState().setValue(GenesisBlock.OPEN, open), 3);
    }

    public ItemStack ghost() {
        return ghost;
    }

    public ItemStack selected() {
        var sample = template.getStackInSlot(0);
        return !ghost.isEmpty() && (useGhost || sample.isEmpty()) ? ghost : sample;
    }

    public void ghost(ItemStack stack) {
        ghost = stack.copyWithCount(1);
        useGhost = !ghost.isEmpty();
        setChanged();
    }

    public boolean take(Player player, boolean stack) {
        var product = selected();
        if (product.isEmpty()) return false;
        int count =
                Math.min(
                        energy.getImaginaryEnergyStored() / COST,
                        stack ? product.getMaxStackSize() : 1);
        if (count == 0) return false;
        var remainder =
                ItemHandlerHelper.insertItemStacked(
                        new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(
                                player.getInventory()),
                        product.copyWithCount(count),
                        false);
        int accepted = count - remainder.getCount();
        energy.setImaginaryEnergy(energy.getImaginaryEnergyStored() - accepted * COST);
        return accepted > 0;
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, GenesisBlockEntity machine) {
        if (level.isClientSide) {
            machine.lidAnimation.tick(state.getValue(GenesisBlock.OPEN));
            return;
        }
        if (!machine.viewers.isEmpty() && level.getGameTime() % 20 == 0)
            machine.viewers.removeIf(id -> {
                var player = level.getPlayerByUUID(id);
                return player == null || !player.isAlive()
                        || !(player.containerMenu instanceof GenesisMenu menu) || menu.machine != machine;
            });
        machine.updateOpenState();
        if (level.hasNeighborSignal(pos))
            ItemProduction.output(
                    level,
                    pos,
                    state.getValue(HorizontalDirectionalBlock.FACING),
                    machine.selected(),
                    machine.energy,
                    COST);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.lyycore.genesis");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GenesisMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getImaginaryEnergyStored());
        tag.putInt("FeRemainder", fe.getRemainder());
        tag.put("Template", template.serializeNBT(registries));
        tag.putBoolean("UseGhost", useGhost);
        if (!ghost.isEmpty()) tag.put("Ghost", ghost.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setImaginaryEnergy(tag.getInt("Energy"));
        fe.setRemainder(tag.getInt("FeRemainder"));
        template.deserializeNBT(registries, tag.getCompound("Template"));
        useGhost = tag.getBoolean("UseGhost");
        ghost = ItemStack.parseOptional(registries, tag.getCompound("Ghost"));
    }
}
