package org.lyy.lyycore.checks;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.energy.*;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.blockEntities.SpatialTransmissionTowerBlockEntity;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.registry.*;
import java.util.*;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "lyycore")
public final class TransmissionRegressions {
    private static final Map<BlockPos, Receiver> RECEIVERS = new HashMap<>();
    private static final class Receiver {
        final Map<Direction, IEnergyStorage> fe = new HashMap<>();
        final Map<Direction, ImaginaryEnergyStorage> ie = new HashMap<>();
        int queries;
    }
    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            var receiver = RECEIVERS.get(pos);
            if (receiver == null) return null;
            receiver.queries++;
            return receiver.fe.get(side);
        }, Blocks.GOLD_BLOCK);
        event.registerBlock(LyyCapabilities.IMAGINARY_ENERGY, (level, pos, state, be, side) -> {
            var receiver = RECEIVERS.get(pos);
            if (receiver == null) return null;
            receiver.queries++;
            return receiver.ie.get(side);
        }, Blocks.GOLD_BLOCK);
    }
    private static Receiver receiver(GameTestHelper test, BlockPos pos) {
        var result = new Receiver();
        RECEIVERS.put(pos, result);
        test.getLevel().setBlockAndUpdate(pos, Blocks.GOLD_BLOCK.defaultBlockState());
        test.getLevel().invalidateCapabilities(pos);
        return result;
    }
    private static SpatialTransmissionTowerBlockEntity tower(GameTestHelper test, BlockPos target) {
        var tower = new SpatialTransmissionTowerBlockEntity(test.absolutePos(new BlockPos(2, 1, 2)),
                LyyBlocks.SPATIAL_TRANSMISSION_TOWER.get().defaultBlockState());
        tower.setLevel(test.getLevel());
        tower.setTarget(GlobalPos.of(test.getLevel().dimension(), target));
        return tower;
    }
    private static void tick(SpatialTransmissionTowerBlockEntity tower) {
        SpatialTransmissionTowerBlockEntity.serverTick(tower.getLevel(), tower.getBlockPos(), tower.getBlockState(), tower);
    }
    @GameTest(template = "empty")
    public static void unsidedInterfacesCacheAndInvalidate(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(4, 1, 4));
        var receiver = receiver(test, pos);
        var first = new EnergyStorage(3000000);
        receiver.fe.put(null, first);
        var tower = tower(test, pos);
        try {
            tick(tower);
            test.assertTrue(first.getEnergyStored() == 500000 && tower.target() != null, "Unsided receiver rejected");
            int queries = receiver.queries;
            tick(tower);
            test.assertTrue(first.getEnergyStored() == 1000000 && receiver.queries == queries, "Repeated capability lookup");
            var replacement = new EnergyStorage(3000000);
            receiver.fe.put(null, replacement);
            test.getLevel().invalidateCapabilities(pos);
            tick(tower);
            test.assertTrue(replacement.getEnergyStored() == 500000 && first.getEnergyStored() == 1000000,
                    "Invalidation retained stale capability");
            var otherPos = pos.offset(1, 0, 0);
            var other = receiver(test, otherPos);
            var rebound = new EnergyStorage(3000000);
            other.fe.put(null, rebound);
            tower.setTarget(GlobalPos.of(test.getLevel().dimension(), otherPos));
            tick(tower);
            test.assertTrue(rebound.getEnergyStored() == 500000 && replacement.getEnergyStored() == 500000,
                    "Rebinding kept old destination");
            RECEIVERS.remove(otherPos);
        } finally { tower.setRemoved(); RECEIVERS.remove(pos); }
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void allFacesShareOneBudgetAndSkipFullFaces(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(4, 1, 4));
        var receiver = receiver(test, pos);
        var full = new EnergyStorage(100, 100, 0, 100);
        var small = new EnergyStorage(100000);
        var large = new EnergyStorage(3000000);
        receiver.fe.put(Direction.DOWN, full);
        receiver.fe.put(Direction.UP, small);
        receiver.fe.put(Direction.NORTH, large);
        receiver.fe.put(null, large);
        var tower = tower(test, pos);
        try {
            tick(tower);
            test.assertTrue(small.getEnergyStored() == 100000 && large.getEnergyStored() == 400000,
                    "Full face stopped transfer or budget was duplicated");
            tick(tower);
            test.assertTrue(large.getEnergyStored() == 900000, "Second tick failed to skip full faces");
        } finally { tower.setRemoved(); RECEIVERS.remove(pos); }
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void nativeEnergyDoesNotAlsoGenerateFe(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(4, 1, 4));
        var receiver = receiver(test, pos);
        var small = new ImaginaryEnergyStorage(0, 0, 0) {
            @Override public int getMaxImaginaryEnergyStored() { return 3000; }
        };
        var large = new ImaginaryEnergyStorage(0, 0, 0);
        var fe = new EnergyStorage(3000000);
        receiver.ie.put(Direction.DOWN, small); receiver.ie.put(null, large); receiver.fe.put(null, fe);
        var tower = tower(test, pos);
        try {
            tick(tower);
            test.assertTrue(small.getImaginaryEnergyStored() == 3000 && large.getImaginaryEnergyStored() == 7000
                    && fe.getEnergyStored() == 0, "Native budget or FE fallback duplicated energy");
        } finally { tower.setRemoved(); RECEIVERS.remove(pos); }
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void unloadedTargetIsRetainedWithoutLoading(GameTestHelper test) {
        var pos = new BlockPos(20000000, 100, 20000000);
        test.assertFalse(test.getLevel().hasChunkAt(pos), "Fixture unexpectedly loaded");
        var tower = tower(test, pos);
        try {
            tick(tower);
            test.assertTrue(tower.target() != null && !test.getLevel().hasChunkAt(pos), "Target was forgotten or force-loaded");
        } finally { tower.setRemoved(); }
        test.succeed();
    }
}
