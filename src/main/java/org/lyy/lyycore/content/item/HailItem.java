package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.CommonHooks;
import org.lyy.lyycore.content.*;
import org.lyy.lyycore.content.entity.SonnetDome;
import org.lyy.lyycore.content.skills.StyleSystem;
import org.lyy.lyycore.registry.LyyEntities;

public final class HailItem extends Item {
    public HailItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer server) {
            player.getCooldowns().addCooldown(this, 20);
            // Damage is immediate. A single cosmetic crystal per target avoids spawning twenty entities per hit burst.
            for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                    entity -> entity.distanceToSqr(player) <= 100 && SonnetDome.isEnemy(entity, player))) {
                var crystal = LyyEntities.HAIL_CRYSTAL.get().create(level);
                if (crystal != null) { crystal.prepare(player.getEyePosition(), target.getBoundingBox().getCenter()); level.addFreshEntity(crystal); }
                SpecialDamage.burst(player, player, target, SpecialDamage.Element.ICE, 20, 20);
            }
            freezeWater(server);
            if (StyleSystem.current(player) == StyleSystem.Style.MOBILITY) {
                var flower = LyyEntities.HAIL_FLOWER.get().create(level);
                if (flower != null) { flower.prepare(player); level.addFreshEntity(flower); }
                PlayerMovement.dash(server, player.getLookAngle(), 5);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1, .7F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    private static void freezeWater(ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.getX(), player.getY() - .01, player.getZ());
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (x * x + z * z > 16) continue;
            BlockPos pos = center.offset(x, 0, z);
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && CommonHooks.canEntityDestroy(level, pos, player)) {
                var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos);
                if (!net.neoforged.neoforge.event.EventHooks.onBlockPlace(player, snapshot, net.minecraft.core.Direction.UP))
                    level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            }
        }
    }
}
