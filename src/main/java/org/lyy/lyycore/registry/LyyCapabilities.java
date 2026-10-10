package org.lyy.lyycore.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.energy.ImaginaryEnergy;

public class LyyCapabilities {
    public static final BlockCapability<ImaginaryEnergy, Direction> IMAGINARY_ENERGY = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "imaginary_energy"), ImaginaryEnergy.class);

    private static org.lyy.lyycore.content.blockEntities.PureSmeltingPlantBlockEntity plant(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (state.getValue(org.lyy.lyycore.content.blocks.LargeStructureBlock.PART) != org.lyy.lyycore.content.blocks.LargeStructureBlock.FRONT) return null;
        var center = org.lyy.lyycore.content.blocks.LargeStructureBlock.center(pos, state);
        return level.getBlockEntity(center) instanceof org.lyy.lyycore.content.blockEntities.PureSmeltingPlantBlockEntity plant && plant.active() ? plant : null;
    }
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LyyBlockEntities.EXPERIMENT_TABLE.get(), (be, side) -> be.items());
        event.registerBlockEntity(IMAGINARY_ENERGY, LyyBlockEntities.OTHERWORLD_CHEST.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LyyBlockEntities.OTHERWORLD_CHEST.get(), (be, side) -> be.fe);
        event.registerBlock(IMAGINARY_ENERGY, (level, pos, state, be, side) -> {
            var center = org.lyy.lyycore.content.blocks.SquareMachineBlock.center(pos, state);
            return level.getBlockEntity(center) instanceof org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity factory ? factory.energy : null;
        }, LyyBlocks.EROSION_FACTORY.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            var center = org.lyy.lyycore.content.blocks.SquareMachineBlock.center(pos, state);
            return level.getBlockEntity(center) instanceof org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity factory ? factory.fe : null;
        }, LyyBlocks.EROSION_FACTORY.get());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            var center = org.lyy.lyycore.content.blocks.SquareMachineBlock.center(pos, state);
            return level.getBlockEntity(center) instanceof org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity factory ? factory.automation : null;
        }, LyyBlocks.EROSION_FACTORY.get());
        event.registerBlockEntity(IMAGINARY_ENERGY, LyyBlockEntities.FISSION_FURNACE.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LyyBlockEntities.FISSION_FURNACE.get(), (be, side) -> be.fe);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LyyBlockEntities.FISSION_FURNACE.get(), (be, side) -> be.automation);
        event.registerBlock(IMAGINARY_ENERGY, (level, pos, state, be, side) -> { var plant = plant(level, pos, state); return plant == null ? null : plant.energy; }, LyyBlocks.PURE_SMELTING_PLANT.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> { var plant = plant(level, pos, state); return plant == null ? null : plant.fe; }, LyyBlocks.PURE_SMELTING_PLANT.get());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> { var plant = plant(level, pos, state); return plant == null ? null : plant.automation; }, LyyBlocks.PURE_SMELTING_PLANT.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LyyBlockEntities.IMAGINARY_DRAGON_NEST.get(), (be, side) -> be.output);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LyyBlockEntities.ALLOY_CAULDRON.get(), (be, side) -> be.water);
        event.registerBlock(IMAGINARY_ENERGY, (level, pos, state, be, side) -> {
            var center = org.lyy.lyycore.content.blocks.SquareMachineBlock.center(pos, state);
            return level.getBlockEntity(center) instanceof org.lyy.lyycore.content.blockEntities.ImaginaryCondensingBeaconBlockEntity beacon ? beacon.energy() : null;
        }, LyyBlocks.IMAGINARY_CONDENSING_BEACON.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            var center = org.lyy.lyycore.content.blocks.SquareMachineBlock.center(pos, state);
            return level.getBlockEntity(center) instanceof org.lyy.lyycore.content.blockEntities.ImaginaryCondensingBeaconBlockEntity beacon ? beacon.fe() : null;
        }, LyyBlocks.IMAGINARY_CONDENSING_BEACON.get());
        event.registerBlockEntity(IMAGINARY_ENERGY, LyyBlockEntities.RESOURCE_GATHERING_FRAME.get(),
                (be, side) -> be.getImaginaryEnergyStorage());
        event.registerBlockEntity(IMAGINARY_ENERGY, LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get(),
                (be, side) -> be.getImaginaryEnergyStorage());
        event.registerBlockEntity(IMAGINARY_ENERGY, LyyBlockEntities.IMAGINARY_ENERGY_CELL.get(),
                (be, side) -> be.getImaginaryEnergyStorage());
        event.registerBlockEntity(IMAGINARY_ENERGY, LyyBlockEntities.IAF.get(),
                (be, side) -> be.getImaginaryEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LyyBlockEntities.RESOURCE_GATHERING_FRAME.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LyyBlockEntities.RESOURCE_GATHERING_FRAME.get(),
                (be, side) -> be.getOutput());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LyyBlockEntities.CRYSTAL_CONDENSING_FRAME.get(),
                (be, side) -> be.getOutput());
        // EnergyCellBlock: energy + items
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                LyyBlockEntities.IMAGINARY_ENERGY_CELL.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                LyyBlockEntities.IMAGINARY_ENERGY_CELL.get(),
                (be, side) -> be.getItemHandler());

        // ImaginaryAlloyForge: energy + items
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                LyyBlockEntities.IAF.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                LyyBlockEntities.IAF.get(),
                (be, side) -> be.getAutomationItemHandler(side));
    }
}
