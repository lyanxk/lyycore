package org.lyy.lyycore.content.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.lyy.lyycore.registry.LyyItems;

@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeAccessoryItem extends Item implements Accessory {
    private static final ResourceLocation SET = ResourceLocation.parse("lyycore:spacetime_blessing");
    private final Kind kind;

    public SpacetimeAccessoryItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.kind = kind;
    }

    @Override public Kind kind() { return kind; }
    @Override public int rollTraitCount(RandomSource random) { return 4; }
    @Override public int attributePoints(int traitCount) { return traitCount == 4 ? 20 : 0; }
    @Override public List<Bonus> bonuses() { return List.of(); }
    @Override public ResourceLocation setId() { return SET; }
    @Override public Item reforgeMaterial() { return LyyItems.SPACETIME_ALLOY_PLATE.get(); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        lines.add(Component.translatable("tooltip.lyycore.spacetime_blessing.set").withStyle(ChatFormatting.LIGHT_PURPLE));
        AccessoryItem.appendTraitTooltip(stack, lines, Component.translatable("tooltip.lyycore.spacetime_blessing.unrolled"));
    }

    // Cancel before our ordinary death cleanup and encounter counters. Vanilla totems run earlier.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var equipment = AccessoryEquipment.get(player);
        if (!equipment.getStackInSlot(Kind.NECKLACE.ordinal()).is(LyyItems.SPACETIME_BLESSING_NECKLACE.get())
                || !equipment.getStackInSlot(Kind.NOTEBOOK.ordinal()).is(LyyItems.SPACETIME_BLESSING_NOTEBOOK.get())) return;
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
    }
}
