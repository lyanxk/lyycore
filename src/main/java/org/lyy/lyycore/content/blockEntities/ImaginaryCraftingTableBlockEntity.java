package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
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
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

public final class ImaginaryCraftingTableBlockEntity extends BlockEntity implements MenuProvider {
    private int progress;
    private ResourceLocation activeRecipe;
    private final ItemStackHandler items = new ItemStackHandler(9) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) {
            progress = 0;
            activeRecipe = null;
            setChanged();
        }
    };
    private final RecipeInput input = new RecipeInput() {
        @Override public ItemStack getItem(int slot) { return items.getStackInSlot(slot); }
        @Override public int size() { return 9; }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return progress; }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 1; }
    };

    public ImaginaryCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.IMAGINARY_CRAFTING_TABLE.get(), pos, state);
    }
    public ItemStackHandler items() { return items; }
    public ContainerData data() { return data; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImaginaryCraftingTableBlockEntity table) {
        var recipe = level.getRecipeManager().getAllRecipesFor(LyyRecipes.IMAGINARY_CRAFTING.get()).stream()
                .filter(holder -> holder.value().matches(table.input, level))
                .min(java.util.Comparator.comparing(holder -> holder.id().toString())).orElse(null);
        if (recipe == null) {
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
        if (++table.progress >= ImaginaryCraftingRecipe.DURATION) {
            ItemStack result = recipe.value().assemble(table.input, level.registryAccess());
            for (int slot = 0; slot < 9; slot++) table.items.setStackInSlot(slot, ItemStack.EMPTY);
            table.items.setStackInSlot(0, result);
        }
        table.setChanged();
    }

    @Override public Component getDisplayName() { return Component.translatable("block.lyycore.imaginary_crafting_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ImaginaryCraftingMenu(id, inventory, this, data);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        if (activeRecipe != null) tag.putString("Recipe", activeRecipe.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = Math.clamp(tag.getInt("Progress"), 0, ImaginaryCraftingRecipe.DURATION - 1);
        activeRecipe = ResourceLocation.tryParse(tag.getString("Recipe"));
    }
}
