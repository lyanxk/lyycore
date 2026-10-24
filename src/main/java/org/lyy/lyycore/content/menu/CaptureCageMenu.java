package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.CaptureCageBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class CaptureCageMenu extends MachineMenu {
    private final CaptureCageBlockEntity cage;
    private final ContainerData data;
    public final Component captive;
    public CaptureCageMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (CaptureCageBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(2), ComponentSerialization.STREAM_CODEC.decode(buf));
    }
    public CaptureCageMenu(int id, Inventory inv, CaptureCageBlockEntity cage, ContainerData data, Component captive) {
        super(LyyMenus.CAPTURE_CAGE.get(), id); this.cage = cage; this.data = data; this.captive = captive; addDataSlots(data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new BlockEntityItemSlot(cage, cage.items, row * 9 + col, 8 + col * 18, 34 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        addPlayerSlots(inv, 8, 126);
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    @Override public boolean stillValid(Player p) { return !cage.isRemoved() && p.level() == cage.getLevel() && p.distanceToSqr(cage.getBlockPos().getCenter()) < 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        return index < 27 ? moveStack(player, index, 27, slots.size(), true) : movePlayerStack(player, index);
    }
}
