package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.recipes.OctagonalRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractImaginaryCraftingBlockEntity extends BlockEntity implements MenuProvider {
    private int progress, duration = 100, recipeEnergy;
    private ResourceLocation activeRecipe;
    private ResourceLocation completedRecipe;
    private RecipeManager cachedManager;
    private RecipeHolder<? extends OctagonalRecipe> cachedRecipe;
    private boolean recipeDirty = true;
    private final ItemStackHandler items = new ItemStackHandler(9) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) {
            progress = 0;
            activeRecipe = null;
            recipeDirty = true;
            setChanged();
        }
    };
    private final RecipeInput input = new RecipeInput() {
        @Override public ItemStack getItem(int slot) { return items.getStackInSlot(slot); }
        @Override public int size() { return 9; }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress; case 1 -> duration;
                case 2 -> recipeEnergy & 0xFFFF; case 3 -> recipeEnergy >>> 16;
                case 4 -> storedEnergy() & 0xFFFF; case 5 -> storedEnergy() >>> 16;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 6; }
    };

    protected AbstractImaginaryCraftingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    protected abstract RecipeHolder<? extends OctagonalRecipe> findRecipe(RecipeInput input);
    protected int storedEnergy() { return 0; }
    protected boolean consumeEnergy(int amount, boolean simulate) { return amount == 0; }
    public ItemStackHandler items() { return items; }
    public ContainerData data() { return data; }

    /** Like a furnace, crafting credit goes to the player taking the actual result. */
    public void awardCraftedResult(Player player, ItemStack taken) {
        if (!(player instanceof ServerPlayer server) || completedRecipe == null) return;
        var recipe = level.getRecipeManager().byKey(completedRecipe).orElse(null);
        if (recipe != null && recipe.value() instanceof OctagonalRecipe crafting
                && ItemStack.isSameItemSameComponents(taken, crafting.result()))
            CriteriaTriggers.RECIPE_CRAFTED.trigger(server, completedRecipe, java.util.List.of());
        completedRecipe = null;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractImaginaryCraftingBlockEntity table) {
        var manager = level.getRecipeManager();
        boolean reloaded = false;
        if (table.cachedManager != manager || table.cachedRecipe != null
                && manager.byKey(table.cachedRecipe.id()).orElse(null) != table.cachedRecipe) {
            reloaded = table.cachedManager != null;
            table.cachedManager = manager;
            table.recipeDirty = true;
        }
        if (table.recipeDirty) {
            var previous = table.cachedRecipe;
            table.cachedRecipe = table.findRecipe(table.input);
            table.recipeDirty = false; // Cache misses as well as successful matches.
            if (reloaded && !sameProcessing(previous, table.cachedRecipe)) {
                table.progress = 0;
                table.activeRecipe = null;
                table.setChanged();
            }
        }
        var recipe = table.cachedRecipe;
        if (recipe == null) {
            table.recipeEnergy = 0;
            table.duration = 100;
            if (table.progress != 0) {
                table.progress = 0;
                table.activeRecipe = null;
                table.setChanged();
            }
            return;
        }
        if (!recipe.id().equals(table.activeRecipe)) {
            table.activeRecipe = recipe.id();
            table.progress = 0;
        }
        table.duration = recipe.value().duration();
        table.recipeEnergy = recipe.value().energy();
        if (!table.consumeEnergy(table.recipeEnergy, true)) return;
        if (++table.progress >= table.duration) {
            table.consumeEnergy(table.recipeEnergy, false);
            ItemStack result = recipe.value().assemble(table.input, level.registryAccess());
            for (int slot = 0; slot < 9; slot++) table.items.setStackInSlot(slot, ItemStack.EMPTY);
            table.items.setStackInSlot(0, result);
            table.completedRecipe = recipe.id();
        }
        table.setChanged();
    }
    private static boolean sameProcessing(RecipeHolder<? extends OctagonalRecipe> before, RecipeHolder<? extends OctagonalRecipe> after) {
        return before != null && after != null && before.id().equals(after.id())
                && before.value().duration() == after.value().duration() && before.value().energy() == after.value().energy()
                && ItemStack.matches(before.value().result(), after.value().result());
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ImaginaryCraftingMenu(id, inventory, this, data);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        if (activeRecipe != null) tag.putString("Recipe", activeRecipe.toString());
        if (completedRecipe != null) tag.putString("CompletedRecipe", completedRecipe.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cachedManager = null;
        cachedRecipe = null;
        recipeDirty = true;
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = Math.clamp(tag.getInt("Progress"), 0, 32766);
        activeRecipe = ResourceLocation.tryParse(tag.getString("Recipe"));
        completedRecipe = ResourceLocation.tryParse(tag.getString("CompletedRecipe"));
    }
}
