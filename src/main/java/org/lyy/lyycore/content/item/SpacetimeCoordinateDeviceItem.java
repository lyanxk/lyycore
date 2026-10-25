package org.lyy.lyycore.content.item;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.energy.PowerChannels;
import org.lyy.lyycore.content.menu.SpacetimeCoordinatesMenu;
import org.lyy.lyycore.energy.EnergyReceiver;

/** A reusable coordinate tool; channel targets live in world data, pending targets on the tool. */
public final class SpacetimeCoordinateDeviceItem extends Item {
    private static final String CHANNEL = "SpacetimePowerChannel", PENDING = "SpacetimePendingTargets";
    public SpacetimeCoordinateDeviceItem(Properties properties) { super(properties); }
    private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static UUID channel(ItemStack stack) {
        var tag = data(stack); return tag.hasUUID(CHANNEL) ? tag.getUUID(CHANNEL) : null;
    }
    public static void channel(ItemStack stack, UUID id) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { if (id == null) tag.remove(CHANNEL); else tag.putUUID(CHANNEL, id); });
    }
    public static List<GlobalPos> pending(ItemStack stack) {
        var result = new ArrayList<GlobalPos>();
        var list = data(stack).getList(PENDING, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i); var dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension == null || !entry.contains("Position", Tag.TAG_LONG)) continue;
            var address = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), net.minecraft.core.BlockPos.of(entry.getLong("Position")));
            if (!result.contains(address)) result.add(address);
        }
        return result;
    }
    public static void pending(ItemStack stack, List<GlobalPos> addresses) {
        var list = new ListTag();
        for (var address : addresses.stream().distinct().toList()) {
            var entry = new CompoundTag(); entry.putString("Dimension", address.dimension().location().toString());
            entry.putLong("Position", address.pos().asLong()); list.add(entry);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { if (list.isEmpty()) tag.remove(PENDING); else tag.put(PENDING, list); });
    }
    /** Move only accepted pending positions; a full channel never loses unsent positions. */
    public static void bind(ServerPlayer player, ItemStack tool, UUID id) {
        var directory = PowerChannels.get(player.serverLevel()); var selected = directory.channel(id);
        if (selected == null || !selected.visibleTo(player.getUUID())) return;
        channel(tool, id);
        var remaining = new ArrayList<GlobalPos>();
        for (var address : pending(tool)) {
            var receiver = PowerChannels.receiver(player.serverLevel(), address);
            boolean alreadyBound = selected.targets().stream().anyMatch(existing -> PowerChannels.receiver(player.serverLevel(), existing).equals(receiver));
            if (!alreadyBound && !directory.addTarget(id, player.getUUID(), receiver)) remaining.add(address);
        }
        pending(tool, remaining); player.getInventory().setChanged();
        if (!remaining.isEmpty()) player.displayClientMessage(Component.translatable("message.lyycore.spacetime_coordinates.full"), true);
    }
    private static void add(ServerPlayer player, ItemStack tool, GlobalPos address) {
        var directory = PowerChannels.get(player.serverLevel()); var selected = directory.channel(channel(tool));
        address = PowerChannels.receiver(player.serverLevel(), address);
        String message;
        if (selected != null && selected.visibleTo(player.getUUID())) {
            var receiver = address;
            if (selected.targets().stream().anyMatch(existing -> PowerChannels.receiver(player.serverLevel(), existing).equals(receiver))) message = "duplicate";
            else message = directory.addTarget(selected.id, player.getUUID(), address) ? "added" : "full";
        } else {
            channel(tool, null);
            var pending = new ArrayList<>(pending(tool));
            if (pending.contains(address)) message = "duplicate";
            else { pending.add(address); pending(tool, pending); message = "pending_added"; }
        }
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("message.lyycore.spacetime_coordinates." + message), true);
    }
    private static void open(ServerPlayer player, InteractionHand hand) {
        if (player.isSpectator()) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new SpacetimeCoordinatesMenu(id, inventory, hand),
                Component.translatable("gui.lyycore.spacetime_coordinates.title")), buffer -> buffer.writeEnum(hand));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) open(server, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null || context.getPlayer().isSpectator()) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            if (EnergyReceiver.supportsEnergy(context.getLevel(), context.getClickedPos())) {
                var pos = EnergyReceiver.controller(context.getLevel(), context.getClickedPos());
                add(player, stack, GlobalPos.of(context.getLevel().dimension(), pos));
            } else open(player, context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) { return onItemUseFirst(context.getItemInHand(), context); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.spacetime_coordinate_device").withColor(0x85C9E4));
        int pending = pending(stack).size();
        if (pending > 0) lines.add(Component.translatable("tooltip.lyycore.spacetime_coordinate_device.pending", pending));
    }
}
