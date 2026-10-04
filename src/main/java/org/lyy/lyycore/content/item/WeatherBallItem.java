package org.lyy.lyycore.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class WeatherBallItem extends SnowballItem {
    public static final int WEATHER_DURATION = 20 * 60 * 60;
    private final boolean storm;

    public WeatherBallItem(Properties properties, boolean storm) {
        super(properties);
        this.storm = storm;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Other vanilla/datapack dimensions use read-only DerivedLevelData for weather.
        // Check on both sides before SnowballItem predicts or consumes the throw.
        if (!level.dimension().equals(Level.OVERWORLD)) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.lyycore.weather_ball.overworld_only"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        var result = super.use(level, player, hand);
        if (level instanceof ServerLevel server && result.getResult().consumesAction()) {
            server.setWeatherParameters(storm ? 0 : WEATHER_DURATION, storm ? WEATHER_DURATION : 0, storm, storm);
        }
        return result;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.lyycore." + (storm ? "storm_ball" : "sun_ball")).withColor(0xCBD2E2));
    }
}
