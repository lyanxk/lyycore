package org.lyy.lyycore.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.item.ImaginaryDisassemblerItem;
import org.lyy.lyycore.registry.LyyItems;

import java.util.UUID;

@GameTestHolder(LyyCore.MODID)
@PrefixGameTestTemplate(false)
public final class DisassemblerGameTests {
    private static final BlockPos CENTER = new BlockPos(7, 2, 7);

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void areaMinesFiveByFiveFacingX(GameTestHelper helper) {
        checkArea(helper, Direction.Axis.X);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void areaMinesFiveByFiveFacingY(GameTestHelper helper) {
        checkArea(helper, Direction.Axis.Y);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void areaMinesFiveByFiveFacingZ(GameTestHelper helper) {
        checkArea(helper, Direction.Axis.Z);
    }

    private static void checkArea(GameTestHelper helper, Direction.Axis axis) {
        var player = equippedPlayer(helper, ImaginaryDisassemblerItem.Mode.AOE5);
        player.setYRot(axis == Direction.Axis.X ? 90 : 0);
        player.setXRot(axis == Direction.Axis.Y ? 90 : 0);
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                helper.setBlock(planePos(axis, a, b), Blocks.STONE);
            }
        }
        BlockPos outside = planePos(axis, 0, 3);
        BlockPos behind = CENTER.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        helper.setBlock(outside, Blocks.STONE);
        helper.setBlock(behind, Blocks.STONE);

        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(CENTER)), "The starting block must break");
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                helper.assertTrue(helper.getBlockState(planePos(axis, a, b)).isAir(),
                        "All 25 blocks in the selected plane must be mined");
            }
        }
        helper.assertBlockPresent(Blocks.STONE, outside);
        helper.assertBlockPresent(Blocks.STONE, behind);
        helper.succeed();
    }

    private static BlockPos planePos(Direction.Axis axis, int a, int b) {
        return switch (axis) {
            case X -> CENTER.offset(0, a, b);
            case Y -> CENTER.offset(a, 0, b);
            case Z -> CENTER.offset(a, b, 0);
        };
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void veinConnectsAllTwentySixNeighborsAndDiagonalChains(GameTestHelper helper) {
        var player = equippedPlayer(helper, ImaginaryDisassemblerItem.Mode.VEIN);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.setBlock(CENTER.offset(dx, dy, dz), Blocks.IRON_ORE);
                }
            }
        }
        BlockPos diagonal = CENTER.offset(2, 2, 2);
        BlockPos continuation = CENTER.offset(3, 1, 3);
        BlockPos otherOre = CENTER.offset(3, 0, 2);
        BlockPos disconnected = CENTER.offset(5, 0, 5);
        helper.setBlock(diagonal, Blocks.IRON_ORE);
        helper.setBlock(continuation, Blocks.IRON_ORE);
        helper.setBlock(otherOre, Blocks.GOLD_ORE);
        helper.setBlock(disconnected, Blocks.IRON_ORE);

        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(CENTER)), "The starting ore must break");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertTrue(helper.getBlockState(CENTER.offset(dx, dy, dz)).isAir(),
                            "Face, edge and corner neighbors must all connect");
                }
            }
        }
        helper.assertTrue(helper.getBlockState(diagonal).isAir() && helper.getBlockState(continuation).isAir(),
                "Scanning must continue beyond the initial neighborhood through diagonal connections");
        helper.assertBlockPresent(Blocks.GOLD_ORE, otherOre);
        helper.assertBlockPresent(Blocks.IRON_ORE, disconnected);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void veinMatchesBlockTypeAcrossDifferentStates(GameTestHelper helper) {
        var player = equippedPlayer(helper, ImaginaryDisassemblerItem.Mode.VEIN);
        helper.setBlock(CENTER, Blocks.OAK_LOG);
        BlockPos neighbor = CENTER.offset(1, 1, 1);
        BlockPos continuation = CENTER.offset(2, 0, 2);
        BlockPos otherLog = CENTER.offset(1, 0, 1);
        helper.setBlock(neighbor, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.setBlock(continuation, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(otherLog, Blocks.BIRCH_LOG);

        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(CENTER)), "The starting log must break");
        helper.assertTrue(helper.getBlockState(neighbor).isAir() && helper.getBlockState(continuation).isAir(),
                "The same block type must connect even when its block state differs");
        helper.assertBlockPresent(Blocks.BIRCH_LOG, otherLog);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void veinExcludesCommonTerrainButStillMinesTheStartingBlock(GameTestHelper helper) {
        var player = equippedPlayer(helper, ImaginaryDisassemblerItem.Mode.VEIN);
        BlockPos neighbor = CENTER.east();
        helper.setBlock(CENTER.below(), Blocks.STONE);
        helper.setBlock(neighbor.below(), Blocks.STONE);
        for (Block terrain : new Block[]{Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
                Blocks.DEEPSLATE, Blocks.TUFF, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
                Blocks.COBBLED_DEEPSLATE, Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT,
                Blocks.END_STONE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
                Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MUD, Blocks.MYCELIUM,
                Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.SANDSTONE, Blocks.RED_SANDSTONE}) {
            helper.setBlock(CENTER, terrain);
            helper.setBlock(neighbor, terrain);
            helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(CENTER)),
                    "Excluded terrain must still allow normal single-block mining: " + terrain);
            helper.assertTrue(helper.getBlockState(CENTER).isAir(), "The starting terrain block must be removed");
            helper.assertBlockPresent(terrain, neighbor);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void veinStopsAtSixtyFourIncludingTheStartingBlock(GameTestHelper helper) {
        var player = equippedPlayer(helper, ImaginaryDisassemblerItem.Mode.VEIN);
        BlockPos origin = new BlockPos(5, 1, 5);
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(3, 3, 4))) {
            helper.setBlock(pos, Blocks.IRON_ORE);
        }
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(origin)), "The starting ore must break");
        int remaining = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(3, 3, 4))) {
            if (helper.getBlockState(pos).is(Blocks.IRON_ORE)) remaining++;
        }
        helper.assertTrue(remaining == 16, "An 80-block vein must retain exactly 16 blocks after mining 64");
        helper.succeed();
    }

    private static FakePlayer equippedPlayer(GameTestHelper helper, ImaginaryDisassemblerItem.Mode mode) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "disassembler-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(CENTER).getCenter());
        ItemStack tool = new ItemStack(LyyItems.IMAGINARY_DISASSEMBLER.get());
        ImaginaryDisassemblerItem.setMode(tool, mode);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private DisassemblerGameTests() { }
}
