package org.lyy.lyycore.content.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.entity.LiuxiKnife;
import org.lyy.lyycore.content.combat.ActionRestrictions;
import org.lyy.lyycore.registry.LyyEntities;
import org.lyy.lyycore.registry.LyySounds;

public final class LiuxiItem extends Item implements SpecialWeapon {
    public static final String THROWN = "LiuxiThrown";
    private static final String ACTIVE = "lyycore:liuxi_active";

    public LiuxiItem(Properties properties) { super(properties); }

    public static void throwKnife(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof LiuxiItem) || !player.isAlive() || player.isSpectator() || ActionRestrictions.frozen(player)) return;
        var data = player.getPersistentData();
        if (data.hasUUID(ACTIVE) && player.serverLevel().getEntity(data.getUUID(ACTIVE)) instanceof LiuxiKnife) return;
        ensureLooting(stack, player.level());
        var knife = LyyEntities.LIUXI_KNIFE.get().create(player.level());
        if (knife == null) return;
        knife.prepare(player, stack);
        if (!player.level().addFreshEntity(knife)) return;
        data.putUUID(ACTIVE, knife.getUUID());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(THROWN, knife.getUUID()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                LyySounds.LIUXI_THROW.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void ensureLooting(ItemStack stack, Level level) {
        var looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        if (EnchantmentHelper.getItemEnchantmentLevel(looting, stack) != 20) stack.enchant(looting, 20);
    }

    public static boolean isThrown(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(THROWN);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server)) return;
        ensureLooting(stack, level);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.hasUUID(THROWN) && !(server.getEntity(data.getUUID(THROWN)) instanceof LiuxiKnife))
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(THROWN));
    }

    public static void returned(Player player, LiuxiKnife knife) {
        player.getPersistentData().remove(ACTIVE);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (data.hasUUID(THROWN) && data.getUUID(THROWN).equals(knife.getUUID()))
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(THROWN));
        }
    }

    @Override public void useSkill(ServerPlayer player, ItemStack stack) { }
    @Override public void useUltimate(ServerPlayer player, ItemStack stack) { }
}
