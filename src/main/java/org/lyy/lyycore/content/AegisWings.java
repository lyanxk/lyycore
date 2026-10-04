package org.lyy.lyycore.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lyy.lyycore.network.WingsNetwork;

/** Research grants flight and protection; visibility is only a cosmetic preference. */
@EventBusSubscriber(modid = "lyycore")
public final class AegisWings {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/why_cant_people_fly");
    private static final String HIDDEN = "lyycore:wings_hidden", CLIENT_UNLOCKED = "lyycore:wings_unlocked";
    private static final String SHIELD_STARTED = "lyycore:wings_shield_started";

    private AegisWings() { }

    public static boolean unlocked(LivingEntity entity) {
        return entity instanceof Player player && (player.level().isClientSide
                ? player.getPersistentData().getBoolean(CLIENT_UNLOCKED) : ResearchProgress.completed(player, RESEARCH));
    }
    public static boolean visible(Player player) {
        return !player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(HIDDEN);
    }
    public static void setVisible(Player player, boolean visible) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(HIDDEN, !visible);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static long shieldStarted(Player player) {
        return player.getPersistentData().contains(SHIELD_STARTED) ? player.getPersistentData().getLong(SHIELD_STARTED) : -100;
    }
    public static void receive(Player player, boolean unlocked, boolean visible, long shieldStarted) {
        player.getPersistentData().putBoolean(CLIENT_UNLOCKED, unlocked);
        player.getPersistentData().putLong(SHIELD_STARTED, shieldStarted);
        setVisible(player, visible);
    }
    public static boolean flightTick(LivingEntity entity, int ticks) {
        if (!entity.level().isClientSide && (ticks + 1) % 10 == 0) entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        return true;
    }
    @SubscribeEvent public static void protect(DamageReductionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !unlocked(player)
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || event.getDamage() <= 0) return;
        event.reduceBy(0.5);
        player.getPersistentData().putLong(SHIELD_STARTED, player.level().getGameTime());
        WingsNetwork.sync(player);
    }
}
