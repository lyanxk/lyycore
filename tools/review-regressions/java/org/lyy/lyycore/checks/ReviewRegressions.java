package org.lyy.lyycore.checks;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.ToolMining;
import org.lyy.lyycore.content.blockEntities.ImaginaryCraftingTableBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryGateBlockEntity;
import org.lyy.lyycore.content.menu.ImaginaryCraftingMenu;
import org.lyy.lyycore.content.menu.ImaginaryGateMenu;
import org.lyy.lyycore.content.recipes.ImaginaryCraftingRecipe;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.content.research.ResearchManager;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ReviewRegressions {
    @GameTest(template = "empty", batch = "research_reload", timeoutTicks = 400)
    public static void researchReloadRejectsStaleMenu(GameTestHelper test) {
        var level = test.getLevel();
        var player = player(level);
        var id = ResourceLocation.parse("lyycore:research/change_weather");
        var before = ResearchManager.get(level, id);
        test.assertTrue(before != null, "Research missing before reload");
        ResearchProgress.setCompleted(player, id, before.value(), true);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        ResearchMenu menu;
        try {
            buffer.writeVarInt(1);
            buffer.writeResourceLocation(id);
            menu = ResearchMenu.memory(1, player.getInventory(), buffer);
        } finally { buffer.release(); }
        var server = level.getServer();
        var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
        test.succeedWhen(() -> {
            test.assertTrue(reload.isDone(), "Waiting for data-pack reload");
            reload.join();
            var after = ResearchManager.get(level, id);
            test.assertTrue(after != null && after != before, "Reload retained an obsolete research catalog");
            test.assertFalse(menu.clickMenuButton(player, 0), "Stale menu produced notes after reload");
            test.assertTrue(ResearchProgress.completed(player, id), "Reload lost completed research");
            test.assertTrue(ResearchNotesItem.research(ResearchNotesItem.create(id, player.getUUID()), level) == after.value(),
                    "Notes did not resolve the reloaded definition");
        });
    }

    static ServerPlayer player(ServerLevel level) {
        // Use a real player for advancement criteria, with a silent transport and
        // no login/handshake; these checks exercise menus rather than networking.
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "review-player"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override public void setListenerForServerboundHandshake(PacketListener listener) { }
        };
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { }
        };
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    @GameTest(template = "empty")
    public static void craftingExtractionCredits(GameTestHelper test) {
        var level = test.getLevel();
        var recipeId = ResourceLocation.parse("lyycore:im_generator");
        var recipe = (ImaginaryCraftingRecipe) level.getRecipeManager().byKey(recipeId).orElseThrow().value();
        var advancement = level.getServer().getAdvancements()
                .get(ResourceLocation.parse("lyycore:progression/production/generator"));
        test.assertTrue(advancement != null, "Generator advancement must exist");
        for (String mode : new String[]{"swap", "pickup", "shift", "hotbar", "throw"}) {
            BlockPos pos = test.absolutePos(new BlockPos(2, 1, 2));
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, LyyBlocks.IMAGINARY_CRAFTING_TABLE.get().defaultBlockState());
            var table = (ImaginaryCraftingTableBlockEntity) level.getBlockEntity(pos);
            var player = player(level);
            for (int slot = 0; slot < 9; slot++) table.items().setStackInSlot(slot, recipe.ingredients().get(slot).getItems()[0].copyWithCount(1));
            for (int tick = 0; tick < ImaginaryCraftingRecipe.DURATION; tick++)
                ImaginaryCraftingTableBlockEntity.serverTick(level, pos, table.getBlockState(), table);
            test.assertTrue(ItemStack.matches(table.items().getStackInSlot(0), recipe.result()), "Crafted result missing: " + mode);
            test.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(), "Credit before extraction: " + mode);
            var menu = new ImaginaryCraftingMenu(1, player.getInventory(), table, table.data());
            switch (mode) {
                case "swap" -> { menu.setCarried(new ItemStack(Items.DIRT)); menu.clicked(0, 0, ClickType.PICKUP, player); }
                case "pickup" -> menu.clicked(0, 0, ClickType.PICKUP, player);
                case "shift" -> menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                case "hotbar" -> menu.clicked(0, 0, ClickType.SWAP, player);
                case "throw" -> menu.clicked(0, 0, ClickType.THROW, player);
            }
            test.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "Missing crafting credit: " + mode);
            test.assertFalse(table.saveWithoutMetadata(level.registryAccess()).contains("CompletedRecipe"), "Stale crafting credit: " + mode);
            if (mode.equals("swap")) {
                test.assertTrue(ItemStack.matches(menu.getCarried(), recipe.result()), "Swap did not return the result");
                test.assertTrue(table.items().getStackInSlot(0).is(Items.DIRT), "Swap did not insert the cursor item");
            }
            // Putting the same product back must not award a second player's advancement.
            var other = player(level);
            table.items().setStackInSlot(0, recipe.result());
            new ImaginaryCraftingMenu(2, other.getInventory(), table, table.data()).clicked(0, 0, ClickType.PICKUP, other);
            test.assertFalse(other.getAdvancements().getOrStartProgress(advancement).isDone(), "Reused crafting credit: " + mode);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void offeringRequiresActualInsertion(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos, LyyBlocks.IMAGINARY_GATE.get().defaultBlockState());
        var gate = (ImaginaryGateBlockEntity) level.getBlockEntity(pos);
        var first = player(level);
        var second = player(level);
        var firstMenu = new ImaginaryGateMenu(1, first.getInventory(), gate, new SimpleContainerData(1));
        var secondMenu = new ImaginaryGateMenu(2, second.getInventory(), gate, new SimpleContainerData(1));
        firstMenu.setCarried(new ItemStack(LyyItems.CRYSTAL_BLOCK.get()));
        firstMenu.clicked(0, 0, ClickType.PICKUP, first);
        assertContributor(test, gate, first);
        secondMenu.setCarried(new ItemStack(LyyItems.CRYSTAL_BLOCK.get()));
        secondMenu.clicked(0, 0, ClickType.PICKUP, second);
        assertContributor(test, gate, first);
        test.assertTrue(secondMenu.getCarried().getCount() == 1, "Full-slot click consumed an item");
        secondMenu.setCarried(new ItemStack(Items.DIRT, 2));
        secondMenu.clicked(0, 0, ClickType.PICKUP, second);
        assertContributor(test, gate, first);
        second.getInventory().setItem(9, new ItemStack(LyyItems.CRYSTAL_BLOCK.get()));
        secondMenu.clicked(1, 0, ClickType.QUICK_MOVE, second);
        assertContributor(test, gate, first);
        // Equal-value hotbar swaps must not leave inventory and machine sharing one stack.
        second.getInventory().setItem(0, new ItemStack(LyyItems.CRYSTAL_BLOCK.get()));
        secondMenu.clicked(0, 0, ClickType.SWAP, second);
        test.assertTrue(gate.items().getStackInSlot(0) != second.getInventory().getItem(0), "Aliased hotbar swap stacks");
        assertContributor(test, gate, first);
        secondMenu.setCarried(ItemStack.EMPTY);
        secondMenu.clicked(0, 0, ClickType.PICKUP, second);
        test.assertFalse(gate.saveWithoutMetadata(level.registryAccess()).contains("OfferingPlayer"), "Removing an offering retained its owner");
        secondMenu.setCarried(ItemStack.EMPTY);
        secondMenu.clicked(1, 0, ClickType.QUICK_MOVE, second);
        assertContributor(test, gate, second);
        test.succeed();
    }

    private static void assertContributor(GameTestHelper test, ImaginaryGateBlockEntity gate, ServerPlayer expected) {
        var saved = gate.saveWithoutMetadata(test.getLevel().registryAccess());
        test.assertTrue(saved.hasUUID("OfferingPlayer") && saved.getUUID("OfferingPlayer").equals(expected.getUUID()),
                "Incorrect offering contributor");
    }

    @GameTest(template = "empty")
    public static void extraMiningRespectsPositionPermission(GameTestHelper test) {
        var level = test.getLevel();
        var player = player(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LyyItems.IMAGINARY_REAPER.get()));
        player.setXRot(90); // A horizontal mining plane.
        var border = level.getWorldBorder();
        double oldX = border.getCenterX(), oldZ = border.getCenterZ(), oldSize = border.getSize();
        BlockPos origin = test.absolutePos(new BlockPos(3, 1, 3));
        try {
            border.setCenter(origin.getX() - 4.5, origin.getZ() + 0.5);
            border.setSize(10);
            for (ToolMining.Pattern pattern : new ToolMining.Pattern[]{ToolMining.Pattern.AREA, ToolMining.Pattern.VEIN}) {
                for (BlockPos pos : new BlockPos[]{origin, origin.east(), origin.west()})
                    level.setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
                test.assertTrue(level.mayInteract(player, origin), "Origin should allow mining");
                test.assertFalse(level.mayInteract(player, origin.east()), "Extra block should deny mining");
                ToolMining.mine(pattern, level, Blocks.DIAMOND_ORE.defaultBlockState(), origin, player);
                test.assertTrue(level.getBlockState(origin.east()).is(Blocks.DIAMOND_ORE), "Mined across protected boundary: " + pattern);
                test.assertTrue(level.getBlockState(origin.west()).isAir(), "Allowed extra block was not mined: " + pattern);
            }
        } finally {
            border.setCenter(oldX, oldZ);
            border.setSize(oldSize);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void weatherBallsCheckDimensionBeforeConsumption(GameTestHelper test) {
        var server = test.getLevel().getServer();
        for (var dimension : java.util.List.of(Level.NETHER, Level.END)) {
            ServerLevel level = server.getLevel(dimension);
            test.assertTrue(level != null, "Test dimension is missing");
            var player = player(level);
            for (var item : new net.minecraft.world.item.Item[]{LyyItems.STORM_BALL.get(), LyyItems.SUN_BALL.get()}) {
                var stack = new ItemStack(item, 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var result = item.use(level, player, InteractionHand.MAIN_HAND);
                test.assertTrue(result.getResult() == InteractionResult.FAIL && stack.getCount() == 2, "Unsupported dimension consumed a weather ball");
            }
        }
        var level = test.getLevel();
        var player = player(level);
        for (boolean storm : new boolean[]{true, false}) {
            var item = storm ? LyyItems.STORM_BALL.get() : LyyItems.SUN_BALL.get();
            var stack = new ItemStack(item, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            test.assertTrue(item.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Overworld throw failed");
            test.assertTrue(stack.getCount() == 1, "Overworld throw must consume one ball");
            var data = server.getWorldData().overworldData();
            test.assertTrue(data.isRaining() == storm && data.isThundering() == storm, "Incorrect weather state");
            test.assertTrue((storm ? data.getRainTime() : data.getClearWeatherTime()) == 72000, "Incorrect weather duration");
        }
        test.succeed();
    }
}
