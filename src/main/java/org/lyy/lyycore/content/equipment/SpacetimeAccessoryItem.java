package org.lyy.lyycore.content.equipment;

import java.util.List;
import java.util.EnumMap;
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
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.lyy.lyycore.registry.LyyItems;
import org.lyy.lyycore.content.PlayerAttributes.Stat;

@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeAccessoryItem extends Item implements Accessory {
    private static final ResourceLocation SET = ResourceLocation.parse("lyycore:spacetime_blessing");
    private static final List<Bonus> BONUSES = combinedBonuses();
    private final Kind kind;

    public SpacetimeAccessoryItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.kind = kind;
    }

    @Override public Kind kind() { return kind; }
    @Override public int rollTraitCount(RandomSource random) { return 4; }
    @Override public int attributePoints(int traitCount) { return traitCount == 4 ? 20 : 0; }
    @Override public List<Bonus> bonuses() { return BONUSES; }
    @Override public ResourceLocation setId() { return SET; }
    @Override public Item reforgeMaterial() { return LyyItems.SPACETIME_ALLOY_PLATE.get(); }

    private static List<Bonus> combinedBonuses() {
        var totals = new EnumMap<Stat, Double>(Stat.class);
        for (var series : AccessoryItem.Series.values())
            for (var bonus : series.bonuses) totals.merge(bonus.stat(), bonus.amount(), Double::sum);
        return totals.entrySet().stream().map(entry -> new Bonus(entry.getKey(), entry.getValue())).toList();
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        AccessoryItem.appendBonusTooltip(bonuses(), lines);
        lines.add(Component.translatable("tooltip.lyycore.spacetime_blessing.set").withStyle(ChatFormatting.LIGHT_PURPLE));
        AccessoryItem.appendTraitTooltip(stack, lines, Component.translatable("tooltip.lyycore.spacetime_blessing.unrolled"));
    }

    public static void registerDeathGuard(FMLCommonSetupEvent event) {
        // Register after automatic subscribers to catch forced kills that uncancel death.
        // Equal-priority listeners run in registration order. Reuse the guard before die() can
        // send the death-screen packet or settle drops/statistics; already-canceled events skip it.
        event.enqueueWork(() -> NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SpacetimeAccessoryItem::preventDeath));
    }

    // Cancel early as well, before ordinary death cleanup and encounter counters.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var equipment = AccessoryEquipment.get(player);
        boolean spacetime = equipment.getStackInSlot(0).is(LyyItems.SPACETIME_BLESSING_NECKLACE.get())
                && equipment.getStackInSlot(1).is(LyyItems.SPACETIME_BLESSING_NOTEBOOK.get());
        boolean finality = equipment.getStackInSlot(0).is(org.lyy.lyycore.content.finality.FinalityContent.NECKLACE.get())
                && equipment.getStackInSlot(1).is(org.lyy.lyycore.content.finality.FinalityContent.NOTEBOOK.get());
        if (!spacetime && !finality) return;
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
    }
}
