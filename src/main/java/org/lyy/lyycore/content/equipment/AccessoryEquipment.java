package org.lyy.lyycore.content.equipment;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lyy.lyycore.content.PlayerAttributes;
import org.lyy.lyycore.content.PlayerAttributes.Stat;

/** Two personal slots, persisted independently from the death-drop inventory. No ticking is needed. */
@EventBusSubscriber(modid = "lyycore")
public final class AccessoryEquipment extends ItemStackHandler {
    private static final String KEY = "lyycore:accessories";
    private static final ResourceLocation BONUS = ResourceLocation.parse("lyycore:accessories");
    private static final Map<ServerPlayer, AccessoryEquipment> INVENTORIES = new HashMap<>();
    private final ServerPlayer owner;
    private ItemStack[] committedItems;

    private AccessoryEquipment(ServerPlayer owner) {
        super(2); this.owner = owner;
        deserializeNBT(owner.registryAccess(), owner.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY));
    }
    public static AccessoryEquipment get(ServerPlayer player) { return INVENTORIES.computeIfAbsent(player, AccessoryEquipment::new); }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof Accessory item && item.kind().ordinal() == slot;
    }
    @Override protected void onContentsChanged(int slot) { changed(); }
    public void changed() { commit(false); }

    private void commit(boolean restoreAttributes) {
        boolean changed = committedItems == null || !ItemStack.matches(committedItems[0], getStackInSlot(0))
                || !ItemStack.matches(committedItems[1], getStackInSlot(1));
        if (changed) {
            for (int i = 0; i < 2; i++) AccessoryData.initialize(getStackInSlot(i), owner.getRandom());
            var persisted = owner.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            persisted.put(KEY, serializeNBT(owner.registryAccess()));
            owner.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            // Both handler callbacks and menu in-place mutations arrive here. Snapshot the
            // committed contents so a second notification cannot repeat the same transaction.
            committedItems = new ItemStack[]{getStackInSlot(0).copy(), getStackInSlot(1).copy()};
        }
        // Entity lifecycle restoration is independent of whether the inventory changed.
        if (changed || restoreAttributes) updateAttributes();
    }
    private void updateAttributes() {
        double[] totals = new double[Stat.values().length];
        boolean set = getStackInSlot(0).getItem() instanceof Accessory a && getStackInSlot(1).getItem() instanceof Accessory b
                && a.setId() != null && a.setId().equals(b.setId());
        for (int slot = 0; slot < 2; slot++) {
            var stack = getStackInSlot(slot);
            if (!(stack.getItem() instanceof Accessory item)) continue;
            for (var bonus : item.bonuses()) totals[bonus.stat().ordinal()] += bonus.amount() * (set ? item.setBonusMultiplier() : 1);
            var data = AccessoryData.read(stack);
            for (var stat : data.traits()) totals[stat.ordinal()] += data.allocated(stat) * stat.perPoint;
        }
        double reach = 0;
        if (set && getStackInSlot(0).getItem() instanceof Accessory accessory) {
            for (var bonus : accessory.setBonuses()) totals[bonus.stat().ordinal()] += bonus.amount();
            reach = accessory.setReachBonus();
        }
        PlayerAttributes.setPersistentBonus(owner, net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE, BONUS, reach);
        PlayerAttributes.setPersistentBonus(owner, net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE, BONUS, reach);
        for (var stat : Stat.values()) PlayerAttributes.setPersistentBonus(owner, stat.attribute, BONUS, totals[stat.ordinal()]);
    }
    public boolean allocate(int slot, Stat stat, boolean add, int amount) {
        if (!AccessoryData.allocate(getStackInSlot(slot), stat, add, amount)) return false;
        changed(); return true;
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) get(player).commit(true); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) get(player).commit(true); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { INVENTORIES.remove(event.getOriginal()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { INVENTORIES.remove(event.getEntity()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { INVENTORIES.clear(); }
}
