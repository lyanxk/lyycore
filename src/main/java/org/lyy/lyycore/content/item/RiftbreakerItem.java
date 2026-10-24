package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.ToolMining;
import org.lyy.lyycore.content.menu.RiftbreakerMenu;
import org.lyy.lyycore.network.ReaperLightningPayload;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@EventBusSubscriber(modid = "lyycore")
public final class RiftbreakerItem extends Item implements SpecialWeapon {
    public static final int MIN_EFFICIENCY = 8, MAX_EFFICIENCY = 128;
    private static final int ATTACK_RADIUS = 15;
    private static final String SETTINGS = "Riftbreaker", READY = "lyycore:riftbreaker_ready";

    public RiftbreakerItem(Properties properties) { super(properties); }

    private static CompoundTag settings(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(SETTINGS);
    }

    private static void update(ItemStack stack, Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var settings = root.getCompound(SETTINGS);
            change.accept(settings);
            root.put(SETTINGS, settings);
        });
    }

    public static int efficiency(ItemStack stack) {
        var tag = settings(stack);
        return tag.contains("Efficiency") ? Mth.clamp(tag.getInt("Efficiency"), MIN_EFFICIENCY, MAX_EFFICIENCY) : MAX_EFFICIENCY;
    }
    public static boolean silkTouch(ItemStack stack) { return settings(stack).getBoolean("SilkTouch"); }
    public static int radius(ItemStack stack) {
        var tag = settings(stack);
        return tag.contains("Radius") ? Mth.clamp(tag.getInt("Radius"), 1, 5) : 1;
    }
    public static ToolMining.Pattern mode(ItemStack stack) {
        int mode = settings(stack).getInt("Mode");
        return mode >= 0 && mode < ToolMining.Pattern.values().length ? ToolMining.Pattern.values()[mode] : ToolMining.Pattern.SINGLE;
    }
    public static Component modeName(ItemStack stack) { return modeName(mode(stack)); }
    public static Component modeName(ToolMining.Pattern pattern) {
        return Component.translatable("item.lyycore.imaginary_reaper.mode." + pattern.name().toLowerCase(Locale.ROOT));
    }
    public static void setEfficiency(ItemStack stack, int value) {
        update(stack, tag -> tag.putInt("Efficiency", Mth.clamp(value, MIN_EFFICIENCY, MAX_EFFICIENCY)));
    }
    public static void setSilkTouch(ItemStack stack, boolean value) { update(stack, tag -> tag.putBoolean("SilkTouch", value)); }
    public static void setRadius(ItemStack stack, int radius) { update(stack, tag -> tag.putInt("Radius", Mth.clamp(radius, 1, 5))); }
    public static void setMode(ItemStack stack, ToolMining.Pattern pattern) { update(stack, tag -> tag.putInt("Mode", pattern.ordinal())); }

    @Override public void useSkill(ServerPlayer player, ItemStack stack) { }
    @Override public void useUltimate(ServerPlayer player, ItemStack stack) { }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator()) return InteractionResultHolder.pass(stack);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server)
                server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new RiftbreakerMenu(id, inventory, hand),
                        stack.getHoverName()), buffer -> buffer.writeEnum(hand));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.getGameTime() < player.getPersistentData().getLong(READY)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer server) {
            player.getPersistentData().putLong(READY, level.getGameTime() + 20);
            double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 40;
            var start = player.getEyePosition().add(0, -.25, 0);
            var source = SpecialDamage.source(player, player, SpecialDamage.Element.LIGHTNING);
            for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ATTACK_RADIUS),
                    target -> target != player && target.isAlive() && target.distanceToSqr(player) <= ATTACK_RADIUS * ATTACK_RADIUS)) {
                float enchantedBase = EnchantmentHelper.modifyDamage(server.serverLevel(), stack, target, source, (float) base);
                if (SpecialDamage.combinedHitFromBase(player, player, target, SpecialDamage.Element.LIGHTNING, enchantedBase, 100)) {
                    float knockback = EnchantmentHelper.modifyKnockback(server.serverLevel(), stack, target, source, 0);
                    if (knockback > 0) target.knockback(knockback * .5, player.getX() - target.getX(), player.getZ() - target.getZ());
                    EnchantmentHelper.doPostAttackEffectsWithItemSource(server.serverLevel(), target, source, stack);
                }
                PacketDistributor.sendToPlayersNear(server.serverLevel(), null, player.getX(), player.getY(), player.getZ(), 64,
                        new ReaperLightningPayload(List.of(start, target.getBoundingBox().getCenter())));
            }
            level.playSound(null, player.blockPosition(), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("lyycore:lightning_zap")),
                    SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return useOn(context); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightEntity(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!(player.getItemInHand(event.getHand()).getItem() instanceof RiftbreakerItem item)) return;
        event.setCancellationResult(item.use(player.level(), player, event.getHand()).getResult());
        event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        var player = event.getEntity();
        if (!(player.getItemInHand(event.getHand()).getItem() instanceof RiftbreakerItem item)) return;
        event.setCancellationResult(item.use(player.level(), player, event.getHand()).getResult());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void leftBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof RiftbreakerItem)
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        var pos = event.getPos();
        var state = player.level().getBlockState(pos);
        if (state.getDestroySpeed(player.level(), pos) >= 0) return;
        var stack = player.getMainHandItem();
        if (player.canInteractWithBlock(pos, 1) && ToolMining.breakSpacetimeOrigin(player, pos))
            ToolMining.mineSpacetime(mode(stack), player.level(), state, pos, player, radius(stack));
        // Leave the client initiation packet intact; the normal block update confirms removal.
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void unbreakableDrops(BlockDropsEvent event) {
        if (!(event.getTool().getItem() instanceof RiftbreakerItem)
                || event.getState().getDestroySpeed(event.getLevel(), event.getPos()) >= 0) return;
        event.getDrops().clear();
        event.setDroppedExperience(0);
        var item = event.getState().getBlock().asItem();
        if (item != net.minecraft.world.item.Items.AIR && event.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            var pos = event.getPos();
            var drop = new ItemEntity(event.getLevel(), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, new ItemStack(item));
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }

    @Override public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return action == ItemAbilities.PICKAXE_DIG || action == ItemAbilities.AXE_DIG
                || action == ItemAbilities.SHOVEL_DIG || action == ItemAbilities.HOE_DIG
                || action == ItemAbilities.SWORD_SWEEP;
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return true; }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return efficiency(stack); }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        ToolMining.mineSpacetime(mode(stack), level, state, pos, entity, radius(stack));
        return true;
    }

    @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SILK_TOUCH)) return silkTouch(stack) ? 1 : 0;
        if (enchantment.is(Enchantments.FORTUNE)) return silkTouch(stack) ? 0 : 5;
        return stack.getTagEnchantments().getLevel(enchantment);
    }
    @Override public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        var enchantments = new ItemEnchantments.Mutable(stack.getTagEnchantments());
        boolean silk = silkTouch(stack);
        enchantments.set(lookup.getOrThrow(Enchantments.SILK_TOUCH), silk ? 1 : 0);
        enchantments.set(lookup.getOrThrow(Enchantments.FORTUNE), silk ? 0 : 5);
        return enchantments.toImmutable();
    }
    @Override public boolean isEnchantable(ItemStack stack) { return true; }
    @Override public int getEnchantmentValue(ItemStack stack) { return Tiers.NETHERITE.getEnchantmentValue(); }
    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        // Sword enchantments coexist with the mining modes; pickaxe tags must not enable tool enchantments.
        return enchantment.value().isSupportedItem(Items.NETHERITE_SWORD.getDefaultInstance());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.lyycore.disassembler.mode", modeName(stack)).withColor(0x9CD9F3));
        int side = radius(stack) * 2 + 1;
        lines.add(Component.translatable("tooltip.lyycore.riftbreaker.settings", efficiency(stack), side, side).withColor(0xCBD2E2));
        lines.add((silkTouch(stack) ? Component.translatable("enchantment.minecraft.silk_touch")
                : Component.translatable("enchantment.minecraft.fortune").append(" V")).withColor(0x9CD9F3));
        lines.add(Component.translatable("tooltip.lyycore.riftbreaker.lightning").withColor(0xCBD2E2));
        lines.add(Component.translatable("tooltip.lyycore.riftbreaker.controls").withColor(0x9CD9F3));
    }
}
