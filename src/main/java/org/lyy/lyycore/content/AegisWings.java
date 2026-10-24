package org.lyy.lyycore.content;

import org.lyy.lyycore.content.research.ResearchChangedEvent;
import org.lyy.lyycore.content.research.ResearchProgress;
import org.lyy.lyycore.content.combat.ActionRestrictions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.content.wings.WingsTier;
import org.lyy.lyycore.content.wings.WingsSettings;

/** Wing tier, protection, flight and synchronized appearance. */
@EventBusSubscriber(modid = "lyycore")
public final class AegisWings {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/why_cant_people_fly");
    public static final ResourceLocation ENHANCEMENT = ResourceLocation.parse("lyycore:research/wings_enhancement");
    public static final ResourceLocation SPEED_UP = ResourceLocation.parse("lyycore:research/speed_up");
    public static final ResourceLocation SUPERSONIC = ResourceLocation.parse("lyycore:research/supersonic");
    public static final ResourceLocation HARDENING = ResourceLocation.parse("lyycore:research/hardening");
    private static final String HIDDEN = "lyycore:wings_hidden", CLIENT_LEVEL = "lyycore:wings_client_level";
    private static final String SHIELD_STARTED = "lyycore:wings_shield_started";

    private AegisWings() { }

    public static boolean unlocked(LivingEntity entity) {
        return entity instanceof Player player && tier(player) != null;
    }
    /** Null means no wings; completing the base research grants level one. */
    public static WingsTier tier(Player player) {
        if (player.level().isClientSide) {
            return switch (player.getPersistentData().getInt(CLIENT_LEVEL)) {
                case 1 -> WingsTier.FIRST;
                case 2 -> WingsTier.SECOND;
                default -> null;
            };
        }
        if (ResearchProgress.completed(player, ENHANCEMENT)) return WingsTier.SECOND;
        return ResearchProgress.completed(player, RESEARCH) ? WingsTier.FIRST : null;
    }
    public static int level(Player player) {
        var tier = tier(player);
        return tier == null ? 0 : tier.level();
    }
    /** Highest unlocked flight upgrade: base, doubled thrust, or quadrupled thrust. */
    public static int flightUpgrade(Player player) {
        if (player.level().isClientSide) return player.getPersistentData().getInt("lyycore:flight_upgrade");
        if (ResearchProgress.completed(player, SUPERSONIC)) return 2;
        return ResearchProgress.completed(player, SPEED_UP) ? 1 : 0;
    }
    /** Validated boost input calls this once per movement tick on server and local client. */
    public static boolean boostFlight(Player player) {
        var tier = tier(player);
        if (tier == null || !WingsSettings.flight(player) || !player.isAlive() || player.isSpectator() || !player.isFallFlying()
                || ActionRestrictions.frozen(player)) return false;
        var velocity = player.getDeltaMovement();
        var boosted = tier.accelerate(velocity, player.getLookAngle(), WingsSettings.acceleration(player), flightUpgrade(player));
        if (boosted.equals(velocity)) return false;
        player.setDeltaMovement(boosted);
        return true;
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
    public static void receive(Player player, int level, boolean visible, long shieldStarted) {
        player.getPersistentData().putInt(CLIENT_LEVEL, level);
        player.getPersistentData().putLong(SHIELD_STARTED, shieldStarted);
        setVisible(player, visible);
    }
    @SubscribeEvent public static void protect(DamageReductionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !unlocked(player)
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || event.getDamage() <= 0) return;
        event.reduceBy(ResearchProgress.completed(player, HARDENING) ? .99 : tier(player).damageReduction());
    }
    public static void showShield(ServerPlayer player) {
        if (!unlocked(player)) return;
        player.getPersistentData().putLong(SHIELD_STARTED, player.level().getGameTime());
        WingsNetwork.sync(player);
    }
    @SubscribeEvent public static void researchChanged(ResearchChangedEvent event) {
        if (event.id.equals(RESEARCH) || event.id.equals(ENHANCEMENT))
            org.lyy.lyycore.network.WingsNetwork.sync(event.player);
        if (event.id.equals(SPEED_UP) || event.id.equals(SUPERSONIC))
            org.lyy.lyycore.network.SkillNetwork.sync(event.player);
    }
}
