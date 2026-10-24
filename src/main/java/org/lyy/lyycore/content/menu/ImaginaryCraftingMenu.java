package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.AbstractImaginaryCraftingBlockEntity;
import org.lyy.lyycore.content.blockEntities.ImaginaryCondensingBeaconBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class ImaginaryCraftingMenu extends MachineMenu {
    public static final int WIDTH = 176, HEIGHT = 240;
    public static final int INVENTORY_LABEL_Y = 147, INVENTORY_Y = 159, HOTBAR_Y = 217;
    public static final int[][] POSITIONS = {{80, 62}, {80, 18}, {111, 31}, {124, 62}, {111, 93},
            {80, 106}, {49, 93}, {36, 62}, {49, 31}};
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final net.minecraft.world.level.block.Block block;
    private final boolean condensing;

    public ImaginaryCraftingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (AbstractImaginaryCraftingBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(7));
    }
    public ImaginaryCraftingMenu(int id, Inventory inventory, AbstractImaginaryCraftingBlockEntity table, ContainerData data) {
        super(LyyMenus.IMAGINARY_CRAFTING.get(), id);
        this.access = ContainerLevelAccess.create(table.getLevel(), table.getBlockPos());
        this.data = data;
        this.block = table.getBlockState().getBlock();
        this.condensing = table instanceof ImaginaryCondensingBeaconBlockEntity;
        addDataSlots(data);
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new SlotItemHandler(table.items(), slot, POSITIONS[slot][0], POSITIONS[slot][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return progress() == 0; }
                @Override public boolean mayPickup(Player player) { return progress() == 0; }
                @Override public int getMaxStackSize() { return 1; }
                @Override public void setByPlayer(ItemStack stack, ItemStack previous) {
                    super.setByPlayer(stack, previous);
                    // Cursor swaps replace the result without calling onTake.
                    if (getSlotIndex() == 0 && !previous.isEmpty()
                            && (!ItemStack.isSameItemSameComponents(stack, previous)
                            || stack.getCount() < previous.getCount()))
                        table.awardCraftedResult(inventory.player, previous);
                }
                @Override public void onTake(Player player, ItemStack stack) {
                    if (getSlotIndex() == 0) table.awardCraftedResult(player, stack);
                    super.onTake(player, stack);
                }
            });
        }
        addPlayerSlots(inventory, 8, INVENTORY_Y);
    }
    public boolean condensing() { return condensing; }
    public boolean energyPerTick() { return data.get(6) != 0; }
    public int duration() { return Math.max(1, data.get(1)); }
    public int energyCost() { return (data.get(2) & 0xFFFF) | (data.get(3) & 0xFFFF) << 16; }
    public int storedEnergy() { return (data.get(4) & 0xFFFF) | (data.get(5) & 0xFFFF) << 16; }
    public int progress() { return data.get(0); }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, block); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        return index < 9 ? moveStack(player, index, 9, slots.size(), true) : moveStack(player, index, 0, 9, false);
    }
}
