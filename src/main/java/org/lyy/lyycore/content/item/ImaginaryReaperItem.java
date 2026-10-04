package org.lyy.lyycore.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.lyy.lyycore.content.ToolMining;
import org.lyy.lyycore.content.menu.ImaginaryReaperMenu;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ImaginaryReaperItem extends Item {
    public static final int MIN_EFFICIENCY = 8, MAX_EFFICIENCY = 128;
    public static final int MIN_DAMAGE = 2, MAX_DAMAGE = 42;
    private static final String SETTINGS = "Reaper";

    public ImaginaryReaperItem(Properties properties) { super(properties); }

    private static CompoundTag settings(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(SETTINGS);
    }

    private static void update(ItemStack stack, Consumer<CompoundTag> change) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag root = data.copyTag();
            CompoundTag settings = root.getCompound(SETTINGS);
            change.accept(settings);
            root.put(SETTINGS, settings);
            return CustomData.of(root);
        });
    }

    public static int efficiency(ItemStack stack) {
        var tag = settings(stack);
        return tag.contains("Efficiency") ? Mth.clamp(tag.getInt("Efficiency"), MIN_EFFICIENCY, MAX_EFFICIENCY) : MAX_EFFICIENCY;
    }

    public static int damage(ItemStack stack) {
        var tag = settings(stack);
        return tag.contains("Damage") ? Mth.clamp(tag.getInt("Damage"), MIN_DAMAGE, MAX_DAMAGE) : MAX_DAMAGE;
    }

    public static boolean silkTouch(ItemStack stack) { return settings(stack).getBoolean("SilkTouch"); }

    public static ToolMining.Pattern mode(ItemStack stack) {
        int value = settings(stack).getInt("Mode");
        return value >= 0 && value < ToolMining.Pattern.values().length ? ToolMining.Pattern.values()[value] : ToolMining.Pattern.SINGLE;
    }

    public static Component modeName(ItemStack stack) {
        return Component.translatable("item.lyycore.imaginary_reaper.mode." + mode(stack).name().toLowerCase(Locale.ROOT));
    }

    public static void setEfficiency(ItemStack stack, int value) {
        update(stack, tag -> tag.putInt("Efficiency", Mth.clamp(value, MIN_EFFICIENCY, MAX_EFFICIENCY)));
    }

    public static void setAttackDamage(ItemStack stack, int value) {
        update(stack, tag -> tag.putInt("Damage", Mth.clamp(value, MIN_DAMAGE, MAX_DAMAGE)));
    }

    public static void setSilkTouch(ItemStack stack, boolean enabled) { update(stack, tag -> tag.putBoolean("SilkTouch", enabled)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) {
                server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new ImaginaryReaperMenu(id, inventory, hand),
                        Component.translatable("item.lyycore.imaginary_reaper")), buffer -> buffer.writeEnum(hand));
            } else {
                int next = (mode(stack).ordinal() + 1) % ToolMining.Pattern.values().length;
                update(stack, tag -> tag.putInt("Mode", next));
                player.displayClientMessage(Component.translatable("item.lyycore.disassembler.mode", modeName(stack))
                        .withStyle(style -> style.withColor(0xCC88FF)), true);
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return action == ItemAbilities.PICKAXE_DIG || action == ItemAbilities.AXE_DIG
                || action == ItemAbilities.SHOVEL_DIG || action == ItemAbilities.HOE_DIG;
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return true; }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return efficiency(stack); }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        ToolMining.mine(mode(stack), level, state, pos, entity);
        return true;
    }

    @Override public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage(stack) - 1,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build();
    }

    // Only the selected built-in mining enchantment participates in loot and combat hooks.
    @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return silkTouch(stack) ? (enchantment.is(Enchantments.SILK_TOUCH) ? 1 : 0)
                : (enchantment.is(Enchantments.FORTUNE) ? 5 : 0);
    }

    @Override public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        boolean silk = silkTouch(stack);
        enchantments.set(lookup.getOrThrow(silk ? Enchantments.SILK_TOUCH : Enchantments.FORTUNE), silk ? 1 : 5);
        return enchantments.toImmutable();
    }

    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) { return false; }
    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) { return false; }
    @Override public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) { return false; }
    @Override public boolean isFoil(ItemStack stack) { return false; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.lyycore.disassembler.mode", modeName(stack)).withColor(0xCC88FF));
        lines.add(Component.translatable("tooltip.lyycore.imaginary_reaper.settings", efficiency(stack), damage(stack)).withColor(0xCBD2E2));
        lines.add((silkTouch(stack) ? Component.translatable("enchantment.minecraft.silk_touch")
                : Component.translatable("enchantment.minecraft.fortune").append(" V")).withColor(0xFFB3D9));
        lines.add(Component.translatable("tooltip.lyycore.imaginary_reaper.chain").withColor(0xCBD2E2));
        lines.add(Component.translatable("tooltip.lyycore.imaginary_reaper.controls").withColor(0xDEA6C9));
    }
}
