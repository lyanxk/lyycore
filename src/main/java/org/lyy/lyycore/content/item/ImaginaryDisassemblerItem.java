package org.lyy.lyycore.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImaginaryDisassemblerItem extends Item {
    public static final int MAX_VEIN = 16;
    public static final int AOE_RADIUS = 1;

    public enum Mode {
        LOW(6.0f, false),
        NORMAL(12.0f, false),
        FAST(20.0f, false),
        EXTREME(40.0f, false),
        AOE3(12.0f, true),
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
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!isCorrectToolForDrops(stack, state)) return 1.0f;
        return getMode(stack).speed;
    }

    // --- Mining logic ---

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) return super.mineBlock(stack, level, state, pos, entity);
        if (state.getDestroySpeed(level, pos) <= 0) return super.mineBlock(stack, level, state, pos, entity);

        Mode mode = getMode(stack);
        int mined = switch (mode) {
            case VEIN -> veinMine(level, player, pos, state, MAX_VEIN);
            case AOE3 -> aoeMine(level, player, pos, AOE_RADIUS);
            default -> 1;
        };

        if (mined > 0) {
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.3f, 1.0f);
        }
        return super.mineBlock(stack, level, state, pos, entity);
    }

    private int aoeMine(Level level, Player player, BlockPos origin, int r) {
        int total = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                cursor.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                total += tryBreak(level, player, cursor.immutable());
                if (total >= MAX_VEIN) return total;
            }
        }
        return total;
    }

    private int veinMine(Level level, Player player, BlockPos origin, BlockState target, int limit) {
        int broken = 0;
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(origin);

        while (!q.isEmpty() && broken < limit) {
            BlockPos p = q.poll();
            if (!visited.add(p)) continue;
            BlockState st = level.getBlockState(p);
            if (!st.is(target.getBlock())) continue;

            if (!p.equals(origin)) broken += tryBreak(level, player, p);
            if (broken >= limit) break;

            for (var dir : net.minecraft.core.Direction.values()) {
                BlockPos np = p.relative(dir);
                if (!visited.contains(np)) q.add(np);
            }
        }
        return broken;
    }

    private int tryBreak(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return 0;
        if (!isCorrectToolForDrops(player.getMainHandItem(), state)) return 0;
        if (!player.mayBuild()) return 0;
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return 0;

        var be = level.getBlockEntity(pos);
        ItemStack tool = player.getMainHandItem();
        net.minecraft.world.level.block.Block.dropResources(state, serverLevel, pos, be, player, tool);
        state.onDestroyedByPlayer(level, pos, player, true, level.getFluidState(pos));
        level.removeBlock(pos, false);
        return 1;
    }

    // --- Mode switching ---

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Mode next = getMode(stack).next();
            setMode(stack, next);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            player.displayClientMessage(Component.translatable("item.lyycore.disassembler.mode", next.name()), true);
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
        list.add(Component.literal("Mode: ").append(Component.literal(m.name()).withStyle(ChatFormatting.AQUA)));
        if (m.special) {
            list.add(Component.translatable("item.lyycore.disassembler.special").withStyle(ChatFormatting.GOLD));
        }
    }
}
