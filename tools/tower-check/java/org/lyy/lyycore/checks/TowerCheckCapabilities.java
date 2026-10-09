package org.lyy.lyycore.checks;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.energy.ImaginaryEnergyStorage;
import org.lyy.lyycore.registry.LyyCapabilities;

/** Test-only receivers expose the same storage on every face, as many real machines do. */
@EventBusSubscriber(modid = LyyCore.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class TowerCheckCapabilities {
    static final Map<GlobalPos, EnergyStorage> FE = new HashMap<>();
    static final Map<GlobalPos, ImaginaryEnergyStorage> IE = new HashMap<>();

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> FE.computeIfAbsent(GlobalPos.of(level.dimension(), pos),
                        ignored -> new EnergyStorage(10_000_000)), Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK);
        event.registerBlock(LyyCapabilities.IMAGINARY_ENERGY,
                (level, pos, state, be, side) -> IE.computeIfAbsent(GlobalPos.of(level.dimension(), pos),
                        ignored -> new ImaginaryEnergyStorage(1_000_000, 2500, 0)), Blocks.GOLD_BLOCK);
    }
}
