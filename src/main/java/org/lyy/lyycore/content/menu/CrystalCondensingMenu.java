package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.blockEntities.CrystalCondensingFrameBlockEntity;
import org.lyy.lyycore.registry.LyyMenus;

public final class CrystalCondensingMenu extends CondensingFrameMenu {
    public CrystalCondensingMenu(int id, Inventory inventory, CrystalCondensingFrameBlockEntity be,
                                IItemHandlerModifiable output, ContainerData data) {
        super(LyyMenus.CRYSTAL_CONDENSING.get(), id, inventory, be, output, data, 4);
    }
    public CrystalCondensingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (CrystalCondensingFrameBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new ItemStackHandler(1), new SimpleContainerData(4));
    }
}
