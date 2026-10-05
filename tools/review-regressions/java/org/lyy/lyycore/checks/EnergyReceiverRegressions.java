package org.lyy.lyycore.checks;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.Config;
import org.lyy.lyycore.content.blockEntities.*;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;
import org.lyy.lyycore.energy.*;
import org.lyy.lyycore.registry.LyyBlocks;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class EnergyReceiverRegressions {
    private static ImaginaryCondensingBeaconBlockEntity beacon(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(5, 1, 4));
        var block = LyyBlocks.IMAGINARY_CONDENSING_BEACON.get();
        for (int part = 0; part < block.height() * 9; part++)
            test.getLevel().setBlock(SquareMachineBlock.partPos(pos, part), block.defaultBlockState().setValue(SquareMachineBlock.PART, part), 2);
        return (ImaginaryCondensingBeaconBlockEntity) test.getLevel().getBlockEntity(pos);
    }
    @GameTest(template = "empty", batch = "energy_receivers")
    public static void generatorCountsMultiblockAsOneReceiver(GameTestHelper test) {
        var beacon = beacon(test);
        var generator = new BaseImaginaryGeneratorBlockEntity(test.absolutePos(new BlockPos(2, 1, 4)), LyyBlocks.IGB.get().defaultBlockState());
        generator.setLevel(test.getLevel());
        BaseImaginaryGeneratorBlockEntity.getPositions(generator);
        test.assertTrue(generator.getTargetCount() == 1, "Multiblock parts counted as separate receivers");
        int amount = Config.GENERATOR_FE_PER_TARGET_TICK.get();
        for (int i = 0; i < 25; i++)
            BaseImaginaryGeneratorBlockEntity.serverTick(test.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
        test.assertTrue(beacon.energy().getImaginaryEnergyStored() == (long) amount * 25 / 100, "Generator duplicated the per-machine budget");
        test.succeed();
    }
    @GameTest(template = "empty", batch = "energy_receivers")
    public static void towerResolvesPartWithoutChangingSelectedCoordinate(GameTestHelper test) {
        var beacon = beacon(test);
        var connection = SquareMachineBlock.partPos(beacon.getBlockPos(), 0);
        var tower = new SpatialTransmissionTowerBlockEntity(test.absolutePos(new BlockPos(2, 1, 4)), LyyBlocks.SPATIAL_TRANSMISSION_TOWER.get().defaultBlockState());
        tower.setLevel(test.getLevel());
        var selected = GlobalPos.of(test.getLevel().dimension(), connection);
        tower.setTarget(selected);
        try {
            SpatialTransmissionTowerBlockEntity.serverTick(test.getLevel(), tower.getBlockPos(), tower.getBlockState(), tower);
            test.assertTrue(tower.target().equals(selected), "Selected port coordinate was overwritten");
            test.assertTrue(EnergyReceiver.controller(test.getLevel(), connection).equals(beacon.getBlockPos()), "Port did not resolve to controller");
            test.assertTrue(beacon.energy().getImaginaryEnergyStored() == 10000, "Tower delivered duplicate energy through multiple faces");
        } finally { tower.setRemoved(); }
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void storagePermissionsAndAdapterLimitsAgree(GameTestHelper test) {
        int[] changes = {0};
        var storage = new ImaginaryEnergyStorage(10, 2, 1, () -> changes[0]++);
        var fe = new ImaginaryEnergyFeAdapter(storage, () -> {});
        test.assertFalse((Object) storage instanceof net.neoforged.neoforge.energy.IEnergyStorage, "IE store still exposes a second FE interface");
        test.assertTrue(fe.receiveEnergy(1000, true) == 200 && changes[0] == 0, "Simulation ignored IE input limit or notified");
        test.assertTrue(fe.receiveEnergy(1000, false) == 200 && storage.getImaginaryEnergyStored() == 2 && changes[0] == 1, "FE input exceeded storage limit");
        test.assertTrue(fe.extractEnergy(1000, false) == 100 && storage.getImaginaryEnergyStored() == 1 && changes[0] == 2, "FE extraction exceeded storage limit");
        storage.setImaginaryEnergy(Integer.MAX_VALUE);
        test.assertTrue(storage.getImaginaryEnergyStored() == 10, "Setter exceeded capacity");
        var inputOnly = new ImaginaryEnergyStorage(10, 10, 0);
        var inputFe = new ImaginaryEnergyFeAdapter(inputOnly, () -> {});
        inputOnly.receiveImaginaryEnergy(5);
        test.assertTrue(!inputFe.canExtract() && inputFe.extractEnergy(100, false) == 0 && inputOnly.extractImaginaryEnergy(1) == 0, "Output permission bypassed");
        test.succeed();
    }
}
