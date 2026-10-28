package org.lyy.lyycore.content.finality;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import org.lyy.lyycore.content.SpecialDamage;
import org.lyy.lyycore.content.item.SpecialWeapon;
import org.lyy.lyycore.network.SpacetimeChainPayload;

import java.util.*;

@EventBusSubscriber(modid = "lyycore")
public final class DestinyWeaverItem extends Item implements SpecialWeapon {
    private static final Map<UUID, Set<UUID>> LINKS = new HashMap<>();
    private static final Map<UUID, PendingUltimate> ULTIMATES = new HashMap<>();
    private static final int ULTIMATE_LINK_TICKS = 4;
    private static final String ATTACK = "WeaverAttack", READY = "lyycore:weaver_ready";

    private record PendingUltimate(long strikeAt, Set<UUID> targets) {}

    public DestinyWeaverItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        var looting =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.LOOTING);
        if (EnchantmentHelper.getItemEnchantmentLevel(looting, stack) < 40)
            stack.enchant(looting, 40);
    }

    private static Set<UUID> links(Player player) {
        return LINKS.computeIfAbsent(player.getUUID(), key -> new HashSet<>());
    }

    private static void toggle(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive()) return;
        var links = links(player);
        if (!links.remove(target.getUUID())) links.add(target.getUUID());
        org.lyy.lyycore.content.SpacetimeChains.sync(player.serverLevel());
    }

    private static void clearConnections(ServerPlayer player) {
        LINKS.remove(player.getUUID());
        ULTIMATES.remove(player.getUUID());
        org.lyy.lyycore.content.SpacetimeChains.sync(player.serverLevel());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isSpectator() || !player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) clearConnections(server);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && !player.isSpectator()) {
            if (player instanceof ServerPlayer server) clearConnections(server);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        if (player instanceof ServerPlayer server && !player.isSpectator()) {
            Vec3 start = player.getEyePosition(),
                    end =
                            level.clip(
                                            new ClipContext(
                                                    start,
                                                    start.add(player.getLookAngle().scale(128)),
                                                    ClipContext.Block.COLLIDER,
                                                    ClipContext.Fluid.NONE,
                                                    player))
                                    .getLocation();
            for (var target :
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(start, end).inflate(1),
                            e -> !(e instanceof Player) && e.isAlive())) {
                var box = target.getBoundingBox();
                var expanded = box.inflate(1);
                var hit = expanded.contains(start) ? Optional.of(start) : expanded.clip(start, end);
                if (hit.isEmpty()) continue;
                var point = hit.get();
                var surface =
                        new Vec3(
                                Mth.clamp(point.x, box.minX, box.maxX),
                                Mth.clamp(point.y, box.minY, box.maxY),
                                Mth.clamp(point.z, box.minZ, box.maxZ));
                if (level.clip(
                                        new ClipContext(
                                                start,
                                                surface,
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.NONE,
                                                player))
                                .getType()
                        != HitResult.Type.MISS) continue;
                links(player).add(target.getUUID());
            }
            org.lyy.lyycore.content.SpacetimeChains.sync(server.serverLevel());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void useSkill(ServerPlayer player, ItemStack stack) {
        boolean attack =
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getBoolean(ATTACK);
        CustomData.update(
                DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(ATTACK, !attack));
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        attack
                                ? "message.lyycore.weaver.harmless"
                                : "message.lyycore.weaver.attack"),
                true);
    }

    @Override
    public void useUltimate(ServerPlayer player, ItemStack stack) {
        long now = player.serverLevel().getGameTime();
        if (now < player.getPersistentData().getLong(READY)) return;
        player.getPersistentData().putLong(READY, now + 200);
        var targets = new HashSet<UUID>();
        for (var target :
                player.serverLevel()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                player.getBoundingBox().inflate(40),
                                e ->
                                        e instanceof Enemy
                                                && e.isAlive()
                                                && e.distanceToSqr(player) <= 1600))
            targets.add(target.getUUID());
        if (targets.isEmpty()) return;
        links(player).addAll(targets);
        ULTIMATES.put(player.getUUID(), new PendingUltimate(now + ULTIMATE_LINK_TICKS, targets));
        org.lyy.lyycore.content.SpacetimeChains.sync(player.serverLevel());
    }

    private static void damage(ServerPlayer player, LivingEntity target, int base) {
        for (var element : SpecialDamage.Element.values()) {
            var source =
                    new net.minecraft.world.damagesource.DamageSource(
                            player.registryAccess()
                                    .lookupOrThrow(Registries.DAMAGE_TYPE)
                                    .getOrThrow(element.type),
                            player,
                            player) {
                        @Override
                        public boolean is(
                                net.minecraft.tags.TagKey<
                                                net.minecraft.world.damagesource.DamageType>
                                        tag) {
                            return tag.equals(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN)
                                    || super.is(tag);
                        }
                    };
            SpecialDamage.hit(target, source, SpecialDamage.scale(player, element, base));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        interactEntity(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        interactEntity(event, event.getTarget());
    }

    private static void interactEntity(PlayerInteractEvent event, Entity target) {
        if (!(event.getItemStack().getItem() instanceof DestinyWeaverItem)
                || event.getEntity().isSpectator()
                || !event.getEntity().isShiftKeyDown() && !(target instanceof LivingEntity)) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) clearConnections(player);
            else toggle(player, (LivingEntity) target);
        }
        if (event instanceof PlayerInteractEvent.EntityInteract interaction) {
            interaction.setCancellationResult(
                    InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            interaction.setCanceled(true);
        } else if (event instanceof PlayerInteractEvent.EntityInteractSpecific interaction) {
            interaction.setCancellationResult(
                    InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            interaction.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isAlive()) {
            LINKS.remove(player.getUUID());
            ULTIMATES.remove(player.getUUID());
            return;
        }
        var ids = LINKS.get(player.getUUID());
        if (ids == null) return;
        var ultimate = ULTIMATES.get(player.getUUID());
        if (ultimate != null && player.serverLevel().getGameTime() >= ultimate.strikeAt()) {
            ULTIMATES.remove(player.getUUID());
            for (var id : ultimate.targets())
                if (player.serverLevel().getEntity(id) instanceof LivingEntity target
                        && target.isAlive()) damage(player, target, 1_000_000);
        }
        var held = player.getMainHandItem();
        boolean attack =
                held.getItem() instanceof DestinyWeaverItem
                        && held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                                .copyTag()
                                .getBoolean(ATTACK)
                        && player.tickCount % 20 == 0;
        ids.removeIf(
                id -> {
                    var entity = player.serverLevel().getEntity(id);
                    if (!(entity instanceof LivingEntity target) || !target.isAlive()) return true;
                    Vec3 offset = player.position().subtract(target.position());
                    double distance = offset.length();
                    if (distance > 6) {
                        double acceleration =
                                Math.min(target.getGravity() * 4, (distance - 6) * .02);
                        target.setDeltaMovement(
                                target.getDeltaMovement()
                                        .add(offset.scale(acceleration / distance)));
                        target.hurtMarked = true;
                    }
                    // Let the ultimate's links appear before any automatic damage to its targets.
                    if (attack && (ultimate == null || !ultimate.targets().contains(id)))
                        damage(player, target, 100_000);
                    return false;
                });
    }

    public static void appendLinks(ServerLevel level, List<SpacetimeChainPayload.Link> visible) {
        for (var player : level.players()) {
            var ids = LINKS.get(player.getUUID());
            if (ids == null) continue;
            for (var id : ids)
                if (level.getEntity(id) instanceof LivingEntity target && target.isAlive())
                    visible.add(
                            new SpacetimeChainPayload.Link(
                                    player.getId(), target.getId(), 0xEBB881F0));
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LINKS.remove(event.getEntity().getUUID());
        ULTIMATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LINKS.remove(event.getEntity().getUUID());
        ULTIMATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        LINKS.clear();
        ULTIMATES.clear();
    }
}
