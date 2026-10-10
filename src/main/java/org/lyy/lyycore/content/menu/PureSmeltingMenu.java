package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.PureSmeltingPlantBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

/** Only a speed setting is exposed; inventory remains a world interaction. */
public final class PureSmeltingMenu extends AbstractContainerMenu {
    private final PureSmeltingPlantBlockEntity plant;
    private final ContainerData data;
    public PureSmeltingMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (PureSmeltingPlantBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(3));
    }
    public PureSmeltingMenu(int id, Inventory inv, PureSmeltingPlantBlockEntity plant, ContainerData data) {
        super(LyyMenus.PURE_SMELTING.get(), id); this.plant = plant; this.data = data; addDataSlots(data);
    }
    public int speed() { return data.get(0); }
    public int energy() { return (data.get(1) & 65535) | (data.get(2) & 65535) << 16; }
    @Override public boolean stillValid(Player p) { return plant != null && !plant.isRemoved() && plant.active() && p.level() == plant.getLevel() && p.distanceToSqr(plant.getBlockPos().getCenter()) < 144; }
    @Override public boolean clickMenuButton(Player p, int value) {
        if (!stillValid(p) || value < 1 || value > 100) return false;
        plant.speed(value); return true;
    }
    @Override public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }
}
