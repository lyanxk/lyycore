package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.lyy.lyycore.Config;
import org.lyy.lyycore.LyyCore;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ImaginaryDisassemblerItem extends Item {
    private static final ThreadLocal<Boolean> BREAKING_EXTRA_BLOCK = ThreadLocal.withInitial(() -> false);
    private static final TagKey<Block> VEIN_EXCLUDED = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(LyyCore.MODID, "disassembler_vein_excluded"));

    public enum Mode {
        LOW(6.0f, false),
        NORMAL(12.0f, false),
        FAST(20.0f, false),
        EXTREME(40.0f, false),
        AOE5(12.0f, true),
        VEIN(12.0f, true);

        public final float speed;
        public final boolean special;
        Mode(float speed, boolean special) { this.speed = speed; this.special = special; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public ImaginaryDisassemblerItem(Item.Properties props) {
        super(props);
    }

    // --- Tool behavior ---

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return action == ItemAbilities.PICKAXE_DIG
                || action == ItemAbilities.AXE_DIG
                || action == ItemAbilities.SHOVEL_DIG
                || action == ItemAbilities.HOE_DIG
                || action == ItemAbilities.SWORD_SWEEP;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return getMode(stack).speed;
    }

    // --- Mining logic ---

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        // Extra blocks are broken through ServerPlayerGameMode#destroyBlock below. That
        // invokes this method again, so stop the recursive AOE/vein expansion here.
        if (BREAKING_EXTRA_BLOCK.get()) return true;
        if (level.isClientSide || !(entity instanceof Player player)) return true;
        if (player.isCreative()) return true;
        if (state.getDestroySpeed(level, pos) <= 0) return true;

        int maxExtraBlocks = Math.max(0, Config.DISASSEMBLER_MAX_VEIN.get() - 1);
        int maxBreakEffects = Config.DISASSEMBLER_MAX_BREAK_EFFECTS.get();
        switch (getMode(stack)) {
            case VEIN -> veinMine(level, player, pos, state, maxExtraBlocks, maxBreakEffects);
            case AOE5 -> aoeMine(level, player, pos, Config.DISASSEMBLER_AOE_RADIUS.get(),
                    Direction.getNearest(player.getLookAngle()).getAxis(), maxExtraBlocks, maxBreakEffects);
            default -> { }
        }

        // This tool is intentionally unbreakable, so do not delegate to Item#mineBlock.
        return true;
    }

    private int aoeMine(Level level, Player player, BlockPos origin, int r, Direction.Axis axis,
                        int maxExtraBlocks, int maxBreakEffects) {
        if (maxExtraBlocks <= 0 || r <= 0) return 0;
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
                total += tryBreak(level, player, cursor.immutable(), total < maxBreakEffects - 1);
                if (total >= maxExtraBlocks) return total;
            }
        }
        return total;
    }

    private int veinMine(Level level, Player player, BlockPos origin, BlockState target,
                         int limit, int maxBreakEffects) {
        if (limit <= 0 || target.is(VEIN_EXCLUDED)) return 0;
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
                int result = tryBreak(level, player, p, broken < maxBreakEffects - 1);
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

    private int tryBreak(Level level, Player player, BlockPos pos, boolean playBreakEffect) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return 0;
        if (!isCorrectToolForDrops(player.getMainHandItem(), state)) return 0;
        if (!player.mayBuild()) return 0;
        if (!(player instanceof ServerPlayer serverPlayer)) return 0;

        BREAKING_EXTRA_BLOCK.set(true);
        try {
            if (!serverPlayer.gameMode.destroyBlock(pos)) return 0;
            if (playBreakEffect) {
                serverPlayer.serverLevel().levelEvent(null, 2001, pos,
                        net.minecraft.world.level.block.Block.getId(state));
            }
            return 1;
        } finally {
            BREAKING_EXTRA_BLOCK.remove();
        }
    }

    // --- Mode switching ---

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Mode next = getMode(stack).next();
            setMode(stack, next);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            player.displayClientMessage(Component.translatable("item.lyycore.disassembler.mode", modeName(next))
                    .withStyle(style -> style.withColor(0xF1B6D7)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static Mode getMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            int ord = customData.copyTag().getInt("Mode");
            if (ord >= 0 && ord < Mode.values().length) return Mode.values()[ord];
        }
        return Mode.NORMAL;
    }

    public static void setMode(ItemStack stack, Mode m) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putInt("Mode", m.ordinal());
            return CustomData.of(tag);
        });
    }

    // --- Tooltip ---

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        Mode m = getMode(stack);
        list.add(Component.translatable("item.lyycore.disassembler.mode",
                modeName(m).copy().withStyle(style -> style.withColor(0xF1B6D7)))
                .withStyle(style -> style.withColor(0xCBD2E2)));
        if (m.special) {
            list.add(Component.translatable("item.lyycore.disassembler.special")
                    .withStyle(style -> style.withColor(0xC9BDE8)));
        }
        list.add(Component.translatable("tooltip.lyycore.action.mode")
                .withStyle(style -> style.withColor(0xDEA6C9)));
    }

    private static Component modeName(Mode mode) {
        return Component.translatable("item.lyycore.disassembler.mode." + mode.name().toLowerCase(Locale.ROOT));
    }
}
