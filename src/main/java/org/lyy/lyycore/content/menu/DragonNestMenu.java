package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class DragonNestMenu extends MachineMenu {
    private final ImaginaryDragonNestBlockEntity nest;
    private final ContainerData data;
    public DragonNestMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (ImaginaryDragonNestBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
    }
    public DragonNestMenu(int id, Inventory inv, ImaginaryDragonNestBlockEntity nest, ContainerData data) {
        super(LyyMenus.DRAGON_NEST.get(), id); this.nest = nest; this.data = data; addDataSlots(data);
        addSlot(new SlotItemHandler(nest.output, 0, 80, 44) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        addPlayerSlots(inv, 8, 106);
    }
    public int progress() { return data.get(0); }
    public int mode() { return Math.clamp(data.get(1), 0, 2); }
    @Override public boolean stillValid(Player p) { return nest != null && !nest.isRemoved() && p.level() == nest.getLevel() && p.distanceToSqr(nest.getBlockPos().getCenter()) < 144; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        return index == 0 ? moveStack(player, index, 1, slots.size(), true) : ItemStack.EMPTY;
    }
}
