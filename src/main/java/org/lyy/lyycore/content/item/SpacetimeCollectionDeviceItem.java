package org.lyy.lyycore.content.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.logistics.LogisticsChannels;
import org.lyy.lyycore.content.menu.SpacetimeLocatorMenu;
import org.lyy.lyycore.network.SpacetimeLocatorNetwork;

/** Only selection settings live on the tool; completed bindings belong to the channel. */
public final class SpacetimeCollectionDeviceItem extends Item {
    private static final String CHANNEL = "LogisticsChannel", OUTPUT = "LocatorOutput", AREA = "LocatorArea", CORNER = "LocatorCorner";
    public SpacetimeCollectionDeviceItem(Properties properties) { super(properties); }
    private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static UUID channel(ItemStack stack) { var tag = data(stack); return tag.hasUUID(CHANNEL) ? tag.getUUID(CHANNEL) : null; }
    public static boolean output(ItemStack stack) { return data(stack).getBoolean(OUTPUT); }
    public static boolean area(ItemStack stack) { return data(stack).getBoolean(AREA) && !output(stack); }
    public static void configure(ItemStack stack, UUID channel, boolean output, boolean area) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (channel == null) tag.remove(CHANNEL); else tag.putUUID(CHANNEL, channel);
            tag.putBoolean(OUTPUT, output); tag.putBoolean(AREA, area && !output); tag.remove(CORNER);
        });
    }
    private static GlobalPos corner(ItemStack stack) {
        var tag = data(stack).getCompound(CORNER);
        var dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        return dimension == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(tag.getLong("Position")));
    }
    private static void corner(ItemStack stack, GlobalPos position) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (position == null) tag.remove(CORNER);
            else {
                var point = new CompoundTag(); point.putString("Dimension", position.dimension().location().toString());
                point.putLong("Position", position.pos().asLong()); tag.put(CORNER, point);
            }
        });
    }
    private static void select(ServerPlayer player, ItemStack stack, BlockPos pos, Direction side) {
        var directory = LogisticsChannels.get(player.serverLevel());
        var channel = directory.channel(channel(stack));
        if (channel == null || !channel.visibleTo(player.getUUID())) {
            configure(stack, null, output(stack), area(stack));
            player.getInventory().setChanged();
            message(player, "select_channel"); return;
        }
        var address = GlobalPos.of(player.level().dimension(), pos);
        if (area(stack)) {
            var first = corner(stack);
            if (first == null || !first.dimension().equals(address.dimension())) {
                corner(stack, address); message(player, "first", pos.getX(), pos.getY(), pos.getZ());
            } else {
                var areas = new ArrayList<>(channel.collectionAreas());
                areas.add(new LogisticsChannels.CollectionArea(first, pos));
                directory.setCollectionAreas(channel.id, areas);
                corner(stack, null); message(player, "second", pos.getX(), pos.getY(), pos.getZ());
            }
        } else {
            var target = new LogisticsChannels.BlockTarget(address, side);
            if (target.inventory(player.serverLevel()) == null) { message(player, "no_inventory"); return; }
            var targets = new ArrayList<>(output(stack) ? channel.outputTargets() : channel.inputTargets());
            boolean added = !targets.contains(target);
            if (added) { targets.add(target); directory.setBlockTargets(channel.id, output(stack), targets); }
            message(player, added ? "bound" : "duplicate");
        }
        player.getInventory().setChanged();
        PacketDistributor.sendToPlayer(player, new SpacetimeLocatorNetwork.Highlight(address));
    }
    private static void message(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.lyycore.spacetime_locator." + key, args), true);
    }
    private static void open(ServerPlayer player, InteractionHand hand) {
        if (player.isSpectator()) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new SpacetimeLocatorMenu(id, inventory, hand),
                Component.translatable("item.lyycore.spacetime_collection_device")), buffer -> buffer.writeEnum(hand));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server && !player.isSpectator()) {
            if (!player.isShiftKeyDown()) open(server, hand);
            else if (area(stack) && corner(stack) != null)
                select(server, stack, BlockPos.containing(player.getX(), player.getY() - .01, player.getZ()), Direction.UP);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null || context.getPlayer().isSpectator()) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) select(player, stack, context.getClickedPos(), context.getClickedFace());
            else open(player, context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) { return onItemUseFirst(context.getItemInHand(), context); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore.spacetime_collection_device").withColor(0x85C9E4));
    }
}
