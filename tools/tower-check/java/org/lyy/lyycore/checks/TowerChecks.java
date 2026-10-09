package org.lyy.lyycore.checks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.blockEntities.ImaginaryCondensingBeaconBlockEntity;
import org.lyy.lyycore.content.blockEntities.SpatialTransmissionTowerBlockEntity;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class TowerChecks {
    private static SpatialTransmissionTowerBlockEntity tower(GameTestHelper test) {
        // Keep the tower off the level's ticker so each assertion observes exactly one manual tick.
        var tower = new SpatialTransmissionTowerBlockEntity(test.absolutePos(new BlockPos(3, 1, 3)),
                LyyBlocks.SPATIAL_TRANSMISSION_TOWER.get().defaultBlockState());
        tower.setLevel(test.getLevel());
        return tower;
    }
    private static GlobalPos at(GameTestHelper test, int x, int z) {
        return GlobalPos.of(test.getLevel().dimension(), test.absolutePos(new BlockPos(x, 1, z)));
    }
    private static ImaginaryCondensingBeaconBlockEntity beacon(GameTestHelper test, int x, int z) {
        test.setBlock(new BlockPos(x, 1, z), LyyBlocks.IMAGINARY_CONDENSING_BEACON.get());
        return (ImaginaryCondensingBeaconBlockEntity) test.getLevel().getBlockEntity(at(test, x, z).pos());
    }
    private static void tick(GameTestHelper test, SpatialTransmissionTowerBlockEntity tower) {
        SpatialTransmissionTowerBlockEntity.serverTick(test.getLevel(), tower.getBlockPos(), tower.getBlockState(), tower);
    }
    private static CompoundTag savedTargets(GlobalPos... targets) {
        var tag = new CompoundTag();
        var list = new ListTag();
        for (var target : targets) list.add(GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).getOrThrow());
        tag.put("Targets", list);
        return tag;
    }
    private static void useDevice(GameTestHelper test, Player player, BlockPos relative) {
        var pos = test.absolutePos(relative);
        LyyItems.COORDINATE_DEVICE.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    @GameTest(template = "tower_empty")
    public static void coordinateDeviceAddsTargetsAndConsumesOnSuccess(GameTestHelper test) {
        var first = beacon(test, 1, 1);
        var second = beacon(test, 5, 1);
        var towerPos = new BlockPos(3, 1, 3);
        var partPos = towerPos.above();
        test.setBlock(towerPos, LyyBlocks.SPATIAL_TRANSMISSION_TOWER.get());
        test.setBlock(partPos, LyyBlocks.SPATIAL_TRANSMISSION_TOWER.get().defaultBlockState()
                .setValue(SquareMachineBlock.PART, SquareMachineBlock.CENTER + 9));
        var tower = (SpatialTransmissionTowerBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(towerPos));
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var device = new ItemStack(LyyItems.COORDINATE_DEVICE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, device);
        useDevice(test, player, partPos);
        test.assertTrue(device.getCount() == 2 && tower.getTargetCount() == 0, "An unrecorded device must not be consumed");
        useDevice(test, player, new BlockPos(1, 1, 1));
        test.assertTrue(device.getCount() == 2, "Recording must not consume the device");
        useDevice(test, player, partPos);
        test.assertTrue(device.getCount() == 1 && tower.getTargetCount() == 1, "Binding any tower part consumes one device");
        useDevice(test, player, partPos);
        test.assertTrue(device.getCount() == 1 && tower.getTargetCount() == 1, "Duplicate binding must not consume the device");
        useDevice(test, player, new BlockPos(5, 1, 1));
        useDevice(test, player, partPos);
        test.assertTrue(device.isEmpty() && tower.getTargetCount() == 2, "Second binding must append and consume the remaining device");
        tick(test, tower);
        test.assertTrue(first.energy().getImaginaryEnergyStored() == 10000
                && second.energy().getImaginaryEnergyStored() == 10000, "The actual item interaction must power both targets");
        var creative = test.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        var creativeDevice = new ItemStack(LyyItems.COORDINATE_DEVICE.get());
        creative.setItemInHand(InteractionHand.MAIN_HAND, creativeDevice);
        test.setBlock(new BlockPos(1, 1, 5), Blocks.IRON_BLOCK);
        useDevice(test, creative, new BlockPos(1, 1, 5));
        useDevice(test, creative, partPos);
        test.assertTrue(creativeDevice.getCount() == 1 && tower.getTargetCount() == 3, "Creative binding preserves the device");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void independentNativeAndFeBudgets(GameTestHelper test) {
        var tower = tower(test);
        var first = beacon(test, 1, 1);
        var second = beacon(test, 5, 1);
        test.setBlock(new BlockPos(1, 1, 5), Blocks.IRON_BLOCK);
        test.setBlock(new BlockPos(5, 1, 5), Blocks.IRON_BLOCK);
        tower.addTarget(at(test, 1, 1)); tower.addTarget(at(test, 5, 1));
        tower.addTarget(at(test, 1, 5)); tower.addTarget(at(test, 5, 5));
        tick(test, tower);
        test.assertTrue(first.energy().getImaginaryEnergyStored() == 10000
                && second.energy().getImaginaryEnergyStored() == 10000, "Every native target needs its own 10,000 IE budget");
        test.assertTrue(TowerCheckCapabilities.FE.get(at(test, 1, 5)).getEnergyStored() == 500000
                && TowerCheckCapabilities.FE.get(at(test, 5, 5)).getEnergyStored() == 500000,
                "Every FE target needs its own 500,000 FE budget, without multiplication by faces");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void duplicateMultiblockBindings(GameTestHelper test) {
        var tower = tower(test);
        var receiver = beacon(test, 1, 1);
        test.setBlock(new BlockPos(2, 1, 1), receiver.getBlockState().setValue(SquareMachineBlock.PART, 5));
        test.assertTrue(tower.addTarget(at(test, 1, 1)), "Initial binding must succeed");
        test.assertTrue(!tower.addTarget(at(test, 1, 1)) && !tower.addTarget(at(test, 2, 1))
                && tower.getTargetCount() == 1, "Coordinates and multiblock aliases must not add duplicates");
        tick(test, tower);
        test.assertTrue(receiver.energy().getImaginaryEnergyStored() == 10000, "Multiblock receives one budget");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void aliasesLoadedFromDiskReceiveOnce(GameTestHelper test) {
        var tower = tower(test);
        var receiver = beacon(test, 1, 1);
        test.setBlock(new BlockPos(2, 1, 1), receiver.getBlockState().setValue(SquareMachineBlock.PART, 5));
        tower.loadCustomOnly(savedTargets(at(test, 1, 1), at(test, 2, 1), at(test, 1, 1)), test.getLevel().registryAccess());
        tick(test, tower);
        test.assertTrue(tower.getTargetCount() == 2 && receiver.energy().getImaginaryEnergyStored() == 10000,
                "Bindings saved while offline must still share a single budget per controller");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void invalidAndUnloadedTargetsDoNotBlockOthers(GameTestHelper test) {
        var tower = tower(test);
        var receiver = beacon(test, 5, 1);
        var unloaded = GlobalPos.of(test.getLevel().dimension(), new BlockPos(10_000_000, 64, 10_000_000));
        var absent = GlobalPos.of(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("lyycore", "missing_test_dimension")), BlockPos.ZERO);
        test.assertTrue(!test.getLevel().hasChunkAt(unloaded.pos()), "Fixture requires an unloaded chunk");
        tower.addTarget(at(test, 1, 1)); tower.addTarget(unloaded); tower.addTarget(absent); tower.addTarget(at(test, 5, 1));
        tick(test, tower);
        test.assertTrue(tower.getTargetCount() == 3 && receiver.energy().getImaginaryEnergyStored() == 10000,
                "Only the invalid loaded target is removed, while the healthy target still receives energy");
        test.assertTrue(tower.targets().contains(unloaded) && tower.targets().contains(absent)
                && !test.getLevel().hasChunkAt(unloaded.pos()), "Offline targets are retained without loading chunks");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void fullAndPartialNativeTargetsStayIndependent(GameTestHelper test) {
        var tower = tower(test);
        var full = beacon(test, 1, 1);
        full.energy().setImaginaryEnergy(ImaginaryCondensingBeaconBlockEntity.CAPACITY);
        var receiver = beacon(test, 5, 1);
        test.setBlock(new BlockPos(1, 1, 5), Blocks.GOLD_BLOCK);
        tower.addTarget(at(test, 1, 1)); tower.addTarget(at(test, 1, 5)); tower.addTarget(at(test, 5, 1));
        tick(test, tower);
        test.assertTrue(tower.getTargetCount() == 3 && receiver.energy().getImaginaryEnergyStored() == 10000,
                "Full and partial targets must not stop subsequent transfers or lose their bindings");
        test.assertTrue(TowerCheckCapabilities.IE.get(at(test, 1, 5)).getImaginaryEnergyStored() == 2500
                && !TowerCheckCapabilities.FE.containsKey(at(test, 1, 5)),
                "Shared native capability is called once; accepting IE excludes FE for only this target");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void invalidatedCacheRemovesOnlyBrokenTarget(GameTestHelper test) {
        var tower = tower(test);
        var receiver = beacon(test, 5, 1);
        test.setBlock(new BlockPos(1, 1, 1), Blocks.IRON_BLOCK);
        tower.addTarget(at(test, 1, 1)); tower.addTarget(at(test, 5, 1));
        tick(test, tower);
        test.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        // This fixture exposes capabilities on a block without a block entity, so the provider must notify caches.
        test.getLevel().invalidateCapabilities(at(test, 1, 1).pos());
        tick(test, tower);
        test.assertTrue(tower.getTargetCount() == 1 && receiver.energy().getImaginaryEnergyStored() == 20000,
                "Breaking a cached target must preserve the other target and its transfer");
        test.succeed();
    }

    @GameTest(template = "tower_empty")
    public static void targetListPersistsAndLegacyTargetMigrates(GameTestHelper test) {
        var tower = tower(test);
        tower.addTarget(at(test, 1, 1)); tower.addTarget(at(test, 5, 1));
        var saved = tower.saveWithoutMetadata(test.getLevel().registryAccess());
        var restored = tower(test);
        restored.loadCustomOnly(saved, test.getLevel().registryAccess());
        test.assertTrue(restored.targets().equals(tower.targets()) && !saved.contains("Target"), "All targets must round-trip");
        var legacy = new CompoundTag();
        legacy.put("Target", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, at(test, 1, 1)).getOrThrow());
        restored.loadCustomOnly(legacy, test.getLevel().registryAccess());
        test.assertTrue(restored.getTargetCount() == 1 && restored.target().equals(at(test, 1, 1)), "Old single-target saves must migrate");
        restored.addTarget(at(test, 5, 1));
        test.assertTrue(restored.getTargetCount() == 2, "Migrated towers must allow adding targets");
        test.succeed();
    }
}
