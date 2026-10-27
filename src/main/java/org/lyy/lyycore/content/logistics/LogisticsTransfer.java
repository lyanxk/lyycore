package org.lyy.lyycore.content.logistics;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Transfers only accepted quantities; neither channels nor this helper store items. */
public final class LogisticsTransfer {
    private LogisticsTransfer() { }

    public static boolean hasItems(IItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++)
            if (!inventory.getStackInSlot(slot).isEmpty()) return true;
        return false;
    }

    public static boolean hasRoom(IItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize())) return true;
        }
        return false;
    }

    /** Simulate acceptance first, then transfer the actual extraction from the source. */
    public static void move(IItemHandler source, IItemHandler destination) {
        if (source == destination) return;
        for (int slot = 0; slot < source.getSlots(); slot++) {
            var stack = source.extractItem(slot, Integer.MAX_VALUE, true);
            if (stack.isEmpty()) continue;
            int accepted = stack.getCount() - ItemHandlerHelper.insertItemStacked(destination, stack, true).getCount();
            if (accepted > 0) ItemHandlerHelper.insertItemStacked(destination, source.extractItem(slot, accepted, false), false);
        }
    }

    public static void collect(ServerLevel level, AABB range, List<IItemHandler> destinations) {
        if (destinations.stream().noneMatch(LogisticsTransfer::hasRoom)) return;
        for (var entity : level.getEntitiesOfClass(ItemEntity.class, range, e -> e.isAlive() && !e.getItem().isEmpty())) {
            var stack = entity.getItem();
            var remaining = stack;
            for (var destination : destinations) {
                remaining = ItemHandlerHelper.insertItemStacked(destination, remaining, false);
                if (remaining.isEmpty()) break;
            }
            if (remaining.isEmpty()) entity.discard();
            else if (remaining.getCount() != stack.getCount()) entity.setItem(remaining);
        }
    }
}
