package org.lyy.lyycore.content.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lyy.lyycore.content.menu.CelestialOrderMenu;

@EventBusSubscriber(modid = "lyycore")
public final class CelestialOrderItem extends Item {
    private static final String TICKS = "CelestialRandomTicks";
    public CelestialOrderItem(Properties properties) { super(properties); }

    public static int iterations(ItemStack stack) {
        int count = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TICKS);
        return count == 10 || count == 100 ? count : 1;
    }
    public static void iterations(ItemStack stack, int count) {
        if (count == 1 || count == 10 || count == 100)
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TICKS, count));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer server)
            server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new CelestialOrderMenu(id, inventory, hand),
                    stack.getHoverName()), buffer -> buffer.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return useOn(context); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isSpectator()) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return use(context.getLevel(), player, context.getHand()).getResult();
        if (context.getLevel() instanceof ServerLevel level) {
            var pos = context.getClickedPos();
            for (int i = 0; i < iterations(context.getItemInHand()); i++) {
                var state = level.getBlockState(pos);
                if (!state.isRandomlyTicking()) break;
                state.randomTick(level, pos, level.random);
            }
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 8, .3, .2, .3, .01);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .6F, 1.3F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown() || player.isSpectator() || !target.isAlive()
                || !(target instanceof AgeableMob parent) || target instanceof Enemy) return InteractionResult.PASS;
        if (target.level() instanceof ServerLevel level) {
            if (parent.isBaby()) parent.setAge(0);
            else {
                // Use species-specific offspring creation for variants, ownership and villagers.
                var child = parent.getBreedOffspring(level, parent);
                if (child == null && parent.getType().create(level) instanceof AgeableMob fallback) child = fallback;
                if (child == null) return InteractionResult.FAIL;
                child.setBaby(true);
                child.moveTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), 0);
                level.addFreshEntityWithPassengers(child);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 12, .4, .4, .4, 0);
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .6F, 1.1F);
        }
        return InteractionResult.sidedSuccess(target.level().isClientSide);
    }

    // Intercept before vanilla feeding, trading or opening a mounted animal's inventory.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        var result = grow(event, event.getTarget());
        if (result != InteractionResult.PASS) { event.setCancellationResult(result); event.setCanceled(true); }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        var result = grow(event, event.getTarget());
        if (result != InteractionResult.PASS) { event.setCancellationResult(result); event.setCanceled(true); }
    }
    private static InteractionResult grow(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        if (!(event.getItemStack().getItem() instanceof CelestialOrderItem item) || !(target instanceof LivingEntity living))
            return InteractionResult.PASS;
        return item.interactLivingEntity(event.getItemStack(), event.getEntity(), living, event.getHand());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        lines.add(Component.translatable("tooltip.lyycore.celestial_order", iterations(stack)).withColor(0x9CD9F3));
    }
}
