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
import org.lyy.lyycore.energy.CoordinateEnergySource;
import java.util.List;

public final class CoordinateDeviceItem extends Item {
    public CoordinateDeviceItem(Properties properties) { super(properties); }
    public static GlobalPos source(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("EnergySource") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("EnergySource")).result().orElse(null) : null;
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Bind before machines open their GUI or accept held items as ingredients.
        return useOn(context);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var pos = context.getClickedPos();
        var controller = EnergyReceiver.controller(level, pos);
        if (level.hasChunkAt(controller) && level.getBlockEntity(controller) instanceof CoordinateEnergySource) {
            var source = GlobalPos.of(level.dimension(), controller);
            CustomData.update(DataComponents.CUSTOM_DATA, context.getItemInHand(), tag -> {
                tag.remove("Target"); // Old devices recorded receivers, not energy sources.
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, source).result().ifPresent(value -> tag.put("EnergySource", value));
            });
            player.displayClientMessage(Component.translatable("message.lyycore.coordinates.saved", describe(source)), true);
            return InteractionResult.CONSUME;
        }

        var source = source(context.getItemInHand());
        var sourceLevel = source == null ? null : level.getServer().getLevel(source.dimension());
        if (sourceLevel == null) {
            player.displayClientMessage(Component.translatable("message.lyycore.coordinates.empty"), true);
            return InteractionResult.CONSUME;
        }
        if (!sourceLevel.hasChunkAt(source.pos())) {
            player.displayClientMessage(Component.translatable("message.lyycore.coordinates.unloaded"), true);
            return InteractionResult.CONSUME;
        }
        if (!(sourceLevel.getBlockEntity(source.pos()) instanceof CoordinateEnergySource generator)) {
            player.displayClientMessage(Component.translatable("message.lyycore.coordinates.empty"), true);
            return InteractionResult.CONSUME;
        }
        if (!EnergyReceiver.supportsEnergy(level, pos)) {
            player.displayClientMessage(Component.translatable("message.lyycore.coordinates.unsupported"), true);
            return InteractionResult.CONSUME;
        }
        var target = GlobalPos.of(level.dimension(), pos);
        var message = switch (generator.bindTarget(target)) {
            case ADDED -> Component.translatable("message.lyycore.coordinates.bound", describe(target), generator.getTargetCount());
            case ALREADY_BOUND -> Component.translatable("message.lyycore.coordinates.already_bound", describe(target));
            case LIMIT_REACHED -> Component.translatable("message.lyycore.coordinates.limit", generator.maxTargets());
        };
        player.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
    }
    private static String describe(GlobalPos target) { return target.dimension().location() + " · " + target.pos().toShortString(); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var source = source(stack);
        lines.add(Component.translatable("tooltip.lyycore.coordinates.use"));
        lines.add(Component.translatable("tooltip.lyycore.coordinates.limit"));
        if (source != null) lines.add(Component.translatable("message.lyycore.coordinates.saved", describe(source)));
    }
}
