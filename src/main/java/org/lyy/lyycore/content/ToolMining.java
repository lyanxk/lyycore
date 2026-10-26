package org.lyy.lyycore.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lyy.lyycore.Config;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Shared area and vein mining, with normal server-side protection and loot handling. */
@EventBusSubscriber(modid = "lyycore")
public final class ToolMining {
    public enum Pattern { SINGLE, AREA, VEIN }
    private static final ThreadLocal<Boolean> BREAKING_EXTRA_BLOCK = ThreadLocal.withInitial(() -> false);
    private static final TagKey<Block> VEIN_EXCLUDED = TagKey.create(Registries.BLOCK,
            ResourceLocation.parse("lyycore:disassembler_vein_excluded"));
    private record MiningFace(GlobalPos origin, Direction.Axis axis) { }
    private static final Map<ServerPlayer, MiningFace> MINING_FACES = new WeakHashMap<>();

    private ToolMining() { }

    // Keep the initiating face: mineBlock runs after removal, and STOP packets need not carry that face.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rememberMiningFace(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && event.getFace() != null) {
            MINING_FACES.put(player, new MiningFace(GlobalPos.of(player.level().dimension(), event.getPos().immutable()),
                    event.getFace().getAxis()));
        } else if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            MINING_FACES.remove(player);
        }
    }

    @Nullable
    private static Direction.Axis miningAxis(ServerPlayer player, BlockPos pos) {
        var face = MINING_FACES.get(player);
        return face != null && face.origin.dimension().equals(player.level().dimension()) && face.origin.pos().equals(pos)
                ? face.axis : null;
    }

    public static void mine(Pattern pattern, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        // destroyBlock invokes the tool again for every extra block.
        if (pattern == Pattern.SINGLE || BREAKING_EXTRA_BLOCK.get() || level.isClientSide
                || !(entity instanceof ServerPlayer player) || player.isCreative()
                || state.getDestroySpeed(level, pos) <= 0) return;
        int limit = Math.max(0, Config.DISASSEMBLER_MAX_VEIN.get() - 1);
        int effects = Config.DISASSEMBLER_MAX_BREAK_EFFECTS.get();
        switch (pattern) {
            case VEIN -> veinMine(level, player, pos, state, limit, effects, false, false);
            case AREA -> aoeMine(level, player, pos, Config.DISASSEMBLER_AOE_RADIUS.get(),
                    miningAxis(player, pos), limit, effects, false);
            default -> { }
        }
    }

    /** Riftbreaker changes radius, adjacency and hardness support without changing older tools. */
    public static void mineSpacetime(Pattern pattern, Level level, BlockState state, BlockPos pos,
                                     LivingEntity entity, int radius) {
        if (BREAKING_EXTRA_BLOCK.get() || level.isClientSide || !(entity instanceof ServerPlayer player)
                || player.isCreative()) return;
        int limit = Math.max(0, Config.DISASSEMBLER_MAX_VEIN.get() - 1);
        int effects = Config.DISASSEMBLER_MAX_BREAK_EFFECTS.get();
        switch (pattern) {
            case VEIN -> veinMine(level, player, pos, state, limit, effects, true, true);
            case AREA -> aoeMine(level, player, pos, radius, miningAxis(player, pos),
                    Math.max(limit, (radius * 2 + 1) * (radius * 2 + 1) - 1), effects, true);
            default -> { }
        }
    }

    /** Break one block through the normal player path without invoking area or vein mining. */
    public static boolean breakSingleBlock(ServerPlayer player, BlockPos pos) {
        return tryBreak(player.level(), player, pos, true, true) != 0;
    }

    private static int aoeMine(Level level, ServerPlayer player, BlockPos origin, int r, @Nullable Direction.Axis axis,
                        int maxExtraBlocks, int maxBreakEffects, boolean spacetime) {
        if (axis == null || maxExtraBlocks <= 0 || r <= 0) return 0;
        int total = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                if (a == 0 && b == 0) continue;
                switch (axis) {
                    case X -> cursor.set(origin.getX(), origin.getY() + a, origin.getZ() + b);
                    case Y -> cursor.set(origin.getX() + a, origin.getY(), origin.getZ() + b);
                    case Z -> cursor.set(origin.getX() + a, origin.getY() + b, origin.getZ());
                }
                total += tryBreak(level, player, cursor.immutable(), total < maxBreakEffects - 1, spacetime);
                if (total >= maxExtraBlocks) return total;
            }
        }
        return total;
    }

    private static int veinMine(Level level, ServerPlayer player, BlockPos origin, BlockState target,
                         int limit, int maxBreakEffects, boolean facesOnly, boolean spacetime) {
        if (limit <= 0 || !spacetime && target.is(VEIN_EXCLUDED)) return 0;
        int broken = 0;
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(origin);
        visited.add(origin);

        while (!q.isEmpty() && broken < limit) {
            BlockPos p = q.poll();
            // The callback supplies the original state, so use the origin as the
            // seed without relying on whether it is still present in the world.
            if (!p.equals(origin)) {
                if (!level.hasChunkAt(p) || !level.getBlockState(p).is(target.getBlock())) continue;
                int result = tryBreak(level, player, p, broken < maxBreakEffects - 1, spacetime);
                if (result == 0) continue;
                broken += result;
            }
            if (broken >= limit) break;

            // Face, edge and corner neighbors all connect; only matching blocks
            // enter the queue, and every position is inspected at most once.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (facesOnly && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                        BlockPos neighbor = p.offset(dx, dy, dz);
                        if (visited.add(neighbor) && level.hasChunkAt(neighbor)
                                && level.getBlockState(neighbor).is(target.getBlock())) {
                            q.add(neighbor);
                        }
                    }
                }
            }
        }
        return broken;
    }

    private static int tryBreak(Level level, ServerPlayer player, BlockPos pos, boolean playBreakEffect, boolean spacetime) {
        if (!level.hasChunkAt(pos)) return 0;
        BlockState state = level.getBlockState(pos);
        boolean unbreakable = state.getDestroySpeed(level, pos) < 0;
        if (state.isAir() || unbreakable && !spacetime) return 0;
        if (!player.getMainHandItem().isCorrectToolForDrops(state)) return 0;
        if (!player.mayBuild()) return 0;
        if (!level.mayInteract(player, pos)) return 0;
        if (spacetime && !level.getWorldBorder().isWithinBounds(pos)) return 0;

        BREAKING_EXTRA_BLOCK.set(true);
        try {
            if (!player.gameMode.destroyBlock(pos) || spacetime && level.getBlockState(pos).equals(state)) return 0;
            if (playBreakEffect) {
                player.serverLevel().levelEvent(null, 2001, pos,
                        net.minecraft.world.level.block.Block.getId(state));
            }
            return 1;
        } finally {
            BREAKING_EXTRA_BLOCK.remove();
        }
    }

}
