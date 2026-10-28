package org.lyy.lyycore.content;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import org.lyy.lyycore.energy.ImaginaryEnergyStorage;

/** Shared direct production for energy-backed catalog machines. Only accepted items cost energy. */
public final class ItemProduction {
    public static void output(
            Level level,
            BlockPos pos,
            Direction front,
            ItemStack product,
            ImaginaryEnergyStorage energy,
            int cost) {
        if (product.isEmpty() || energy.getImaginaryEnergyStored() < cost) return;
        for (var side : new Direction[] {front.getCounterClockWise(), front.getClockWise()}) {
            var targetPos = pos.relative(side);
            if (!level.hasChunkAt(targetPos)) continue;
            var target =
                    level.getCapability(
                            Capabilities.ItemHandler.BLOCK, targetPos, side.getOpposite());
            if (target == null) continue;
            for (int slot = 0;
                    slot < target.getSlots() && energy.getImaginaryEnergyStored() >= cost;
                    slot++) {
                var current = target.getStackInSlot(slot);
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, product))
                    continue;
                int count =
                        Math.min(
                                energy.getImaginaryEnergyStored() / cost,
                                Math.max(0, target.getSlotLimit(slot) - current.getCount()));
                if (count == 0) continue;
                var remaining = target.insertItem(slot, product.copyWithCount(count), false);
                energy.setImaginaryEnergy(
                        energy.getImaginaryEnergyStored() - (count - remaining.getCount()) * cost);
            }
        }
    }
}
