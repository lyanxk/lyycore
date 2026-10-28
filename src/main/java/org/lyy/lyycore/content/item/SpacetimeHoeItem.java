package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;

/** Planting deliberately delegates consumption and placement rules to the seed. */
public final class SpacetimeHoeItem extends HoeItem {
    public SpacetimeHoeItem(Properties properties) {
        super(Tiers.NETHERITE, properties.component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    private static boolean isSeed(ItemStack stack) {
        if (stack.is(Tags.Items.SEEDS)) return true;
        // Carrots, potatoes, wart and berries are planting items too. Redstone and string are not.
        if (!(stack.getItem() instanceof ItemNameBlockItem item)) return false;
        var block = item.getBlock();
        return block instanceof BushBlock || block instanceof CocoaBlock || block instanceof GrowingPlantHeadBlock;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND
                || !isSeed(player.getOffhandItem())) return super.useOn(context);
        var level = context.getLevel();
        var origin = context.getClickedPos();
        var block = level.getBlockState(origin).getBlock();
        var seed = player.getOffhandItem().getItem();
        InteractionResult first = player.getOffhandItem().useOn(seedContext(context, origin));
        // Edible planting items return CONSUME_PARTIAL when placement failed and eating started.
        if (!first.consumesAction() || first == InteractionResult.CONSUME_PARTIAL || level.isClientSide) return first;

        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        visited.add(origin);
        queue.add(origin);
        int processed = 1;
        while (!queue.isEmpty() && processed < 128 && !player.getOffhandItem().isEmpty()
                && player.getOffhandItem().is(seed)) {
            var current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                var next = current.relative(direction);
                if (!visited.add(next) || !level.hasChunkAt(next) || !level.getBlockState(next).is(block)) continue;
                queue.addLast(next);
                processed++;
                if (level.mayInteract(player, next) && player.mayUseItemAt(next, context.getClickedFace(), player.getOffhandItem()))
                    player.getOffhandItem().useOn(seedContext(context, next));
                if (processed >= 128 || player.getOffhandItem().isEmpty() || !player.getOffhandItem().is(seed)) break;
            }
        }
        return first;
    }

    private static UseOnContext seedContext(UseOnContext original, BlockPos pos) {
        Vec3 hit = original.getClickLocation().add(Vec3.atLowerCornerOf(pos.subtract(original.getClickedPos())));
        return new UseOnContext(original.getPlayer(), InteractionHand.OFF_HAND,
                new BlockHitResult(hit, original.getClickedFace(), pos, original.isInside()));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.spacetime_hoe").withColor(0x9CD9F3));
    }
}
