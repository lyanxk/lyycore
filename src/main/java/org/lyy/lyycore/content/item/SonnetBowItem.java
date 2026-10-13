package org.lyy.lyycore.content.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.entity.SonnetArrow;
import org.lyy.lyycore.content.entity.SonnetDome;
import org.lyy.lyycore.registry.LyyEntities;

public class SonnetBowItem extends BowItem implements SpecialWeapon {
    public static final float ARROW_SPEED = 3.0F;
    public static final int NORMAL_SHOT_INTERVAL = 20;
    private static final int SKILL_INTERVAL = 100;
    public static final int CRYSTAL_SHOT_INTERVAL = 20;
    private static final String LAST_SHOT = "SonnetLastShot";
    private static final String DOME = "SonnetDome";

    public SonnetBowItem(Properties properties) { super(properties); }

    @Override public void useSkill(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.getLong("SonnetSkillUntil") > now) return;
        shoot(player, stack, true);
        data.putLong("SonnetSkillUntil", now + SKILL_INTERVAL);
        data.putLong("SonnetHoverUntil", now + 20);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new org.lyy.lyycore.network.SkillNetwork.Hover(now + 20, true));
    }

    @Override public void useUltimate(ServerPlayer player, ItemStack stack) {
        var existing = findOwnedDome(player);
        if (existing != null) {
            existing.recall(player);
            return;
        }
        if (resolveDome(stack, player.serverLevel()) != null) return;
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("SonnetUltimateUntil") > now) return;
        var dome = LyyEntities.SONNET_DOME.get().create(player.level());
        var arrow = LyyEntities.CRYSTAL_ARROW.get().create(player.level());
        if (dome == null || arrow == null) return;
        dome.prepare(player);
        arrow.setOwner(player);
        arrow.setPos(player.getEyePosition());
        arrow.launchDome(dome);
        arrow.setDeltaMovement(0, ARROW_SPEED, 0);
        player.stopUsingItem();
        player.level().addFreshEntity(dome);
        bind(stack, dome);
        player.getPersistentData().putUUID("SonnetActiveDome", dome.getUUID());
        player.level().addFreshEntity(arrow);
        player.getPersistentData().putLong("SonnetUltimateUntil", now + 310);
    }

    private static SonnetDome findOwnedDome(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.hasUUID("SonnetActiveDome")) {
            var id = data.getUUID("SonnetActiveDome");
            for (var level : player.server.getAllLevels()) {
                if (level.getEntity(id) instanceof SonnetDome dome && dome.isValid() && dome.belongsTo(player))
                    return dome;
            }
            data.remove("SonnetActiveDome");
        }
        // Recognize saved domes from before the player-side binding was introduced.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var dome = resolveDome(player.getInventory().getItem(slot), player.serverLevel());
            if (dome != null && dome.belongsTo(player)) return dome;
        }
        return null;
    }

    public static boolean isCrystal(ItemStack stack) {
        return stack.getItem() instanceof SonnetBowItem &&
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("SonnetCrystal");
    }

    public static void bind(ItemStack stack, SonnetDome dome) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(DOME, dome.getUUID()));
    }

    public static void revert(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { tag.remove(DOME); tag.remove("SonnetCrystal"); tag.remove(LAST_SHOT); });
    }

    public static SonnetDome resolveDome(ItemStack stack, ServerLevel level) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(DOME) && level.getEntity(tag.getUUID(DOME)) instanceof SonnetDome dome
                && dome.isValid() ? dome : null;
    }

    public static SonnetDome findDome(ItemStack stack, ServerLevel level) {
        SonnetDome dome = resolveDome(stack, level);
        return dome != null && dome.isActive() ? dome : null;
    }

    public static void refreshBinding(ItemStack stack, ServerLevel level) {
        if (!stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(DOME)) return;
        SonnetDome dome = resolveDome(stack, level);
        if (dome == null) revert(stack);
        else if (dome.isOpen() && !isCrystal(stack))
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("SonnetCrystal", true));
    }

    public static void startCrystalShot(ItemStack stack, Level level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(LAST_SHOT, level.getGameTime()));
    }

    public static float crystalShotAge(ItemStack stack, Level level, float partialTick) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (level == null || !tag.contains(LAST_SHOT)) return Float.POSITIVE_INFINITY;
        return Math.max(0, level.getGameTime() - tag.getLong(LAST_SHOT) + partialTick);
    }

    public static float crystalDraw(ItemStack stack, Level level, float partialTick) {
        return net.minecraft.util.Mth.clamp((crystalShotAge(stack, level, partialTick) - 2) / (CRYSTAL_SHOT_INTERVAL - 2), 0, 1);
    }

    public static int crystalDrawStage(ItemStack stack, Level level) {
        float age = crystalShotAge(stack, level, 0);
        return age < 2 ? 0 : age < 8 ? 1 : age < 14 ? 2 : 3;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (isCrystal(stack)) {
            if (level instanceof ServerLevel server) {
                SonnetDome dome = findDome(stack, server);
                if (dome == null) {
                    revert(stack);
                } else {
                    if (dome.fire(player)) {
                        startCrystalShot(stack, level);
                        player.getCooldowns().addCooldown(this, CRYSTAL_SHOT_INTERVAL);
                    }
                    return InteractionResultHolder.consume(stack);
                }
            } else {
                // Predict only the visual release; server confirmation syncs this timestamp to observers.
                if (!player.getCooldowns().isOnCooldown(this)) {
                    startCrystalShot(stack, level);
                    player.getCooldowns().addCooldown(this, CRYSTAL_SHOT_INTERVAL);
                }
                return InteractionResultHolder.consume(stack);
            }
        }
        if (player instanceof ServerPlayer server) shoot(server, stack, false);
        player.getCooldowns().addCooldown(this, NORMAL_SHOT_INTERVAL);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) { }

    private void shoot(ServerPlayer player, ItemStack stack, boolean crystallizing) {
        var level = player.serverLevel();
        // Ordinary shots retain the selected arrow's potion/glowing effects without consuming it.
        ItemStack ammo = crystallizing ? new ItemStack(Items.ARROW) : player.getProjectile(stack);
        if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);
        AbstractArrow original = ammo.is(Items.SPECTRAL_ARROW)
                ? new SpectralArrow(level, player, ammo.copyWithCount(1), null)
                : new Arrow(level, player, ammo.copyWithCount(1), null);
        AbstractArrow arrow = customArrow(original, ammo, stack);
        if (arrow instanceof SonnetArrow sonnet) sonnet.setCharged(crystallizing);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, ARROW_SPEED, 1);
        level.addFreshEntity(arrow);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1, 1.0F / (level.random.nextFloat() * 0.4F + 1.2F) + 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow original, ItemStack ammo, ItemStack weapon) {
        AbstractArrow arrow = ammo.is(Items.SPECTRAL_ARROW)
                ? LyyEntities.CRYSTAL_SPECTRAL_ARROW.get().create(original.level())
                : LyyEntities.CRYSTAL_ARROW.get().create(original.level());
        if (arrow == null) throw new IllegalStateException("Sonnet arrow creation failed");
        var data = original.saveWithoutId(new CompoundTag());
        data.remove("weapon");
        data.putByte("PierceLevel", (byte) 0);
        arrow.load(data);
        arrow.setBaseDamage(2);
        arrow.setCritArrow(false);
        arrow.clearFire();
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server) {
            refreshBinding(stack, server);
            if (stack.isEnchanted()) stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
    }

    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return isCrystal(stack) ? UseAnim.NONE : UseAnim.BOW; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) { return false; }
    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) { return false; }
    @Override public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) { return false; }
    @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) { return 0; }
    @Override public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) { return ItemEnchantments.EMPTY; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
}
