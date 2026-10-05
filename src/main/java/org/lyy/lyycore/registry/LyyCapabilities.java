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

    public static void register(RegisterCapabilitiesEvent event) {
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
