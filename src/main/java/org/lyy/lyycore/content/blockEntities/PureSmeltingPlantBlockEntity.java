package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import org.lyy.lyycore.content.blocks.*;
import org.lyy.lyycore.content.menu.PureSmeltingMenu;
import org.lyy.lyycore.content.recipes.PureSmeltingRecipe;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.*;

public final class PureSmeltingPlantBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BASE_IE_PER_TICK = 500;
    private int speed = 1, progress, outputCount;
    private ItemStack output = ItemStack.EMPTY;
    private boolean dirtyRecipe = true, working;
    private RecipeManager recipeManager;
    private RecipeHolder<PureSmeltingRecipe> recipe;
    private String recipeId = "";
    public final ImaginaryEnergyStorage energy = new ImaginaryEnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, this::setChanged);
    public final ImaginaryEnergyFeAdapter fe = new ImaginaryEnergyFeAdapter(energy, this::setChanged, Integer.MAX_VALUE, 0);
    public final ItemStackHandler inputs = new ItemStackHandler(9) {
        @Override protected void onContentsChanged(int slot) { dirtyRecipe = true; progress = 0; sync(); }
    };
    private final RecipeInput input = new RecipeInput() {
        public ItemStack getItem(int slot) { return inputs.getStackInSlot(slot); }
        public int size() { return 9; }
    };
    /** Output uses an explicit count, so vanilla stack codecs never have to encode a 256-stack. */
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() { return 10; }
        public ItemStack getStackInSlot(int slot) { return slot == 9 ? output.copyWithCount(outputCount) : inputs.getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot == 9 ? stack : inputs.insertItem(slot, stack, simulate); }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 9 || amount <= 0 || outputCount == 0) return ItemStack.EMPTY;
            int take = Math.min(Math.min(amount, outputCount), output.getMaxStackSize());
            var result = output.copyWithCount(take);
            if (!simulate) { outputCount -= take; if (outputCount == 0) output = ItemStack.EMPTY; sync(); }
            return result;
        }
        public int getSlotLimit(int slot) { return slot == 9 ? 256 : 64; }
        public boolean isItemValid(int slot, ItemStack stack) { return slot < 9; }
    };
    public final ContainerData data = new ContainerData() {
        public int get(int index) { return switch (index) { case 0 -> speed; case 1 -> energy.getImaginaryEnergyStored() & 65535; case 2 -> energy.getImaginaryEnergyStored() >>> 16; default -> 0; }; }
        public void set(int index, int value) { }
        public int getCount() { return 3; }
    };
    public PureSmeltingPlantBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.PURE_SMELTING_PLANT.get(), pos, state); }
    public int speed() { return speed; }
    public boolean working() { return working; }
    public boolean active() { return getBlockState().getBlock() instanceof PureSmeltingPlantBlock block && block.active(); }
    public void speed(int value) { speed = Math.clamp(value, 1, 100); sync(); }
    public void interact(ServerPlayer player, ItemStack held) {
        if (held.isEmpty()) {
            for (int slot = 8; slot >= 0; slot--) if (!inputs.getStackInSlot(slot).isEmpty()) {
                player.getInventory().placeItemBackInInventory(inputs.extractItem(slot, 64, false)); return;
            }
            player.getInventory().placeItemBackInInventory(automation.extractItem(9, 64, false));
        } else {
            var remaining = ItemHandlerHelper.insertItemStacked(inputs, held.copy(), false);
            held.setCount(remaining.getCount());
        }
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, PureSmeltingPlantBlockEntity plant) {
        if (!plant.active()) return;
        if (level.getGameTime() % 8 == 0) plant.pushOutput();
        if (plant.recipeManager != level.getRecipeManager()) {
            if (plant.recipeManager != null) plant.progress = 0;
            plant.recipeManager = level.getRecipeManager(); plant.dirtyRecipe = true;
        }
        if (plant.dirtyRecipe) {
            plant.recipe = level.getRecipeManager().getRecipeFor(LyyRecipes.PURE_SMELTING.get(), plant.input, level).orElse(null);
            String id = plant.recipe == null ? "" : plant.recipe.id().toString();
            if (!id.equals(plant.recipeId)) plant.progress = 0;
            plant.recipeId = id; plant.dirtyRecipe = false;
        }
        boolean running = false;
        if (plant.recipe != null) {
            var r = plant.recipe.value();
            int cost = BASE_IE_PER_TICK * plant.speed * plant.speed;
            boolean room = plant.outputCount + r.resultCount() <= 256
                    && (plant.output.isEmpty() || ItemStack.isSameItemSameComponents(plant.output, r.result()));
            if (room && plant.energy.getImaginaryEnergyStored() >= cost) {
                running = true;
                plant.energy.setImaginaryEnergy(plant.energy.getImaginaryEnergyStored() - cost);
                plant.progress += plant.speed;
                if (plant.progress >= r.duration()) {
                    int[] slots = r.assignment(plant.input);
                    if (slots != null) {
                        for (int i = 0; i < slots.length; i++) plant.inputs.extractItem(slots[i], r.inputs().get(i).count(), false);
                        plant.output = r.result().copyWithCount(1); plant.outputCount += r.resultCount();
                    }
                    plant.progress = 0; plant.dirtyRecipe = true; plant.sync();
                }
                plant.setChanged();
            }
        }
        if (plant.working != running) { plant.working = running; plant.sync(); }
    }
    private void pushOutput() {
        if (outputCount == 0) return;
        var facing = getBlockState().getValue(LargeStructureBlock.FACING);
        var target = worldPosition.relative(facing, 2);
        if (!level.hasChunkAt(target)) return;
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, facing.getOpposite());
        if (handler == null) return;
        var stack = automation.extractItem(9, 64, true);
        int accepted = stack.getCount() - ItemHandlerHelper.insertItemStacked(handler, stack, false).getCount();
        automation.extractItem(9, accepted, false);
    }
    public void dropContents() {
        for (int slot = 0; slot < 9; slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inputs.getStackInSlot(slot));
        while (outputCount > 0) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), automation.extractItem(9, 64, false));
    }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inputs", inputs.serializeNBT(registries));
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
        tag.putInt("OutputCount", outputCount); tag.putInt("Speed", speed); tag.putInt("Progress", progress);
        tag.putString("Recipe", recipeId); tag.putInt("IE", energy.getImaginaryEnergyStored()); tag.putByte("FE", fe.getRemainder()); tag.putBoolean("Working", working);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        outputCount = output.isEmpty() ? 0 : Math.clamp(tag.getInt("OutputCount"), 0, 256);
        speed = Math.clamp(tag.getInt("Speed"), 1, 100); progress = Math.clamp(tag.getInt("Progress"), 0, 72000);
        recipeId = tag.getString("Recipe"); energy.setImaginaryEnergy(tag.getInt("IE")); fe.setRemainder(tag.getByte("FE")); working = tag.getBoolean("Working"); dirtyRecipe = true;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.pure_smelting_plant"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new PureSmeltingMenu(id, inventory, this, data); }
}
