package org.lyy.lyycore.content.item;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.lyy.lyycore.energy.EnergyReceiver;
import org.lyy.lyycore.content.blockEntities.TargetedEnergySourceBlockEntity;
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
        var controller = EnergyReceiver.controller(level, pos);
        if (level.hasChunkAt(controller) && level.getBlockEntity(controller) instanceof TargetedEnergySourceBlockEntity tower && !player.isShiftKeyDown()) {
            var target = target(context.getItemInHand());
            if (target == null) player.displayClientMessage(Component.translatable("message.lyycore.coordinates.empty"), true);
            else {
                var result = tower.toggleTarget(target);
                var message = switch (result) {
                    case ADDED -> Component.translatable("message.lyycore.coordinates.bound", describe(target), tower.getTargetCount());
                    case REMOVED -> Component.translatable("message.lyycore.coordinates.removed", describe(target), tower.getTargetCount());
                    case LIMIT_REACHED -> Component.translatable("message.lyycore.coordinates.limit", tower.maxTargets());
                };
                player.displayClientMessage(message, true);
                if (result != TargetedEnergySourceBlockEntity.BindingResult.LIMIT_REACHED)
                    context.getItemInHand().consume(1, player);
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
        lines.add(Component.translatable("tooltip.lyycore.coordinates.limit"));
        if (target != null) lines.add(Component.literal(describe(target)));
    }
}
