package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import org.lyy.lyycore.content.ToolMining;

import java.util.List;
import java.util.Locale;

public class ImaginaryDisassemblerItem extends Item {
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

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        ToolMining.Pattern pattern = switch (getMode(stack)) {
            case AOE5 -> ToolMining.Pattern.AREA;
            case VEIN -> ToolMining.Pattern.VEIN;
            default -> ToolMining.Pattern.SINGLE;
        };
        ToolMining.mine(pattern, level, state, pos, entity);
        return true;
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
