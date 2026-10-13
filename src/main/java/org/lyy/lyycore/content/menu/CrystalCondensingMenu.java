package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;
import org.lyy.lyycore.registry.LyyRecipes;
import net.minecraft.world.item.ItemStack;

public final class CrystalCondensingMenu extends CondensingFrameMenu {
    private final CrystalCondensingFrameBlockEntity be;
    private final ContainerData data;
    public CrystalCondensingMenu(int id, Inventory inventory, CrystalCondensingFrameBlockEntity be,
                                IItemHandlerModifiable output, ContainerData data) {
        super(LyyMenus.CRYSTAL_CONDENSING.get(), id, inventory, be, output, data, 5);
        this.be = be;
        this.data = data;
    }
    public CrystalCondensingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (CrystalCondensingFrameBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new ItemStackHandler(1), new SimpleContainerData(5));
    }
    public boolean isProductionEnabled() { return data.get(4) == 0; }
    public ItemStack recipeResult() {
        return be.getLevel().getRecipeManager().getAllRecipesFor(LyyRecipes.CRYSTAL_CONDENSING.get()).stream()
                .min(java.util.Comparator.comparing(r -> r.id().toString()))
                .map(r -> r.value().result()).orElse(ItemStack.EMPTY);
    }
    @Override public boolean clickMenuButton(Player player, int index) {
        if (player.level().isClientSide || !stillValid(player) || index < 0 || index > 1) return false;
        be.setProductionPaused(index == 0);
        broadcastChanges();
        return true;
    }
}
