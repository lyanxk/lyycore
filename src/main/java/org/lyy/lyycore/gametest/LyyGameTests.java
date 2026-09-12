package org.lyy.lyycore.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryAlloyForgeBlockEntity;
import org.lyy.lyycore.content.item.ImaginaryDisassemblerItem;
import org.lyy.lyycore.energy.IEnergyConversion;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.content.recipes.ImaginaryAlloyingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;

@GameTestHolder(LyyCore.MODID)
@PrefixGameTestTemplate(false)
public final class LyyGameTests {
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void sonnetArrowsKeepAmmoEffectsButRejectEnchantmentsAndPickup(GameTestHelper helper) {
        var bow = LyyItems.WHISPER_OF_THE_PAST.get();
        var weapon = new ItemStack(bow);
        var tipped = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.POISON);
        for (var ammo : java.util.List.of(new ItemStack(Items.ARROW), tipped, new ItemStack(Items.SPECTRAL_ARROW))) {
            net.minecraft.world.entity.projectile.AbstractArrow original = ammo.is(Items.SPECTRAL_ARROW)
                    ? new net.minecraft.world.entity.projectile.SpectralArrow(helper.getLevel(), 1, 2, 3, ammo, weapon)
                    : new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), 1, 2, 3, ammo, weapon);
            original.setCritArrow(true);
            original.setBaseDamage(7.5);
            var crystal = bow.customArrow(original, ammo, weapon);
            helper.assertTrue(ammo.is(Items.SPECTRAL_ARROW)
                            ? crystal instanceof org.lyy.lyycore.content.entity.SonnetArrow.Spectral
                            : crystal instanceof org.lyy.lyycore.content.entity.SonnetArrow,
                    "Partial shots retain their original arrow effect family");
            var before = original.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            var after = crystal.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            helper.assertTrue(before.getCompound("item").equals(after.getCompound("item")), "Ammo potion data must survive");
            helper.assertTrue(!crystal.isCritArrow() && crystal.getBaseDamage() == 2 && crystal.getPierceLevel() == 0,
                    "Weapon enchantments and critical bonuses must not leak into shots");
            helper.assertTrue(crystal.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED, "No arrows are collectible");
        }
        helper.assertTrue(!weapon.isDamageableItem() && !bow.isEnchantable(weapon), "Bow must have neither durability nor enchanting");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void imaginaryEnergySaturatesWithoutOverflow(GameTestHelper helper) {
        ImaginaryEnergyStorage storage = new ImaginaryEnergyStorage(1_000, 1_000, 1_000);
        storage.setImaginaryEnergy(Integer.MAX_VALUE - 5);
        helper.assertTrue(storage.receiveImaginaryEnergy(10) == 5, "Only remaining IE capacity should be accepted");
        helper.assertTrue(storage.getImaginaryEnergyStored() == Integer.MAX_VALUE, "IE must saturate at Integer.MAX_VALUE");
        helper.assertTrue(IEnergyConversion.toFE(Integer.MAX_VALUE) == Integer.MAX_VALUE, "FE conversion must saturate safely");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void energyCellSyncsFullWidthValues(GameTestHelper helper) {
        helper.setBlock(TEST_POS, LyyBlocks.IMAGINARY_ENERGY_CELL.get());
        EnergyCellBlockEntity cell = helper.getBlockEntity(TEST_POS);
        cell.getEnergyStorage().setImaginaryEnergy(Integer.MAX_VALUE);
        int low = cell.getData().get(0);
        int high = cell.getData().get(1);
        int reconstructed = low & 0xFFFF | (high & 0xFFFF) << 16;
        helper.assertTrue(reconstructed == Integer.MAX_VALUE, "Container data must retain all 32 IE bits");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void energyCellPreservesConversionRemainder(GameTestHelper helper) {
        helper.setBlock(TEST_POS, LyyBlocks.IMAGINARY_ENERGY_CELL.get());
        EnergyCellBlockEntity cell = helper.getBlockEntity(TEST_POS);
        cell.getEnergyStorage().receiveEnergy(99, false);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(cell.getEnergyStorage().getEnergyStored() == 99,
                    "Sub-100 FE remainder must stay buffered");
            helper.assertTrue(cell.getIEnergyStored() == 0, "Incomplete FE must not become IE");
            cell.getEnergyStorage().receiveEnergy(1, false);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(cell.getEnergyStorage().getEnergyStored() == 0,
                        "Exactly 100 FE must be consumed");
                helper.assertTrue(cell.getIEnergyStored() == 1, "Exactly 100 FE must become one IE");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void forgeDropsInventoryOnRemoval(GameTestHelper helper) {
        helper.setBlock(TEST_POS, LyyBlocks.IAF.get());
        ImaginaryAlloyForgeBlockEntity forge = helper.getBlockEntity(TEST_POS);
        forge.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        helper.destroyBlock(TEST_POS);
        helper.assertItemEntityCountIs(Items.DIAMOND, TEST_POS, 2.0, 3);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void energyCellDropsInventoryOnRemoval(GameTestHelper helper) {
        helper.setBlock(TEST_POS, LyyBlocks.IMAGINARY_ENERGY_CELL.get());
        EnergyCellBlockEntity cell = helper.getBlockEntity(TEST_POS);
        cell.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        helper.destroyBlock(TEST_POS);
        helper.assertItemEntityCountIs(Items.DIAMOND, TEST_POS, 2.0, 2);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void forgeOutputRejectsAutomationInsertion(GameTestHelper helper) {
        helper.setBlock(TEST_POS, LyyBlocks.IAF.get());
        ImaginaryAlloyForgeBlockEntity forge = helper.getBlockEntity(TEST_POS);
        ItemStack offered = new ItemStack(Items.DIAMOND);
        ItemStack remainder = forge.getAutomationItemHandler(null).insertItem(3, offered, false);
        helper.assertTrue(remainder.getCount() == 1, "Automation must not insert into the output slot");
        helper.assertTrue(forge.getItemHandler().getStackInSlot(3).isEmpty(), "Output slot must remain empty");
        int low = forge.getData().get(6);
        int high = forge.getData().get(7);
        helper.assertTrue((low & 0xFFFF | (high & 0xFFFF) << 16) == 1_000_000,
                "Forge capacity sync must retain all 32 bits");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void alloyingTimesAreSeconds(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var battery = recipes.byKey(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID,
                "imaginary_alloy/im_battery_alloying")).orElseThrow().value();
        var alloy = recipes.byKey(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID,
                "imaginary_alloy/imaginium_alloy")).orElseThrow().value();
        helper.assertTrue(battery instanceof ImaginaryAlloyingRecipe recipe && recipe.getProcessTime() == 100,
                "Battery recipe must take 100 seconds");
        helper.assertTrue(alloy instanceof ImaginaryAlloyingRecipe recipe && recipe.getProcessTime() == 200,
                "Alloy recipe must take 200 seconds");
        helper.assertTrue(ImaginaryAlloyForgeBlockEntity.calculateEnergySpentAtProgress(4_000, 4_000, 1_000) == 1_000,
                "Distributed forge cost must equal the recipe total on completion");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void collectorSkipsFullNearestContainer(GameTestHelper helper) {
        BlockPos fullChestPos = new BlockPos(2, 1, 1);
        BlockPos availableChestPos = new BlockPos(2, 1, 2);
        helper.setBlock(TEST_POS, LyyBlocks.ITEM_COLLECTOR.get());
        helper.setBlock(fullChestPos, net.minecraft.world.level.block.Blocks.BARREL);
        helper.setBlock(availableChestPos, net.minecraft.world.level.block.Blocks.BARREL);

        Container fullChest = (Container) helper.getBlockEntity(fullChestPos);
        for (int slot = 0; slot < fullChest.getContainerSize(); slot++) {
            fullChest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        helper.spawnItem(Items.DIAMOND, new BlockPos(1, 1, 2)).setNoGravity(true);
        helper.succeedWhen(() -> helper.assertContainerContains(availableChestPos, Items.DIAMOND));
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void generatorDiscoversTargetsWithoutInteraction(GameTestHelper helper) {
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(TEST_POS, LyyBlocks.IGB.get());
        helper.setBlock(targetPos, LyyBlocks.IAF.get());
        helper.succeedWhen(() -> {
            ImaginaryAlloyForgeBlockEntity forge = helper.getBlockEntity(targetPos);
            helper.assertTrue(forge.getEnergyStorage().getEnergyStored() >= 512,
                    "Generator must automatically discover and power nearby targets");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void disassemblerSpecialModesIgnoreCreativePlayers(GameTestHelper helper) {
        BlockPos neighbor = new BlockPos(2, 1, 1);
        helper.setBlock(TEST_POS, net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(neighbor, net.minecraft.world.level.block.Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack tool = new ItemStack(LyyItems.IMAGINARY_DISASSEMBLER.get());
        ImaginaryDisassemblerItem.setMode(tool, ImaginaryDisassemblerItem.Mode.AOE3);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        ((ImaginaryDisassemblerItem) tool.getItem()).mineBlock(tool, helper.getLevel(),
                helper.getBlockState(TEST_POS), helper.absolutePos(TEST_POS), player);
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, neighbor);
        helper.succeed();
    }

    private LyyGameTests() { }
}
