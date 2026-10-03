package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.ResourceFrameKind;
import org.lyy.lyycore.content.blockEntities.ResourceGatheringFrameBlockEntity;
import org.lyy.lyycore.content.recipes.ResourceGatheringRecipe;
import org.lyy.lyycore.registry.LyyMenus;

import java.util.List;

public final class ResourceGatheringMenu extends CondensingFrameMenu {
    private final ResourceGatheringFrameBlockEntity be;
    private final ContainerData data;
    public ResourceGatheringMenu(int id, Inventory inventory, ResourceGatheringFrameBlockEntity be,
                                 IItemHandlerModifiable output, ContainerData data) {
        super(LyyMenus.RESOURCE_GATHERING.get(), id, inventory, be, output, data, 6);
        this.be = be;
        this.data = data;
    }
    public ResourceGatheringMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (ResourceGatheringFrameBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new ItemStackHandler(1), new SimpleContainerData(6));
    }
    public ResourceFrameKind kind() { return be.kind(); }
    public List<RecipeHolder<ResourceGatheringRecipe>> recipes() { return be.recipes(); }
    public int selectedIndex() { return (data.get(4) & 0xFFFF) | (data.get(5) & 0xFFFF) << 16; }
    public ItemStack selectedResult() {
        var recipes = recipes();
        int index = selectedIndex();
        return index >= 0 && index < recipes.size() ? recipes.get(index).value().result() : ItemStack.EMPTY;
    }
    @Override public boolean clickMenuButton(Player player, int index) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        boolean selected = be.selectRecipe(index);
        if (selected) broadcastChanges();
        return selected;
    }
}
