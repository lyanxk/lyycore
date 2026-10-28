package org.lyy.lyycore.content.finality;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import org.lyy.lyycore.content.energy.PowerChannels;

import java.util.*;

/** Settings stay bound to the exact stack in the selected hand or armor slot. */
public final class FinalitySettingsMenu extends AbstractContainerMenu {
    public record Channel(UUID id, String name) {}

    public static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static final int SELECT_ARMOR = 1000;
    public EquipmentSlot slot;
    public final List<Channel> channels;
    public ItemStack item;
    private final Player owner;
    private final ContainerData data;

    public FinalitySettingsMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(
                id,
                inventory,
                buf.readEnum(EquipmentSlot.class),
                buf.readList(b -> new Channel(b.readUUID(), b.readUtf())),
                false);
    }

    private FinalitySettingsMenu(
            int id,
            Inventory inventory,
            EquipmentSlot slot,
            List<Channel> channels,
            boolean server) {
        super(FinalityContent.SETTINGS.get(), id);
        owner = inventory.player;
        this.slot = slot;
        this.channels = channels;
        item = inventory.player.getItemBySlot(slot);
        data =
                server
                        ? new ContainerData() {
                            public int get(int i) {
                                if (i == 3) return FinalitySettingsMenu.this.slot.ordinal();
                                if (i == 0)
                                    return optionFlags();
                                if (i == 1)
                                    return (int) Math.round(FinalityArmorItem.speed(item) * 100);
                                var selected = PortableEnergyItem.channel(item);
                                for (int j = 0; j < channels.size(); j++)
                                    if (channels.get(j).id().equals(selected)) return j;
                                return -1;
                            }

                            public void set(int i, int value) {}

                            public int getCount() {
                                return 4;
                            }
                        }
                        : new SimpleContainerData(4);
        if (!server) {
            data.set(0, optionFlags());
            data.set(1, (int) Math.round(FinalityArmorItem.speed(item) * 100));
            data.set(2, -1);
            data.set(3, slot.ordinal());
        }
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        open(player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    public static boolean canConfigure(Player player, EquipmentSlot slot) {
        var item = player.getItemBySlot(slot).getItem();
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
            return item instanceof FinalityArmorItem || item instanceof PortableEnergyItem;
        return ARMOR_SLOTS.contains(slot) && item instanceof FinalityArmorItem armor
                && armor.getEquipmentSlot() == slot;
    }

    public static void open(ServerPlayer player, EquipmentSlot slot) {
        if (!player.isAlive() || player.isSpectator() || !canConfigure(player, slot)) return;
        var stack = player.getItemBySlot(slot);
        var channels =
                stack.getItem() instanceof PortableEnergyItem
                        ? PowerChannels.get(player.serverLevel())
                                .visibleTo(player.getUUID())
                                .stream()
                                .map(c -> new Channel(c.id, c.name()))
                                .toList()
                        : List.<Channel>of();
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new FinalitySettingsMenu(id, inv, slot, channels, true),
                        stack.getHoverName()),
                buf -> {
                    buf.writeEnum(slot);
                    buf.writeCollection(
                            channels,
                            (b, c) -> {
                                b.writeUUID(c.id());
                                b.writeUtf(c.name());
                            });
                });
    }

    public boolean enabled(int option) {
        return (data.get(0) & (1 << option)) != 0;
    }

    private int optionFlags() {
        int flags = 0;
        for (int i = 0; i < FinalityArmorItem.optionCount(item); i++)
            if (FinalityArmorItem.enabled(item, i)) flags |= 1 << i;
        return flags;
    }

    public double speed() {
        return data.get(1) / 100.0;
    }

    public int selectedChannel() {
        return data.get(2);
    }

    private void selectEquipment(EquipmentSlot selected) {
        slot = selected;
        item = owner.getItemBySlot(selected);
    }

    @Override
    public void setData(int index, int value) {
        super.setData(index, value);
        if (index == 3) selectEquipment(EquipmentSlot.values()[value]);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator() && player.getItemBySlot(slot) == item;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || !stillValid(player) || player.isSpectator())
            return false;
        if (item.getItem() instanceof FinalityArmorItem) {
            if (id >= SELECT_ARMOR && id < SELECT_ARMOR + ARMOR_SLOTS.size()) {
                var selected = ARMOR_SLOTS.get(id - SELECT_ARMOR);
                if (!canConfigure(player, selected)) return false;
                if (selected != slot) {
                    selectEquipment(selected);
                    broadcastChanges();
                }
                return true;
            }
            if (id >= 0 && id < FinalityArmorItem.optionCount(item)) FinalityArmorItem.toggle(item, id);
            else if (item.getItem() instanceof FinalityArmorItem armor
                    && armor.getEquipmentSlot() == EquipmentSlot.FEET && id >= 100 && id <= 400)
                FinalityArmorItem.speed(item, id / 100.0);
            else return false;
        } else if (id == -1) PortableEnergyItem.bind(item, null);
        else if (id >= 0 && id < channels.size()) {
            var channel = PowerChannels.get(server.serverLevel()).channel(channels.get(id).id());
            if (channel == null || !channel.visibleTo(player.getUUID())) return false;
            PortableEnergyItem.bind(item, channel.id);
        } else return false;
        player.getInventory().setChanged();
        broadcastChanges();
        return true;
    }
}
