package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.ErosionFactoryBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class ErosionFactoryMenu extends MachineMenu {
    private final ErosionFactoryBlockEntity factory;
    private final ContainerData data;
    public ErosionFactoryMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (ErosionFactoryBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
    }
    public ErosionFactoryMenu(int id, Inventory inv, ErosionFactoryBlockEntity factory, ContainerData data) {
        super(LyyMenus.EROSION_FACTORY.get(), id); this.factory = factory; this.data = data; addDataSlots(data);
        addSlot(new BlockEntityItemSlot(factory, factory.items, 0, 24, 40));
        addSlot(new BlockEntityItemSlot(factory, factory.items, 1, 76, 40));
        addSlot(new BlockEntityItemSlot(factory, factory.items, 2, 136, 40) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addPlayerSlots(inv, 8, 96);
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    @Override public boolean stillValid(Player p) { return p.isAlive() && !factory.isRemoved() && p.level() == factory.getLevel() && p.distanceToSqr(factory.getBlockPos().getCenter()) < 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 3) return moveStack(player, index, 3, slots.size(), true);
        int destination = factory.items.isItemValid(0, slots.get(index).getItem()) ? 0 : 1;
        return moveStack(player, index, destination, destination + 1, false);
    }
}
