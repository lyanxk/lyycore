package org.lyy.lyycore.content.item;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.lyy.lyycore.content.blocks.SquareMachineBlock;
import org.lyy.lyycore.content.blockEntities.SpatialTransmissionTowerBlockEntity;
import java.util.List;

public final class CoordinateDeviceItem extends Item {
    public CoordinateDeviceItem(Properties properties) { super(properties); }
    public static GlobalPos target(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("Target") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Target")).result().orElse(null) : null;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var controller = state.getBlock() instanceof SquareMachineBlock ? SquareMachineBlock.center(pos, state) : pos;
        if (level.getBlockEntity(controller) instanceof SpatialTransmissionTowerBlockEntity tower && !player.isShiftKeyDown()) {
            var target = target(context.getItemInHand());
            if (target == null) player.displayClientMessage(Component.translatable("message.lyycore.coordinates.empty"), true);
            else {
                boolean added = tower.addTarget(target);
                player.displayClientMessage(Component.translatable(added ? "message.lyycore.coordinates.bound"
                        : "message.lyycore.coordinates.already_bound", describe(target), tower.getTargetCount()), true);
                if (added) context.getItemInHand().consume(1, player);
            }
        } else {
            var target = GlobalPos.of(level.dimension(), pos);
            CustomData.update(DataComponents.CUSTOM_DATA, context.getItemInHand(), tag ->
                    GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().ifPresent(value -> tag.put("Target", value)));
            player.displayClientMessage(Component.translatable("message.lyycore.coordinates.saved", describe(target)), true);
        }
        return InteractionResult.CONSUME;
    }
    private static String describe(GlobalPos target) { return target.dimension().location() + " · " + target.pos().toShortString(); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var target = target(stack);
        lines.add(Component.translatable("tooltip.lyycore.coordinates.use"));
        if (target != null) lines.add(Component.literal(describe(target)));
    }
}
