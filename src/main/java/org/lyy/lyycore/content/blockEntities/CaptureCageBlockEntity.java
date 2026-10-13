package org.lyy.lyycore.content.blockEntities;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import org.lyy.lyycore.content.CapturedLoot;
import org.lyy.lyycore.content.menu.CaptureCageMenu;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.*;

public final class CaptureCageBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = Integer.MAX_VALUE;
    public final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(CAPACITY, CAPACITY, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, CAPACITY, 0);
    public final ItemStackHandler items = new ItemStackHandler(27) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() { return items.getSlots(); }
        public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        public ItemStack extractItem(int slot, int count, boolean simulate) { return items.extractItem(slot, count, simulate); }
        public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };
    private CompoundTag captured = new CompoundTag();
    private final List<ItemStack> pending = new ArrayList<>();
    private Mob lootEntity;
    private boolean attemptedEntity;
    private final ContainerData data = new ContainerData() {
        public int get(int index) { return index == 0 ? energy.getImaginaryEnergyStored() & 65535 : energy.getImaginaryEnergyStored() >>> 16; }
        public void set(int index, int value) { }
        public int getCount() { return 2; }
    };
    public CaptureCageBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.CAPTURE_CAGE.get(), pos, state); }
    public Component captiveName() { return EntityType.byString(captured.getString("id")).map(EntityType::getDescription).orElse(Component.translatable("gui.lyycore.capture_empty")); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CaptureCageBlockEntity cage) {
        cage.pushOutput();
        cage.flushPending();
        if (!cage.pending.isEmpty() || cage.captured.isEmpty()) return;
        int cost = cage.captured.getString("id").equals("minecraft:wither") ? 20_000_000 : 1_000_000;
        if (cage.energy.getImaginaryEnergyStored() < cost) return;
        if (!cage.attemptedEntity) {
            cage.attemptedEntity = true;
            var entity = EntityType.loadEntityRecursive(cage.captured.copy(), level, e -> e);
            if (entity instanceof Mob mob) cage.lootEntity = mob;
        }
        if (cage.lootEntity == null) return;
        var result = cost == 20_000_000 ? List.of(new ItemStack(Items.NETHER_STAR))
                : CapturedLoot.roll((ServerLevel)level, cage.lootEntity, pos.getCenter());
        cage.energy.setImaginaryEnergy(cage.energy.getImaginaryEnergyStored() - cost);
        cage.pending.addAll(result);
        cage.flushPending();
        cage.setChanged();
    }
    private void flushPending() {
        if (pending.isEmpty()) return;
        for (var iterator = pending.listIterator(); iterator.hasNext();) {
            var rest = ItemHandlerHelper.insertItemStacked(items, iterator.next(), false);
            if (rest.isEmpty()) iterator.remove(); else iterator.set(rest);
        }
        setChanged();
    }
    private void pushOutput() {
        for (var side : new Direction[]{Direction.DOWN, Direction.UP}) {
            var pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            var output = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side.getOpposite());
            if (output == null || output == automation) continue;
            for (int i = 0; i < items.getSlots(); i++) {
                var stack = items.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                var rest = ItemHandlerHelper.insertItemStacked(output, stack.copy(), false);
                items.extractItem(i, stack.getCount() - rest.getCount(), false);
            }
        }
    }
    public ItemStack asItem() {
        var stack = new ItemStack(LyyItems.CAPTURE_CAGE.get());
        BlockItem.setBlockEntityData(stack, getType(), saveWithoutMetadata(level.registryAccess()));
        return stack;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.capture_cage"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new CaptureCageMenu(id, inv, this, data, captiveName()); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registry) {
        super.saveAdditional(tag, registry);
        tag.put("Captured", captured.copy()); tag.put("Items", items.serializeNBT(registry));
        tag.putInt("Energy", energy.getImaginaryEnergyStored()); tag.putInt("FeRemainder", fe.getRemainder());
        var list = new ListTag(); for (var stack : pending) if (!stack.isEmpty()) list.add(stack.save(registry)); tag.put("Pending", list);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registry) {
        super.loadAdditional(tag, registry);
        captured = tag.getCompound("Captured").copy(); items.deserializeNBT(registry, tag.getCompound("Items"));
        energy.setImaginaryEnergy(tag.getInt("Energy")); fe.setRemainder(tag.getInt("FeRemainder"));
        pending.clear(); for (var entry : tag.getList("Pending", Tag.TAG_COMPOUND)) ItemStack.parse(registry, entry).ifPresent(pending::add);
        lootEntity = null; attemptedEntity = false;
    }
}
