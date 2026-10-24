package org.lyy.lyycore.content.menu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.blockEntities.FissionFurnaceBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;
public final class FissionFurnaceMenu extends MachineMenu {
    private final FissionFurnaceBlockEntity furnace;
    private final ContainerData data;
    public FissionFurnaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (FissionFurnaceBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
    }
    public FissionFurnaceMenu(int id, Inventory inv, FissionFurnaceBlockEntity furnace, ContainerData data) {
        super(LyyMenus.FISSION_FURNACE.get(), id); this.furnace = furnace; this.data = data; addDataSlots(data);
        addSlot(new BlockEntityItemSlot(furnace, furnace.items, 0, 35, 40));
        for (int i = 1; i <= 2; i++) addSlot(new BlockEntityItemSlot(furnace, furnace.items, i, 88 + i * 22, 40) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); furnace.awardExperience(); }
        });
        addPlayerSlots(inv, 8, 96);
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    @Override public boolean stillValid(Player p) { return !furnace.isRemoved() && p.level() == furnace.getLevel() && p.distanceToSqr(furnace.getBlockPos().getCenter()) < 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        return index < 3 ? moveStack(player, index, 3, slots.size(), true) : moveStack(player, index, 0, 1, false);
    }
}
