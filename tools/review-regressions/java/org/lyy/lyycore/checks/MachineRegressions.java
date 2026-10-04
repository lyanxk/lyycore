package org.lyy.lyycore.checks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryAlloyForgeBlockEntity;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyCapabilities;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class MachineRegressions {
    @GameTest(template = "empty")
    public static void energyCapabilitiesShareOneBalance(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos, LyyBlocks.IMAGINARY_ENERGY_CELL.get().defaultBlockState());
        var cell = (EnergyCellBlockEntity) level.getBlockEntity(pos);
        var nativeIE = level.getCapability(LyyCapabilities.IMAGINARY_ENERGY, pos, null);
        var fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        test.assertTrue(nativeIE != null && fe != null, "Missing energy capabilities");
        nativeIE.receiveImaginaryEnergy(1000, false);
        test.assertTrue(fe.getEnergyStored() == 100000, "FE cannot see native IE");
        test.assertTrue(fe.extractEnergy(150, true) == 150 && nativeIE.getImaginaryEnergyStored() == 1000,
                "Simulated extraction changed energy");
        test.assertTrue(fe.extractEnergy(150, false) == 150 && nativeIE.getImaginaryEnergyStored() == 998
                && fe.getEnergyStored() == 99850, "Fractional extraction lost or duplicated energy");
        var saved = cell.saveWithoutMetadata(level.registryAccess());
        var restored = new EnergyCellBlockEntity(pos, cell.getBlockState());
        restored.loadWithComponents(saved, level.registryAccess());
        test.assertTrue(restored.getEnergyStorage().getEnergyStored() == 99850, "Fractional energy did not survive save/load");
        test.assertTrue(fe.receiveEnergy(50, true) == 50 && fe.getEnergyStored() == 99850, "Simulated input changed energy");
        fe.receiveEnergy(50, false);
        test.assertTrue(nativeIE.getImaginaryEnergyStored() == 999 && fe.getEnergyStored() == 99900, "FE input did not restore whole IE");
        nativeIE.extractImaginaryEnergy(999, false);
        test.assertTrue(fe.getEnergyStored() == 0, "Native extraction left duplicate FE");
        fe.receiveEnergy(99, false);
        test.assertTrue(fe.extractEnergy(99, false) == 99 && fe.getEnergyStored() == 0, "Sub-IE transfers failed");
        nativeIE.receiveImaginaryEnergy(Integer.MAX_VALUE, false);
        test.assertTrue(fe.getEnergyStored() == Integer.MAX_VALUE && fe.receiveEnergy(1, false) == 0, "Full store overflowed");
        test.assertTrue(fe.extractEnergy(Integer.MAX_VALUE, true) == 1_000_000_000
                && nativeIE.getImaginaryEnergyStored() == Integer.MAX_VALUE, "FE transfer limit or simulation changed");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void energyLegacySaveConservesBalance(GameTestHelper test) {
        var registries = test.getLevel().registryAccess();
        var cell = new EnergyCellBlockEntity(BlockPos.ZERO, LyyBlocks.IMAGINARY_ENERGY_CELL.get().defaultBlockState());
        var tag = new CompoundTag();
        tag.putInt("IE", 1000);
        tag.putInt("energy", 125);
        cell.loadWithComponents(tag, registries);
        test.assertTrue(cell.getIEnergyStored() == 1001 && cell.getEnergyStorage().getEnergyStored() == 100125,
                "Legacy FE buffer failed to migrate");
        tag.putInt("IE", Integer.MAX_VALUE);
        tag.putInt("energy", 1_000_000_000);
        cell.loadWithComponents(tag, registries);
        long before = Integer.MAX_VALUE * 100L + 1_000_000_000L;
        cell.getEnergyStorage().extractEnergy(150, false);
        var saved = cell.saveWithoutMetadata(registries);
        test.assertTrue(saved.getInt("IE") * 100L + saved.getInt("energy") == before - 150,
                "Full legacy store lost buffered FE during extraction");
        cell.loadWithComponents(saved, registries);
        var again = cell.saveWithoutMetadata(registries);
        test.assertTrue(again.getInt("IE") * 100L + again.getInt("energy") == before - 150,
                "Repeated legacy migration lost or duplicated energy");
        test.succeed();
    }

    private static ImaginaryAlloyForgeBlockEntity forge(GameTestHelper test, int x, ImaginaryAlloyingRecipe recipe) {
        var pos = test.absolutePos(new BlockPos(x, 1, 2));
        var level = test.getLevel();
        level.setBlockAndUpdate(pos, LyyBlocks.IAF.get().defaultBlockState());
        var forge = (ImaginaryAlloyForgeBlockEntity) level.getBlockEntity(pos);
        forge.getImaginaryEnergyStorage().receiveImaginaryEnergy(10000, false);
        int slot = 0;
        for (var input : recipe.getInputs()) if (!input.isEmpty())
            forge.getItemHandler().setStackInSlot(slot++, input.getItems()[0].copyWithCount(2));
        if (recipe.getCatalyst() != null && !recipe.getCatalyst().isEmpty())
            forge.getItemHandler().setStackInSlot(2, recipe.getCatalyst().getItems()[0].copyWithCount(2));
        return forge;
    }

    private static void tick(ImaginaryAlloyForgeBlockEntity forge, int count) {
        var level = forge.getLevel();
        for (int i = 0; i < count; i++)
            ImaginaryAlloyForgeBlockEntity.serverTick(level, forge.getBlockPos(), level.getBlockState(forge.getBlockPos()), forge);
    }

    @GameTest(template = "empty")
    public static void overlappingIngredientsMatchAndConsumeBothOrders(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var recipe = new ImaginaryAlloyingRecipe(List.of(Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT),
                Ingredient.of(Items.IRON_INGOT)), Ingredient.of(Items.COAL), true, new ItemStack(Items.DIAMOND), 1, 20);
        var recipes = new ArrayList<RecipeHolder<?>>(original);
        recipes.add(new RecipeHolder<>(ResourceLocation.parse("lyycore:regression_overlap"), recipe));
        try {
            manager.replaceRecipes(recipes);
            var forge = forge(test, 2, recipe);
            for (boolean reverse : new boolean[]{false, true}) {
                var items = forge.getItemHandler();
                items.setStackInSlot(0, new ItemStack(reverse ? Items.GOLD_INGOT : Items.IRON_INGOT, 2));
                items.setStackInSlot(1, new ItemStack(reverse ? Items.IRON_INGOT : Items.GOLD_INGOT, 2));
                items.setStackInSlot(2, new ItemStack(Items.COAL, 2));
                items.setStackInSlot(3, ItemStack.EMPTY);
                tick(forge, 20);
                test.assertTrue(items.getStackInSlot(3).is(Items.DIAMOND), "Unordered recipe failed in one order");
                for (int slot = 0; slot < 3; slot++) test.assertTrue(items.getStackInSlot(slot).getCount() == 1,
                        "Incorrect input/catalyst consumption in slot " + slot);
            }
            var insufficient = new RecipeInput() {
                public ItemStack getItem(int i) { return i == 0 ? new ItemStack(Items.IRON_INGOT) : i == 2 ? new ItemStack(Items.COAL) : ItemStack.EMPTY; }
                public int size() { return 3; }
            };
            test.assertFalse(recipe.matches(insufficient, level), "One item satisfied two ingredients");
        } finally { manager.replaceRecipes(original); }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "alloy_reload", timeoutTicks = 400)
    public static void realReloadRefreshesIdleAndActiveForges(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var id = ResourceLocation.parse("lyycore:imaginary_alloy/imaginium_alloy");
        var recipe = (ImaginaryAlloyingRecipe) manager.byKey(id).orElseThrow().value();
        var withoutRecipe = original.stream().filter(entry -> !entry.id().equals(id)).toList();
        try {
            manager.replaceRecipes(withoutRecipe);
            var idle = forge(test, 1, recipe);
            tick(idle, 1);
            test.assertTrue(idle.getData().get(0) == 0 && idle.getData().get(2) == 0, "Idle fixture unexpectedly matched");
            manager.replaceRecipes(original);
            var unchanged = forge(test, 2, recipe);
            tick(unchanged, 5);
            var modified = new ImaginaryAlloyingRecipe(recipe.getInputs(), recipe.getCatalyst(), recipe.isCatalystConsumed(),
                    new ItemStack(Items.DIAMOND), 3, 10);
            var altered = new ArrayList<RecipeHolder<?>>(withoutRecipe);
            altered.add(new RecipeHolder<>(id, modified));
            var removedRecipe = new ImaginaryAlloyingRecipe(List.of(Ingredient.of(Items.DIRT)), null, false,
                    new ItemStack(Items.EMERALD), 3, 10);
            altered.add(new RecipeHolder<>(ResourceLocation.parse("lyycore:regression_removed"), removedRecipe));
            manager.replaceRecipes(altered);
            var changed = forge(test, 3, modified);
            var removed = forge(test, 4, removedRecipe);
            tick(changed, 5);
            tick(removed, 5);
            level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds()).join();
            tick(idle, 1); tick(unchanged, 1); tick(changed, 1); tick(removed, 1);
            test.assertTrue(idle.getData().get(0) == 1, "New matching recipe did not wake an idle forge");
            test.assertTrue(unchanged.getData().get(0) == 6, "Unchanged recipe lost its progress");
            test.assertTrue(changed.getData().get(0) == 1 && changed.getData().get(2) == recipe.getProcessTime() * 20,
                    "Changed definition retained stale progress/duration");
            test.assertTrue(removed.getData().get(0) == 0 && removed.getData().get(2) == 0
                    && removed.getItemHandler().getStackInSlot(3).isEmpty(), "Removed recipe continued processing");
        } finally { manager.replaceRecipes(original); }
        test.succeed();
    }
}
