package org.lyy.lyycore.content.finality;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import org.lyy.lyycore.content.blockEntities.SpacetimeMatrixBlockEntity;
import org.lyy.lyycore.content.energy.PowerChannels;

import java.util.*;

@EventBusSubscriber(modid = "lyycore")
public final class PortableEnergyItem extends Item {
    // Curios exposes this standard item handler for integrations without a hard dependency.
    private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

    public PortableEnergyItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static UUID channel(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID("PowerChannel") ? tag.getUUID("PowerChannel") : null;
    }

    public static void bind(ItemStack stack, UUID id) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    if (id == null) tag.remove("PowerChannel");
                    else tag.putUUID("PowerChannel", id);
                });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) FinalitySettingsMenu.open(server, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isAlive()
                || player.isSpectator()) return;
        var inventory = player.getInventory();
        var curios = player.getCapability(CURIOS_INVENTORY);
        Set<UUID> channels = new HashSet<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            collectChannel(inventory.getItem(slot), channels);
        if (curios != null)
            for (int slot = 0; slot < curios.getSlots(); slot++)
                collectChannel(curios.getStackInSlot(slot), channels);
        boolean changed = false;
        for (var id : channels) {
            var channel = PowerChannels.get(player.serverLevel()).channel(id);
            if (channel == null) continue;
            var level = player.server.getLevel(channel.source.dimension());
            if (level == null
                    || !level.hasChunkAt(channel.source.pos())
                    || !(level.getBlockEntity(channel.source.pos())
                            instanceof SpacetimeMatrixBlockEntity matrix)) continue;
            // Inventory already includes main hand, armor and offhand exactly once.
            for (int slot = 0; slot < inventory.getContainerSize(); slot++)
                changed |= matrix.chargeItem(inventory.getItem(slot));
            if (curios != null)
                for (int slot = 0; slot < curios.getSlots(); slot++)
                    matrix.chargeItem(curios.getStackInSlot(slot));
        }
        if (changed) inventory.setChanged();
    }

    private static void collectChannel(ItemStack stack, Set<UUID> channels) {
        if (stack.getItem() instanceof PortableEnergyItem) {
            var id = channel(stack);
            if (id != null) channels.add(id);
        }
    }
}
