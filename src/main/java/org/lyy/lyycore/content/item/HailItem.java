package org.lyy.lyycore.content.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
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
            if (level.getBlockState(pos).is(Blocks.WATER) && CommonHooks.canEntityDestroy(level, pos, player))
                placeIce(level, player, pos);
        }
    }
    /** Defer physics/client updates until placement listeners have seen the actual new state. */
    private static void placeIce(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (level.captureBlockSnapshots) {
            // An outer placement transaction owns its snapshots, event and rollback.
            level.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        int firstSnapshot = level.capturedBlockSnapshots.size();
        List<BlockSnapshot> snapshots;
        level.captureBlockSnapshots = true;
        try {
            level.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
        } finally {
            level.captureBlockSnapshots = false;
            snapshots = List.copyOf(level.capturedBlockSnapshots.subList(firstSnapshot, level.capturedBlockSnapshots.size()));
            level.capturedBlockSnapshots.subList(firstSnapshot, level.capturedBlockSnapshots.size()).clear();
        }
        if (snapshots.isEmpty()) return;
        boolean accepted = false;
        try {
            accepted = !(snapshots.size() == 1
                    ? EventHooks.onBlockPlace(player, snapshots.getFirst(), Direction.UP)
                    : EventHooks.onMultiBlockPlace(player, snapshots, Direction.UP));
        } finally {
            if (!accepted) {
                boolean restoring = level.restoringBlockSnapshots;
                level.restoringBlockSnapshots = true;
                try {
                    for (int i = snapshots.size() - 1; i >= 0; i--)
                        snapshots.get(i).restore(snapshots.get(i).getFlags() | Block.UPDATE_CLIENTS);
                } finally { level.restoringBlockSnapshots = restoring; }
            }
        }
        if (accepted) for (var snapshot : snapshots) {
            var current = level.getBlockState(snapshot.getPos());
            current.onPlace(level, snapshot.getPos(), snapshot.getState(), false);
            level.markAndNotifyBlock(snapshot.getPos(), level.getChunkAt(snapshot.getPos()),
                    snapshot.getState(), current, snapshot.getFlags(), 512);
        }
    }
}
