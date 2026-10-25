package org.lyy.lyycore.content.menu;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.blockEntities.SpacetimeMatrixBlockEntity;
import org.lyy.lyycore.content.energy.PowerChannels;
import org.lyy.lyycore.network.PowerChannelNetwork;
import org.lyy.lyycore.registry.LyyMenus;

public final class SpacetimeMatrixMenu extends MachineMenu {
    public static final int WIDTH = 292, HEIGHT = 244, MACHINE_SLOTS = 11;
    public final SpacetimeMatrixBlockEntity be;
    private final Player viewer;
    private long lastArmorTick = Long.MIN_VALUE, revision = -1, lastPower = -1;
    private int lastStage = -1;
    private PowerChannelNetwork.Snapshot snapshot;

    public SpacetimeMatrixMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (SpacetimeMatrixBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }
    public SpacetimeMatrixMenu(int id, Inventory inventory, SpacetimeMatrixBlockEntity be) {
        super(LyyMenus.SPACETIME_MATRIX.get(), id);
        this.be = be; viewer = inventory.player;
        snapshot = new PowerChannelNetwork.Snapshot(id, PowerChannelNetwork.NONE, List.of(), List.of(), 0, 0);
        EquipmentSlot[] armor = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < armor.length; i++) {
            var equipment = armor[i];
            addSlot(new Slot(inventory, 39 - i, 20, 44 + i * 20) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(equipment, viewer); }
                @Override public boolean mayPickup(Player player) {
                    return (player.isCreative() || !EnchantmentHelper.has(getItem(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(player);
                }
                @Override public void setByPlayer(ItemStack stack, ItemStack previous) {
                    viewer.onEquipItem(equipment, previous, stack); super.setByPlayer(stack, previous);
                }
            });
        }
        for (int i = 0; i < 6; i++) addSlot(new BlockEntityItemSlot(be, be.items, i, 52 + i % 3 * 20, 54 + i / 3 * 24));
        addSlot(new BlockEntityItemSlot(be, be.items, 6, 132, 113));
        addPlayerSlots(inventory, 65, 154);
        if (viewer instanceof ServerPlayer server) be.ensureChannel(server);
    }
    public PowerChannelNetwork.Snapshot snapshot() { return snapshot; }
    public void update(PowerChannelNetwork.Snapshot snapshot) { this.snapshot = snapshot; }
    public void handle(ServerPlayer player, PowerChannelNetwork.Request request) {
        if (request.action() != PowerChannelNetwork.Action.UPDATE || !stillValid(player)) return;
        var channel = be.ensureChannel(player);
        if (channel != null && channel.id.equals(request.channel()))
            PowerChannels.get(player.serverLevel()).update(channel.id, player.getUUID(), request.name(), request.publicChannel());
        revision = -1; broadcastChanges();
    }
    @Override public void broadcastChanges() {
        if (viewer instanceof ServerPlayer server && stillValid(server)) {
            long tick = server.level().getGameTime();
            if (lastArmorTick != tick) {
                lastArmorTick = tick;
                boolean charged = false;
                for (var stack : server.getArmorSlots()) charged |= be.chargeItem(stack);
                if (charged) server.getInventory().setChanged();
            }
            var directory = PowerChannels.get(server.serverLevel());
            var channel = be.ensureChannel(server);
            if (channel != null && (revision != directory.revision() || lastPower != be.power() || lastStage != be.stage())) {
                boolean visible = channel.visibleTo(viewer.getUUID());
                PacketDistributor.sendToPlayer(server, new PowerChannelNetwork.Snapshot(containerId, visible ? channel.id : PowerChannelNetwork.NONE,
                        visible ? List.of(PowerChannelNetwork.entry(channel, viewer.getUUID())) : List.of(), List.of(), be.power(), be.stage()));
                revision = directory.revision(); lastPower = be.power(); lastStage = be.stage();
            }
        }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) {
        return be != null && !be.isRemoved() && player.isAlive() && !player.isSpectator() && player.level() == be.getLevel()
                && player.distanceToSqr(be.getBlockPos().getCenter()) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < MACHINE_SLOTS) return moveStack(player, index, MACHINE_SLOTS, slots.size(), true);
        var stack = slots.get(index).getItem();
        // Upgrade acceptance and chargeability are defined by the matrix's item handler.
        var moved = moveStack(player, index, 10, 11, false);
        if (!moved.isEmpty()) return moved;
        moved = moveStack(player, index, 4, 10, false);
        if (!moved.isEmpty()) return moved;
        for (int i = 0; i < 4; i++) if (!slots.get(i).hasItem() && slots.get(i).mayPlace(stack))
            return moveStack(player, index, i, i + 1, false);
        return movePlayerStack(player, index);
    }
}
