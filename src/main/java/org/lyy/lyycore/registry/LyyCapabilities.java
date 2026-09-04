package org.lyy.lyycore.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.lyy.lyycore.content.blockEntities.EnergyCellBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryAlloyForgeBlockEntity;

public class LyyCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
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
