package org.lyy.lyycore.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.EnderCompanions;
import org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity.Mode;
import org.lyy.lyycore.content.item.DragonMightItem;
import org.lyy.lyycore.registry.LyyMenus;

/** A remote controller exposes only mode buttons, never the nest's inventory. */
public final class DragonControlMenu extends AbstractContainerMenu {
    private final ContainerData data;
    public DragonControlMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, new SimpleContainerData(1)); }
    private DragonControlMenu(int id, ContainerData data) { super(LyyMenus.DRAGON_CONTROL.get(), id); this.data = data; addDataSlots(data); }
    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DragonControlMenu(id, new ContainerData() {
            public int get(int index) { var nest = EnderCompanions.boundNest(player); return nest == null ? -1 : nest.mode().ordinal(); }
            public void set(int index, int value) { }
            public int getCount() { return 1; }
        }), Component.translatable("gui.lyycore.dragon_control")));
    }
    public int mode() { return data.get(0); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer server) || !stillValid(player) || button < 0 || button >= Mode.values().length) return false;
        var nest = EnderCompanions.boundNest(server);
        if (nest == null) return false;
        nest.select(server, Mode.values()[button]); return true;
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && (player.getMainHandItem().getItem() instanceof DragonMightItem || player.getOffhandItem().getItem() instanceof DragonMightItem); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
