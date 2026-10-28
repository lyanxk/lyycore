package org.lyy.lyycore.content.blockEntities;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.lyy.lyycore.content.ProductionCatalog;
import org.lyy.lyycore.content.blocks.OtherworldChestBlock;
import org.lyy.lyycore.content.menu.OtherworldChestMenu;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** Energy-backed production: charge only for items actually delivered. No hidden output inventory. */
public final class OtherworldChestBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 1_000_000_000, COST = 10_000;
    public final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(CAPACITY, CAPACITY, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);
    private ItemStack selection = ItemStack.EMPTY;
    private List<ItemStack> cachedCatalog;
    private int selectedIndex = -1;
    private final java.util.Set<java.util.UUID> viewers = new java.util.HashSet<>();
    // Client-local animation progress, matching the supplied 0.75 s / 0.6 s clips.
    private static final int OPEN_TICKS = 15, CLOSE_TICKS = 12, IDLE_TICKS = 80;
    private boolean animationOpen;
    private int animationTick, previousAnimationTick, idleTick;

    public static void clientTick(Level level, BlockPos pos, BlockState state, OtherworldChestBlockEntity chest) {
        boolean open = state.getValue(OtherworldChestBlock.OPEN);
        if (open != chest.animationOpen) {
            chest.animationOpen = open;
            chest.animationTick = chest.idleTick = 0;
        }
        chest.previousAnimationTick = chest.animationTick;
        int duration = open ? OPEN_TICKS : CLOSE_TICKS;
        if (chest.animationTick < duration) chest.animationTick++;
        else chest.idleTick = chest.idleTick % IDLE_TICKS + 1;
    }

    public boolean animationOpen() { return animationOpen; }
    public float animationProgress(float partial) {
        return Mth.lerp(partial, previousAnimationTick, animationTick) / (animationOpen ? OPEN_TICKS : CLOSE_TICKS);
    }
    public float idleAnimationTime(float partial) { return Math.max(0, idleTick - 1 + partial) / 20F; }
    public void startOpen(Player player) {
        if (level == null || level.isClientSide) return;
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
        if (!isRemoved() && getBlockState().getValue(OtherworldChestBlock.OPEN) != open)
            level.setBlock(worldPosition, getBlockState().setValue(OtherworldChestBlock.OPEN, open), 3);
    }
    private final ContainerData data = new ContainerData() {
        public int get(int index) { return index < 2 ? energy.getImaginaryEnergyStored() >>> (index * 16) & 65535 : selectedIndex(); }
        public void set(int index, int value) { }
        public int getCount() { return 3; }
    };
    public OtherworldChestBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.OTHERWORLD_CHEST.get(), pos, state);
        animationOpen = state.getValue(OtherworldChestBlock.OPEN);
        animationTick = previousAnimationTick = animationOpen ? OPEN_TICKS : CLOSE_TICKS;
    }
    public List<ItemStack> products() { return ProductionCatalog.products(level); }
    public int selectedIndex() {
        var catalog = products();
        if (catalog != cachedCatalog) {
            cachedCatalog = catalog; selectedIndex = -1;
            for (int i = 0; i < catalog.size(); i++) {
                var candidate = catalog.get(i);
                if (!ItemStack.isSameItemSameComponents(candidate, selection)) continue;
                selectedIndex = i;
                break;
            }
        }
        return selectedIndex;
    }
    public boolean select(int index) {
        var catalog = products();
        if (index < 0 || index >= catalog.size()) return false;
        var next = catalog.get(index);
        if (!ItemStack.isSameItemSameComponents(selection, next)) {
            selection = next.copyWithCount(1); setChanged();
        }
        cachedCatalog = catalog; selectedIndex = index;
        return true;
    }
    private ItemStack selected() {
        int index = selectedIndex(); return index < 0 ? ItemStack.EMPTY : products().get(index);
    }
    private int affordable() { return energy.getImaginaryEnergyStored() / COST; }
    private void charge(int count) { if (count > 0) energy.setImaginaryEnergy(energy.getImaginaryEnergyStored() - count * COST); }
    public boolean take(Player player, boolean stack) {
        var product = selected();
        if (product.isEmpty()) return false;
        int count = Math.min(affordable(), stack ? product.getMaxStackSize() : 1);
        if (count == 0) return false;
        var remaining = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(
                new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), product.copyWithCount(count), false);
        int accepted = count - remaining.getCount();
        charge(accepted);
        if (accepted > 0) player.inventoryMenu.broadcastChanges();
        return accepted > 0;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, OtherworldChestBlockEntity chest) {
        // Only inspect this chest's viewers; disconnected players must not leave the lid open.
        if (!chest.viewers.isEmpty() && level.getGameTime() % 20 == 0) chest.viewers.removeIf(id -> {
            var player = level.getPlayerByUUID(id);
            return player == null || !player.isAlive() || !(player.containerMenu instanceof OtherworldChestMenu menu) || !menu.isFor(chest);
        });
        chest.updateOpenState();
        if (!level.hasNeighborSignal(pos) || chest.affordable() == 0) return;
        var product = chest.selected();
        if (product.isEmpty()) return;
        org.lyy.lyycore.content.ItemProduction.output(level,pos,state.getValue(OtherworldChestBlock.FACING),product,chest.energy,COST);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.otherworld_chest"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new OtherworldChestMenu(id, inv, this, data); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putInt("Energy", energy.getImaginaryEnergyStored());
        tag.putInt("FeRemainder", fe.getRemainder());
        if (!selection.isEmpty()) tag.put("SelectedProduct", selection.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); energy.setImaginaryEnergy(tag.getInt("Energy"));
        fe.setRemainder(tag.getInt("FeRemainder"));
        selection = ItemStack.parseOptional(registries, tag.getCompound("SelectedProduct"));
        cachedCatalog = null; selectedIndex = -1;
    }
}
