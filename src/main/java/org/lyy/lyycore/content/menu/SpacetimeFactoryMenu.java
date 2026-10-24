package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity.Page;
import org.lyy.lyycore.registry.LyyMenus;

/** A page is a per-viewer selection: changing it never changes any running job. */
public final class SpacetimeFactoryMenu extends MachineMenu {
    public static final int WIDTH = 256, HEIGHT = 292, MACHINE_SLOTS = SpacetimeFactoryBlockEntity.TOTAL_SLOTS;
    public static final int INVENTORY_X = 47, INVENTORY_Y = 210, HOTBAR_Y = 268;
    private static final int[] FIRST_INPUT = {0, 17, 27, 37};
    public static final int CRAFTING_X = 9, CRAFTING_Y = 39;
    private final SpacetimeFactoryBlockEntity factory;
    private final ContainerData data;
    private int pageIndex;
    public SpacetimeFactoryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (SpacetimeFactoryBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(SpacetimeFactoryBlockEntity.DATA_COUNT));
    }
    public SpacetimeFactoryMenu(int id, Inventory inventory, SpacetimeFactoryBlockEntity factory, ContainerData data) {
        super(LyyMenus.SPACETIME_FACTORY.get(), id);
        this.factory = factory; this.data = data; addDataSlots(data);
        for (var page : Page.values()) {
            var positions = positions(page);
            for (int slot = 0; slot < page.inputCount; slot++) {
                addSlot(new BlockEntityItemSlot(factory, factory.inputs(page), slot, positions[slot][0], positions[slot][1]) {
                    @Override public boolean isActive() { return page() == page; }
                    @Override public boolean mayPlace(ItemStack stack) { return isActive(); }
                    @Override public boolean mayPickup(Player player) { return isActive(); }
                    @Override public void setChanged() { super.setChanged(); factory.inventoryChanged(page); }
                });
            }
            addSlot(new SlotItemHandler(factory.outputHandler(page), 0, positions[page.inputCount][0], positions[page.inputCount][1]) {
                @Override public boolean isActive() { return page() == page; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return isActive(); }
                @Override public int getMaxStackSize() { return 64; }
                @Override public int getMaxStackSize(ItemStack stack) { return stack.getMaxStackSize(); }
                @Override public void onTake(Player player, ItemStack stack) {
                    factory.awardCraftedResult(page, player, stack); super.onTake(player, stack);
                }
            });
        }
        addPlayerSlots(inventory, INVENTORY_X, INVENTORY_Y);
    }
    public SpacetimeFactoryBlockEntity factory() { return factory; }
    public Page page() { return Page.values()[pageIndex]; }
    public int pageIndex() { return pageIndex; }
    public void selectPage(int index) { if (index >= 0 && index < Page.values().length) pageIndex = index; }
    public static int firstInputSlot(Page page) { return FIRST_INPUT[page.ordinal()]; }
    public static int[] inputSlots(Page page) { return java.util.stream.IntStream.range(firstInputSlot(page), outputSlot(page)).toArray(); }
    public static int outputSlot(Page page) { return firstInputSlot(page) + page.inputCount; }
    public static int[][] positions(Page page) {
        var positions = new int[page.inputCount + 1][2];
        if (page == Page.SPACETIME) {
            for (int slot = 0; slot < 16; slot++) {
                double angle = (slot % 8) * Math.PI / 4; int radius = slot < 8 ? 76 : 43;
                positions[slot] = new int[]{119 + (int) Math.round(radius * Math.sin(angle)), 101 - (int) Math.round(radius * Math.cos(angle))};
            }
            positions[16] = new int[]{119, 101};
        } else if (page == Page.CONDENSING || page == Page.CRAFTING) {
            for (int slot = 0; slot < 9; slot++) {
                int[] original = ImaginaryCraftingMenu.POSITIONS[slot];
                positions[slot] = new int[]{original[0] + CRAFTING_X, original[1] + CRAFTING_Y};
            }
            positions[9] = new int[]{189, 101};
        } else {
            // The alloying JEI diagram, translated by (40, 65).
            positions[0] = new int[]{56, 91}; positions[1] = new int[]{56, 115};
            positions[2] = new int[]{102, 103}; positions[3] = new int[]{176, 103};
        }
        return positions;
    }
    private int value(int index) { return data.get(index * 2) & 65535 | (data.get(index * 2 + 1) & 65535) << 16; }
    private int pageValue(Page page, int field) { return value(2 + page.ordinal() * 6 + field); }
    public int storedEnergy() { return value(0); }
    public int capacity() { return value(1); }
    public int progress(Page page) { return pageValue(page, 0); }
    public int duration(Page page) { return Math.max(1, pageValue(page, 1)); }
    public int energyCost(Page page) { return pageValue(page, 2); }
    public int outputCount(Page page) { return pageValue(page, 3); }
    public boolean energyPerTick(Page page) { return pageValue(page, 4) != 0; }
    public boolean running(Page page) { return pageValue(page, 5) != 0; }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !factory.isRemoved() && player.level() == factory.getLevel()
                && player.distanceToSqr(factory.getBlockPos().getCenter()) < 64;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || player.isSpectator()) return false;
        if (button >= 0 && button < Page.values().length) { selectPage(button); return true; }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).isActive()) return ItemStack.EMPTY;
        return index < MACHINE_SLOTS ? moveStack(player, index, MACHINE_SLOTS, slots.size(), true)
                : moveStack(player, index, firstInputSlot(page()), outputSlot(page()), false);
    }
}
