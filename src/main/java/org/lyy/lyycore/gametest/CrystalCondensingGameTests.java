package org.lyy.lyycore.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.content.blocks.CrystalCondensingFrameBlock;
import org.lyy.lyycore.content.menu.CrystalCondensingMenu;
import org.lyy.lyycore.content.recipes.CrystalCondensingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyBlockEntities;
import org.lyy.lyycore.registry.LyyRecipes;

@GameTestHolder(LyyCore.MODID)
@PrefixGameTestTemplate(false)
public final class CrystalCondensingGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static CrystalCondensingFrameBlockEntity place(GameTestHelper h, boolean wet) {
        h.setBlock(POS, LyyBlocks.CRYSTAL_CONDENSING_FRAME.get().defaultBlockState()
                .setValue(CrystalCondensingFrameBlock.WATERLOGGED, wet));
        return h.getBlockEntity(POS);
    }
    private static void tick(GameTestHelper h, CrystalCondensingFrameBlockEntity be) {
        var ticker = be.getBlockState().getTicker(h.getLevel(), LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get());
        h.assertTrue(ticker != null, "Crystal frames must provide their registered server ticker");
        ticker.tick(h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }
    private static CrystalCondensingRecipe recipe(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get());
        h.assertTrue(recipes.size() == 1, "The built-in condensation recipe must load exactly once");
        return recipes.getFirst().value();
    }
    private static void restore(GameTestHelper h, CrystalCondensingFrameBlockEntity be, int energy, int count) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("IE", energy);
        tag.putInt("CrystalCount", count);
        if (count > 0) tag.put("Crystal", recipe(h).result().save(h.getLevel().registryAccess()));
        be.loadWithComponents(tag, h.getLevel().registryAccess());
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void legacyCrystalOutputMigratesWithoutLosingBulkCount(GameTestHelper h) {
        var be = place(h, false);
        restore(h, be, 0, 1024);
        var legacy = be.saveWithoutMetadata(h.getLevel().registryAccess());
        legacy.getCompound("Crystal").putString("id", "lyycore:raw_imaginium");
        var restored = new CrystalCondensingFrameBlockEntity(be.getBlockPos(), be.getBlockState());
        restored.loadWithComponents(legacy, h.getLevel().registryAccess());
        h.assertTrue(restored.getOutputCount() == 1024 && restored.getOutput().getStackInSlot(0)
                        .is(org.lyy.lyycore.registry.LyyItems.IMAGINARY_CRYSTAL.get()),
                "Legacy crystal output must migrate to imaginary_crystal while retaining all 1024 crystals");
        h.assertTrue(restored.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("Crystal")
                        .getString("id").equals("lyycore:imaginary_crystal"),
                "Migrated machine output must save with the canonical new item ID");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void costsAndPassiveGeneration(GameTestHelper h) {
        var be = place(h, false);
        var rec = recipe(h);
        h.assertTrue(rec.dryCost() == 200 && rec.wetCost() == 100, "Recipe must use the documented IE costs");
        be.getEnergyStorage().receiveEnergy(20_000, false);
        tick(h, be);
        h.assertTrue(be.getOutputCount() == 1 && be.getEnergyIE() == 0, "Dry production consumes 200 IE");
        restore(h, be, 100, 0);
        h.getLevel().setBlock(be.getBlockPos(), be.getBlockState().setValue(CrystalCondensingFrameBlock.WATERLOGGED, true), 3);
        tick(h, be);
        h.assertTrue(be.getOutputCount() == 1 && be.getEnergyIE() == 0, "Waterlogged production consumes 100 IE");
        h.assertTrue(be.getBlockState().getValue(CrystalCondensingFrameBlock.WATERLOGGED), "Production must not consume water");
        restore(h, be, 0, 0);
        for (int i = 0; i < 19; i++) tick(h, be);
        h.assertTrue(be.getEnergyIE() == 0, "Passive generation must wait 20 ticks");
        tick(h, be);
        h.assertTrue(be.getEnergyIE() == 1, "Passive generation supplies exactly 1 IE per second");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void energyInputOnlyAndRemainderPersist(GameTestHelper h) {
        var be = place(h, false);
        for (Direction side : Direction.values()) {
            var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), side);
            h.assertTrue(energy != null && energy.canReceive() && !energy.canExtract(), "All six faces must accept energy only");
            h.assertTrue(energy.extractEnergy(100, false) == 0, "External extraction must fail");
        }
        be.getEnergyStorage().receiveEnergy(99, false);
        h.assertTrue(be.getEnergyIE() == 0, "Incomplete FE must not round up");
        var restored = new CrystalCondensingFrameBlockEntity(be.getBlockPos(), be.getBlockState());
        restored.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        restored.getEnergyStorage().receiveEnergy(1, false);
        h.assertTrue(restored.getEnergyIE() == 1, "FE remainder survives reload");
        restore(h, be, Integer.MAX_VALUE - 1, 0);
        be.getEnergyStorage().receiveEnergy(99, false);
        h.assertTrue(be.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, true) == 1, "Only the final FE fits");
        h.assertTrue(be.getEnergyIE() == Integer.MAX_VALUE - 1, "Simulation must not mutate energy");
        h.assertTrue(be.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) == 1, "Overflow-sized input must be capped");
        h.assertTrue(be.getEnergyIE() == Integer.MAX_VALUE && be.getEnergyStorage().receiveEnergy(1, false) == 0, "Full store rejects input");
        var data = be.getData();
        h.assertTrue(((data.get(0) & 0xFFFF) | ((data.get(1) & 0xFFFF) << 16)) == Integer.MAX_VALUE, "Menu energy words must preserve all 31 bits");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bulkOutputSurvivesSaveAndStopsAt1024(GameTestHelper h) {
        var be = place(h, false);
        restore(h, be, 600, 1023);
        tick(h, be);
        tick(h, be);
        h.assertTrue(be.getOutputCount() == 1024 && be.getEnergyIE() == 400, "A full output must not consume energy");
        var restored = new CrystalCondensingFrameBlockEntity(be.getBlockPos(), be.getBlockState());
        restored.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(restored.getOutputCount() == 1024 && restored.getEnergyIE() == 400, "Bulk count and IE must survive NBT");
        h.assertTrue(restored.getOutput().extractItem(0, Integer.MAX_VALUE, true).getCount() == 64
                && restored.getOutputCount() == 1024, "Simulation exposes at most one normal stack");
        h.assertTrue(restored.getOutput().insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1,
                "Automation cannot insert into the output");
        int extracted = 0;
        while (restored.getOutputCount() > 0) extracted += restored.getOutput().extractItem(0, 64, false).getCount();
        h.assertTrue(extracted == 1024, "All 1024 crystals must be recoverable");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void outputUsesRelativeLeftAndRightAndRecoversAfterFull(GameTestHelper h) {
        var be = place(h, false);
        h.getLevel().setBlock(be.getBlockPos(), be.getBlockState().setValue(CrystalCondensingFrameBlock.FACING, Direction.EAST), 3);
        h.setBlock(POS.north(), Blocks.BARREL);
        h.setBlock(POS.south(), Blocks.BARREL);
        h.setBlock(POS.east(), Blocks.BARREL);
        BarrelBlockEntity left = h.getBlockEntity(POS.north());
        BarrelBlockEntity right = h.getBlockEntity(POS.south());
        BarrelBlockEntity front = h.getBlockEntity(POS.east());
        for (int i = 0; i < left.getContainerSize(); i++) left.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        for (int i = 0; i < right.getContainerSize(); i++) right.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        restore(h, be, 200, 1023);
        tick(h, be);
        h.assertTrue(be.getOutputCount() == 1024 && front.isEmpty(), "Full sides retain output; front is not an export target");
        right.setItem(0, ItemStack.EMPTY);
        for (int i = 0; i < 20; i++) tick(h, be);
        h.assertTrue(right.getItem(0).getCount() == 64 && be.getOutputCount() == 960, "A newly freed side accepts output without more production energy");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void menuPickupHotbarAndQuickMoveConserveBulkCount(GameTestHelper h) {
        var be = place(h, false);
        restore(h, be, 0, 1024);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new CrystalCondensingMenu(1, player.getInventory(), be, be.getOutput(), be.getData());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().getCount() == 64 && be.getOutputCount() == 960, "Pickup takes a legal stack");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 1, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().getCount() == 32 && be.getOutputCount() == 928, "Right click takes half of the exposed stack");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.SWAP, player);
        h.assertTrue(player.getInventory().getItem(0).getCount() == 64 && be.getOutputCount() == 864, "Hotbar swap must only remove its visible stack");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(be.getOutputCount() == 0, "Shift click drains bulk output into available inventory slots");
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            h.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Player inventory stacks must stay legal");
            count += stack.getCount();
        }
        h.assertTrue(count == 928, "Quick move must conserve all remaining crystals");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bucketsWaterlogWithoutReplacingTheBlockEntity(GameTestHelper h) {
        var be = place(h, false);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(be.getBlockPos()), Direction.UP, be.getBlockPos(), false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(be.getBlockState().getValue(CrystalCondensingFrameBlock.WATERLOGGED)
                && player.getMainHandItem().is(Items.BUCKET), "Water bucket adds water and returns an empty bucket");
        be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!be.getBlockState().getValue(CrystalCondensingFrameBlock.WATERLOGGED)
                && player.getMainHandItem().is(Items.WATER_BUCKET), "Empty bucket collects the water");
        h.assertTrue(h.getLevel().getBlockEntity(be.getBlockPos()) == be, "Water changes must preserve the machine");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!be.getBlockState().getValue(CrystalCondensingFrameBlock.WATERLOGGED)
                && player.getMainHandItem().is(Items.LAVA_BUCKET), "Other buckets must not modify water");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void breakingDropsEveryStoredCrystal(GameTestHelper h) {
        var be = place(h, false);
        restore(h, be, 0, 1024);
        var item = recipe(h).result().getItem();
        h.destroyBlock(POS);
        h.assertItemEntityCountIs(item, POS, 3, 1024);
        h.succeed();
    }
}
