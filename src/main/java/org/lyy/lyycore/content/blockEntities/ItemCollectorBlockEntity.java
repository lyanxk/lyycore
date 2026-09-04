package org.lyy.lyycore.content.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.lyy.lyycore.Config;
import org.lyy.lyycore.registry.LyyBlockEntities;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCollectorBlockEntity extends BlockEntity {
    public enum Mode {
        SMALL, LARGE;
        public Mode next() { return this == SMALL ? LARGE : SMALL; }
        public int radius() {
            return this == SMALL ? Config.COLLECTOR_SMALL_RADIUS.get() : Config.COLLECTOR_LARGE_RADIUS.get();
        }
    }

    private Mode mode = Mode.SMALL;
    @Nullable private Target cachedTarget;
    private long nextTargetSearch;

    private record Target(BlockPos pos, @Nullable Direction side) { }

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mode = tag.getInt("Mode") == 0 ? Mode.SMALL : Mode.LARGE;
        this.cachedTarget = null;
        this.nextTargetSearch = 0;
    }

    // --- Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemCollectorBlockEntity be) {
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % Config.COLLECTOR_TICK_INTERVAL.get() != 0) return;

        int r = be.mode.radius();
        AABB range = new AABB(
                pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + r + 1, pos.getY() + r + 1, pos.getZ() + r + 1);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, range,
                e -> e != null && !e.getItem().isEmpty() && e.isAlive());
        if (items.isEmpty()) return;

        if (!be.isCachedTargetValid(level)) be.cachedTarget = null;
        if (be.cachedTarget == null) {
            if (level.getGameTime() < be.nextTargetSearch) return;
            be.cachedTarget = be.findNearestContainer(level, Config.COLLECTOR_TARGET_RADIUS.get(),
                    items.getFirst().getItem(), null);
            if (be.cachedTarget == null) {
                be.nextTargetSearch = level.getGameTime() + Config.COLLECTOR_SEARCH_BACKOFF.get();
                return;
            }
        }

        IItemHandler handler = getTargetHandler(level, be.cachedTarget);
        if (handler == null) { be.cachedTarget = null; return; }

        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            var stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            var simRemainder = ItemHandlerHelper.insertItemStacked(handler, stack, true);
            if (simRemainder.getCount() == stack.getCount()) {
                Target newTarget = be.findNearestContainer(level, Config.COLLECTOR_TARGET_RADIUS.get(),
                        stack, be.cachedTarget);
                if (newTarget != null) {
                    be.cachedTarget = newTarget;
                    handler = getTargetHandler(level, be.cachedTarget);
                } else continue;
            }
            if (handler == null) break;

            var remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else if (remainder.getCount() < stack.getCount()) {
                itemEntity.setItem(remainder);
            }
        }
    }

    private boolean isCachedTargetValid(Level level) {
        if (cachedTarget == null) return false;
        return getTargetHandler(level, cachedTarget) != null;
    }

    @Nullable
    private static IItemHandler getTargetHandler(Level level, Target target) {
        return level.hasChunkAt(target.pos())
                ? level.getCapability(Capabilities.ItemHandler.BLOCK, target.pos(), target.side())
                : null;
    }

    @Nullable
    private Target findNearestContainer(Level level, int radius, ItemStack stack, @Nullable Target excluded) {
        BlockPos origin = getBlockPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Target best = null;
        double bestDist2 = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;
                    Target candidate = findUsableTarget(level, cursor, stack, excluded);
                    if (candidate == null) continue;
                    double d2 = cursor.distSqr(origin);
                    if (d2 < bestDist2) {
                        bestDist2 = d2;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private static Target findUsableTarget(Level level, BlockPos pos, ItemStack stack, @Nullable Target excluded) {
        Target unsided = new Target(pos.immutable(), null);
        if (!unsided.equals(excluded)) {
            IItemHandler unsidedHandler = getTargetHandler(level, unsided);
            if (unsidedHandler != null && accepts(unsidedHandler, stack)) return unsided;
        }

        for (Direction side : Direction.values()) {
            Target candidate = new Target(pos.immutable(), side);
            if (candidate.equals(excluded)) continue;
            IItemHandler handler = getTargetHandler(level, candidate);
            if (handler != null && accepts(handler, stack)) return candidate;
        }
        return null;
    }

    private static boolean accepts(IItemHandler handler, ItemStack stack) {
        return ItemHandlerHelper.insertItemStacked(handler, stack, true).getCount() < stack.getCount();
    }
}
