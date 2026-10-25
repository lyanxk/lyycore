package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import java.util.ArrayDeque;
import java.util.HashSet;

public final class SuchenItem extends BrushItem {
    public SuchenItem(Properties properties) { super(properties); }

    @Override public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack, UseOnContext context) {
        // Run before the block's own right-click interaction, using the same brushing path.
        return context.getLevel().getBlockEntity(context.getClickedPos()) instanceof BrushableBlockEntity
                ? useOn(context) : InteractionResult.PASS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (context.getPlayer() == null || !(level.getBlockEntity(context.getClickedPos()) instanceof BrushableBlockEntity))
            return super.useOn(context);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!brush(context, context.getClickedPos())) return InteractionResult.FAIL;
        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        visited.add(context.getClickedPos());
        queue.add(context.getClickedPos());
        int processed = 1;
        while (!queue.isEmpty() && processed < 64) {
            var current = queue.removeFirst();
            for (var direction : Direction.values()) {
                var next = current.relative(direction);
                if (!visited.add(next) || !level.hasChunkAt(next)) continue;
                if (brush(context, next)) {
                    queue.addLast(next);
                    if (++processed == 64) break;
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    private static boolean brush(UseOnContext context, BlockPos pos) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (!(level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return false;
        // Supply successive brush times to finish vanilla's ten strokes in this interaction.
        // The completed block is replaced immediately, so no future brush timer survives.
        long time = level.getGameTime() + 10;
        for (int stroke = 0; stroke < 10; stroke++) {
            if (brushable.brush(time + stroke * 10L, player, context.getClickedFace())) {
                level.destroyBlock(pos, true, player);
                return true;
            }
        }
        return false;
    }
}
