package org.lyy.lyycore.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.blockEntities.ResourceGatheringFrameBlockEntity;
import org.lyy.lyycore.content.blocks.ResourceGatheringFrameBlock;
import org.lyy.lyycore.content.menu.ResourceGatheringMenu;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.registry.LyyBlockEntities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@GameTestHolder(LyyCore.MODID)
@PrefixGameTestTemplate(false)
public final class ResourceGatheringGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static ResourceGatheringFrameBlockEntity place(GameTestHelper h, ResourceFrameKind kind, boolean wet) {
        var state = kind.block().defaultBlockState();
        if (kind.supportsWater()) state = state.setValue(ResourceGatheringFrameBlock.WATERLOGGED, wet);
        h.setBlock(POS, state);
        return h.getBlockEntity(POS);
    }
    private static void tick(GameTestHelper h, ResourceGatheringFrameBlockEntity be) {
        var ticker = be.getBlockState().getTicker(h.getLevel(), LyyBlockEntities.RESOURCE_GATHERING_FRAME.get());
        h.assertTrue(ticker != null, "Every resource frame must provide its registered server ticker");
        ticker.tick(h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }
    private static void energy(GameTestHelper h, ResourceGatheringFrameBlockEntity be, int ie) {
        var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putInt("IE", ie);
        tag.putInt("PassiveTicks", 0);
        be.loadWithComponents(tag, h.getLevel().registryAccess());
    }
    private static void bulk(GameTestHelper h, ResourceGatheringFrameBlockEntity be, ItemStack item, int count) {
        var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.put("Crystal", item.copyWithCount(1).save(h.getLevel().registryAccess()));
        tag.putInt("CrystalCount", count);
        be.loadWithComponents(tag, h.getLevel().registryAccess());
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fourIndependentRecipePoolsAndCoreTags(GameTestHelper h) {
        var types = new HashSet<>();
        var serializers = new HashSet<>();
        int[] expected = {9, 25, 9, 19};
        for (var kind : ResourceFrameKind.values()) {
            var recipes = ResourceGatheringFrameBlockEntity.recipes(h.getLevel(), kind);
            h.assertTrue(recipes.size() == expected[kind.ordinal()], "All documented default outputs must load for " + kind);
            types.add(kind.recipeType()); serializers.add(kind.serializer());
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "gathering/" + kind.id() + "_products"));
            var outputs = new HashSet<net.minecraft.world.item.Item>();
            for (var holder : recipes) {
                var recipe = holder.value();
                h.assertTrue(recipe.kind() == kind && recipe.getType() == kind.recipeType(), "Recipe pool cannot cross frame categories");
                h.assertTrue(recipe.result().is(tag), "Every produced resource must be a valid core ingredient");
                h.assertTrue(recipe.energyCost(false) == 200 && recipe.energyCost(true) == (kind.supportsWater() ? 100 : 200), "Correct dry/wet costs");
                outputs.add(recipe.result().getItem());
                var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
                try {
                    kind.serializer().streamCodec().encode(buf, recipe);
                    var decoded = kind.serializer().streamCodec().decode(buf);
                    h.assertTrue(decoded.kind() == kind && ItemStack.isSameItemSameComponents(decoded.result(), recipe.result())
                            && decoded.dryCost() == recipe.dryCost() && decoded.wetCost() == recipe.wetCost(), "Recipe sync preserves category, item and costs");
                } finally { buf.release(); }
            }
            var tagItems = h.getLevel().registryAccess().registryOrThrow(Registries.ITEM).getTag(tag).orElseThrow();
            h.assertTrue(tagItems.size() == outputs.size(), "Core ingredients and production range must agree exactly");
        }
        h.assertTrue(types.size() == 4 && serializers.size() == 4, "Four independent registered types and serializers are required");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mixedCoresAndEightAlloyFrameCrafts(GameTestHelper h) {
        for (var kind : ResourceFrameKind.values()) {
            var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, kind.coreId())).orElseThrow();
            CraftingRecipe core = (CraftingRecipe) holder.value();
            var production = ResourceGatheringFrameBlockEntity.recipes(h.getLevel(), kind);
            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < 9; i++) inputs.add(production.get(i).value().result());
            h.assertTrue(core.matches(CraftingInput.of(3, 3, inputs), h.getLevel()), "Nine mixed resources must make " + kind.coreId());
            h.assertTrue(core.assemble(CraftingInput.of(3, 3, inputs), h.getLevel().registryAccess()).is(kind.core()), "Correct core result");
            inputs.set(0, new ItemStack(Items.BEDROCK));
            h.assertTrue(!core.matches(CraftingInput.of(3, 3, inputs), h.getLevel()), "Out-of-range ingredients must fail");
            inputs.set(0, ItemStack.EMPTY);
            h.assertTrue(!core.matches(CraftingInput.of(3, 3, inputs), h.getLevel()), "Eight ingredients must fail");
            List<ItemStack> frame = new ArrayList<>();
            for (int i = 0; i < 9; i++) frame.add(new ItemStack(i == 4 ? kind.core() : LyyItems.IMAGINARY_ALLOY_INGOT.get()));
            var frameRecipe = (CraftingRecipe) h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, kind.blockId())).orElseThrow().value();
            h.assertTrue(frameRecipe.matches(CraftingInput.of(3, 3, frame), h.getLevel()), "Frame requires eight alloys around its core");
            h.assertTrue(frameRecipe.assemble(CraftingInput.of(3, 3, frame), h.getLevel().registryAccess()).is(kind.block().asItem()), "Frame craft yields correct variant");
            frame.set(4, new ItemStack(ResourceFrameKind.values()[(kind.ordinal() + 1) % 4].core()));
            h.assertTrue(!frameRecipe.matches(CraftingInput.of(3, 3, frame), h.getLevel()), "Cores cannot be exchanged across frame types");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void selectionRequiredAndWaterDiscountByKind(GameTestHelper h) {
        for (var kind : ResourceFrameKind.values()) {
            var be = place(h, kind, false);
            energy(h, be, 200);
            tick(h, be);
            h.assertTrue(be.getOutputCount() == 0 && be.getEnergyIE() == 200, "New frames wait for a player selection");
            h.assertTrue(be.selectRecipe(0), "First output can be selected");
            tick(h, be);
            h.assertTrue(be.getOutputCount() == 1 && be.getEnergyIE() == 0, "Dry production costs 200 IE");
            be.getOutput().extractItem(0, 64, false);
            if (kind.supportsWater()) h.getLevel().setBlock(be.getBlockPos(), be.getBlockState().setValue(ResourceGatheringFrameBlock.WATERLOGGED, true), 3);
            energy(h, be, 100);
            tick(h, be);
            h.assertTrue(be.getOutputCount() == (kind.supportsWater() ? 1 : 0), "Only the overworld frames get the water discount");
            h.assertTrue(be.getEnergyIE() == (kind.supportsWater() ? 0 : 100), "No energy lost on insufficient power");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void changedSelectionWaitsForOldOutputAndPersists(GameTestHelper h) {
        var be = place(h, ResourceFrameKind.TREE, false);
        be.selectRecipe(0);
        energy(h, be, 600);
        tick(h, be);
        var original = be.getOutput().getStackInSlot(0);
        h.assertTrue(be.selectRecipe(1), "Selection can change while output is occupied");
        tick(h, be);
        h.assertTrue(be.getOutputCount() == 1 && be.getEnergyIE() == 400
                && ItemStack.isSameItemSameComponents(be.getOutput().getStackInSlot(0), original), "Old output is retained without consuming more energy");
        var restored = new ResourceGatheringFrameBlockEntity(be.getBlockPos(), be.getBlockState());
        restored.setLevel(h.getLevel());
        restored.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(restored.getSelectedIndex() == 1 && restored.getOutputCount() == 1, "Selection and output survive reload");
        restored.getOutput().extractItem(0, 64, false);
        tick(h, restored);
        h.assertTrue(ItemStack.isSameItemSameComponents(restored.getOutput().getStackInSlot(0), restored.recipes().get(1).value().result())
                && restored.getEnergyIE() == 200, "Selected output starts once old output is removed");
        var invalid = restored.saveWithoutMetadata(h.getLevel().registryAccess());
        invalid.putString("SelectedRecipe", ResourceGatheringFrameBlockEntity.recipes(h.getLevel(), ResourceFrameKind.NETHER).getFirst().id().toString());
        restored.loadWithComponents(invalid, h.getLevel().registryAccess());
        restored.getOutput().extractItem(0, 64, false);
        tick(h, restored);
        h.assertTrue(restored.getSelectedIndex() == -1 && restored.getOutputCount() == 0 && restored.getEnergyIE() == 200,
                "Cross-category or removed recipes never produce through stale selection IDs");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void selectionPacketsValidateIndexDistanceAndPreserveInventory(GameTestHelper h) {
        var be = place(h, ResourceFrameKind.OVERWORLD, false);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(be.getBlockPos()));
        var menu = new ResourceGatheringMenu(1, player.getInventory(), be, be.getOutput(), be.getData());
        h.assertTrue(!menu.clickMenuButton(player, -1) && !menu.clickMenuButton(player, Integer.MAX_VALUE), "Invalid selection packets must fail");
        h.assertTrue(menu.clickMenuButton(player, 24) && menu.selectedIndex() == 24, "Second selector page can choose a recipe");
        player.setPos(Vec3.atCenterOf(be.getBlockPos()).add(100, 0, 0));
        h.assertTrue(!menu.clickMenuButton(player, 0) && menu.selectedIndex() == 24, "Out-of-range players cannot change production");
        player.setPos(Vec3.atCenterOf(be.getBlockPos()));
        bulk(h, be, menu.selectedResult(), 1024);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(be.getOutputCount() == 1024, "A full inventory must not consume output");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(be.getOutputCount() == 960 && player.getInventory().getItem(0).getCount() == 64, "Quick move only transfers available capacity");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void netherUsesVanillaInteractionAndOverworldBucketsRetainSelection(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (var kind : ResourceFrameKind.values()) {
            var be = place(h, kind, false);
            be.selectRecipe(0);
            var hit = new BlockHitResult(Vec3.atCenterOf(be.getBlockPos()), Direction.UP, be.getBlockPos(), false);
            if (!kind.supportsWater()) {
                h.assertTrue(!be.getBlockState().hasProperty(ResourceGatheringFrameBlock.WATERLOGGED)
                        && !(kind.block() instanceof net.minecraft.world.level.block.LiquidBlockContainer)
                        && !(kind.block() instanceof net.minecraft.world.level.block.BucketPickup), "Nether frame is an ordinary block without liquid interfaces or a waterlogged state");
                for (var item : List.of(Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.BUCKET)) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                    var interaction = be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                    h.assertTrue(interaction == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                            "Nether frame must leave bucket handling to the vanilla interaction path");
                }
                h.assertTrue(be.getData().get(3) == 0, "Non-waterlogged frame syncs a dry GUI state");
                continue;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(be.getBlockState().getValue(ResourceGatheringFrameBlock.WATERLOGGED) == kind.supportsWater(), "Only overworld frames accept a water bucket");
            if (kind.supportsWater()) {
                h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Water bucket returns an empty bucket");
                be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && !be.getBlockState().getValue(ResourceGatheringFrameBlock.WATERLOGGED), "Empty bucket drains water");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
            be.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET) && !be.getBlockState().getValue(ResourceGatheringFrameBlock.WATERLOGGED), "Lava does not alter a frame");
            h.assertTrue(be.getSelectedIndex() == 0, "Bucket interactions preserve selection");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bulkEnergyAndSideExportAreSharedByEveryFrame(GameTestHelper h) {
        for (var kind : ResourceFrameKind.values()) {
            var be = place(h, kind, false);
            be.selectRecipe(0);
            for (Direction side : Direction.values()) {
                var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), side);
                h.assertTrue(energy != null && energy.canReceive() && !energy.canExtract() && energy.extractEnergy(1, false) == 0,
                        "Every face receives energy without exposing an energy source");
            }
            be.getEnergyStorage().receiveEnergy(99, false);
            var save = be.saveWithoutMetadata(h.getLevel().registryAccess());
            be.loadWithComponents(save, h.getLevel().registryAccess());
            be.getEnergyStorage().receiveEnergy(1, false);
            h.assertTrue(be.getEnergyIE() == 1, "FE remainder persists for every frame");
            bulk(h, be, be.recipes().getFirst().value().result(), 1023);
            energy(h, be, 400);
            tick(h, be); tick(h, be);
            h.assertTrue(be.getOutputCount() == 1024 && be.getEnergyIE() == 200, "Full storage stops production without wasting energy");
            h.setBlock(POS.west(), Blocks.BARREL);
            h.setBlock(POS.east(), Blocks.BARREL);
            BarrelBlockEntity left = h.getBlockEntity(POS.west());
            BarrelBlockEntity right = h.getBlockEntity(POS.east());
            for (int i = 0; i < left.getContainerSize(); i++) left.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            energy(h, be, 0);
            for (int i = 0; i < 20; i++) tick(h, be);
            int count = 0;
            for (int i = 0; i < right.getContainerSize(); i++) count += right.getItem(i).getCount();
            h.assertTrue(count == 1024 && be.getOutputCount() == 0 && be.getEnergyIE() == 1, "Blocked left side falls back right; stored output retries without production energy");
            h.setBlock(POS.west(), Blocks.AIR); h.setBlock(POS.east(), Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void breakingEveryResourceFrameDropsLegacyStoredOutput(GameTestHelper h) {
        for (var kind : ResourceFrameKind.values()) {
            var be = place(h, kind, false);
            var item = be.recipes().getFirst().value().result();
            bulk(h, be, item, 1024);
            h.assertTrue(be.getOutputCount() == 1024, "Existing bulk storage keys must load for " + kind);
            h.destroyBlock(POS);
            h.assertItemEntityCountIs(item.getItem(), POS, 3, 1024);
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(POS)).inflate(3))
                    .forEach(net.minecraft.world.entity.Entity::discard);
        }
        h.succeed();
    }
}
