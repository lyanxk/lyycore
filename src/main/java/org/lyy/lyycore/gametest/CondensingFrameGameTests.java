package org.lyy.lyycore.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.ResourceFrameKind;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.lyy.lyycore.registry.LyyBlocks;

import java.util.List;

@GameTestHolder(LyyCore.MODID)
@PrefixGameTestTemplate(false)
public final class CondensingFrameGameTests {
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void containedWaterDoesNotSpreadAndCanStillBeCollected(GameTestHelper h) {
        List<Block> frames = List.of(LyyBlocks.CRYSTAL_CONDENSING_FRAME.get(),
                ResourceFrameKind.TREE.block(), ResourceFrameKind.OVERWORLD.block(), ResourceFrameKind.MINERAL.block());
        List<BlockPos> positions = List.of(new BlockPos(1, 2, 1), new BlockPos(3, 2, 1),
                new BlockPos(1, 2, 3), new BlockPos(3, 2, 3));
        for (int i = 0; i < frames.size(); i++) {
            BlockPos pos = positions.get(i);
            for (Direction side : Direction.values()) h.setBlock(pos.relative(side), Blocks.AIR);
            h.setBlock(pos, frames.get(i));
            var state = h.getLevel().getBlockState(h.absolutePos(pos));
            var waterlogged = (SimpleWaterloggedBlock) frames.get(i);
            h.assertTrue(waterlogged.placeLiquid(h.getLevel(), h.absolutePos(pos), state, Fluids.WATER.getSource(false)),
                    "Vanilla water placement must still fill the frame");
            // Also exercise neighbor updates and explicit ticks, including ticks
            // that may already have been scheduled in an existing world.
            h.setBlock(pos.above(), Blocks.STONE);
            h.setBlock(pos.above(), Blocks.AIR);
            Fluids.WATER.tick(h.getLevel(), h.absolutePos(pos), Fluids.WATER.getSource(false));
        }
        h.runAfterDelay(20, () -> {
            for (int i = 0; i < frames.size(); i++) {
                BlockPos pos = positions.get(i);
                BlockPos absolute = h.absolutePos(pos);
                var state = h.getLevel().getBlockState(absolute);
                var be = h.getLevel().getBlockEntity(absolute);
                h.assertTrue(state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false) && state.getFluidState().isSource(),
                        "Water state must remain available to rendering and production");
                for (Direction side : Direction.values()) {
                    h.assertTrue(h.getLevel().getBlockState(absolute.relative(side)).isAir(),
                            "Contained water must not spread through " + side + " for " + frames.get(i));
                }
                var bucket = ((SimpleWaterloggedBlock) frames.get(i)).pickupBlock(null, h.getLevel(), absolute, state);
                h.assertTrue(bucket.is(Items.WATER_BUCKET)
                                && !h.getLevel().getBlockState(absolute).getValue(BlockStateProperties.WATERLOGGED)
                                && h.getLevel().getFluidState(absolute).isEmpty()
                                && h.getLevel().getBlockEntity(absolute) == be,
                        "Collecting contained water must preserve the frame and its block entity");
            }
            h.succeed();
        });
    }
}
