package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.lyy.lyycore.registry.LyyBlockEntities;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCollectorBlockEntity extends BlockEntity {
    public enum Mode {
        SMALL(2), LARGE(4);
        public final int radius;
        Mode(int r) { this.radius = r; }
        public Mode next() { return this == SMALL ? LARGE : SMALL; }
    }

    private Mode mode = Mode.SMALL;
    @Nullable private BlockPos cachedTarget;
    private static final int INTERVAL_T = 20;

    public ItemCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(LyyBlockEntities.ITEM_COLLECTOR.get(), pos, state);
    }

    public Mode toggleMode() {
        this.mode = this.mode.next();
        setChanged();
        return this.mode;
    }

    public Mode getMode() { return mode; }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", mode == Mode.SMALL ? 0 : 1);
        if (cachedTarget != null) {
            tag.putBoolean("HasTarget", true);
            tag.putInt("Tx", cachedTarget.getX());
            tag.putInt("Ty", cachedTarget.getY());
            tag.putInt("Tz", cachedTarget.getZ());
        } else {
            tag.putBoolean("HasTarget", false);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mode = tag.getInt("Mode") == 0 ? Mode.SMALL : Mode.LARGE;
        if (tag.getBoolean("HasTarget")) {
            this.cachedTarget = new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz"));
        } else {
            this.cachedTarget = null;
        }
    }

    // --- Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemCollectorBlockEntity be) {
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % INTERVAL_T != 0) return;

        if (!be.isCachedTargetValid(level)) {
            be.cachedTarget = be.findNearestContainerPos(level, 3);
        }
        if (be.cachedTarget == null) return;

        int r = be.mode.radius;
        AABB range = new AABB(
                pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + r + 1, pos.getY() + r + 1, pos.getZ() + r + 1);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, range,
                e -> e != null && !e.getItem().isEmpty() && e.isAlive());
        if (items.isEmpty()) return;

        IItemHandler handler = getTargetHandler(level, be.cachedTarget);
        if (handler == null) { be.cachedTarget = null; return; }

        boolean anyInserted = false;
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            var stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            var simRemainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true);
            if (simRemainder.getCount() == stack.getCount()) {
                BlockPos newTarget = be.findNearestContainerPos(level, 1);
                if (newTarget != null && !newTarget.equals(be.cachedTarget)) {
                    be.cachedTarget = newTarget;
                    handler = getTargetHandler(level, be.cachedTarget);
                } else {
                    handler = getTargetHandler(level, be.cachedTarget);
                }
            }
            if (handler == null) break;

            var remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            if (remainder.isEmpty()) {
                itemEntity.discard();
                anyInserted = true;
            } else if (remainder.getCount() < stack.getCount()) {
                itemEntity.setItem(remainder);
                anyInserted = true;
            }
        }
        if (anyInserted) be.setChanged();
    }

    private boolean isCachedTargetValid(Level level) {
        if (cachedTarget == null) return false;
        return getTargetHandler(level, cachedTarget) != null;
    }

    @Nullable
    private static IItemHandler getTargetHandler(Level level, BlockPos targetPos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, null);
    }

    @Nullable
    private BlockPos findNearestContainerPos(Level level, int radius) {
        BlockPos origin = getBlockPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist2 = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getCapability(Capabilities.ItemHandler.BLOCK, cursor, null) == null) continue;
                    double d2 = cursor.distSqr(origin);
                    if (d2 < bestDist2) {
                        bestDist2 = d2;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
