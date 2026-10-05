package org.lyy.lyycore.checks;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.DamageReductionEvent;
import org.lyy.lyycore.content.wings.FeatherAttack;
import org.lyy.lyycore.content.wings.WingsTier;
import org.lyy.lyycore.network.WingsNetwork;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class WingsSystemRegressions {
    private static final WingsTier UPGRADE = WingsTier.SECOND;

    private static void unlock(ServerPlayer player) {
        var research = new CompoundTag();
        research.putBoolean(AegisWings.RESEARCH.toString(), true);
        var persisted = new CompoundTag();
        persisted.put("lyycore:research", research);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    @GameTest(template = "empty")
    public static void oldSavesAndUpgradesKeepArmorAndState(GameTestHelper test) {
        var states = new ArrayList<WingsNetwork.State>();
        var player = ReviewRegressions.player(test.getLevel(), packet -> {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof WingsNetwork.State state) states.add(state);
        });
        test.assertTrue(AegisWings.level(player) == 0, "Locked player received wings");
        unlock(player);
        test.assertTrue(AegisWings.tier(player) == WingsTier.FIRST, "Legacy research did not resolve to tier one");
        AegisWings.setVisible(player, false);
        var damage = new DamageReductionEvent(player, player.damageSources().generic(), 100);
        NeoForge.EVENT_BUS.post(damage);
        test.assertTrue(damage.getMultiplier() == .5, "Hidden level-one wings lost armor");
        test.assertTrue(AegisWings.setTier(player, UPGRADE), "Upgrade failed");
        // The fixture has no entity tracker; exercise the explicit observer snapshot instead.
        NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, player));
        test.assertTrue(states.getLast().level() == 2 && !states.getLast().visible(), "Tier/preference was not synchronized");
        var restored = ReviewRegressions.player(test.getLevel());
        restored.getPersistentData().put(Player.PERSISTED_NBT_TAG, player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy());
        test.assertTrue(AegisWings.tier(restored) == UPGRADE && !AegisWings.visible(restored), "Saved upgrade was lost");
        damage = new DamageReductionEvent(restored, restored.damageSources().generic(), 100);
        damage.reduceBy(.5);
        NeoForge.EVENT_BUS.post(damage);
        test.assertTrue(damage.getMultiplier() == .125, "Upgraded armor did not multiply with other reductions");
        var voidDamage = new DamageReductionEvent(restored, restored.damageSources().fellOutOfWorld(), 100);
        NeoForge.EVENT_BUS.post(voidDamage);
        test.assertTrue(voidDamage.getMultiplier() == 1, "Wings reduced void damage");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void weaponUsesTierSnapshotWithoutStyleUnlocks(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        var target = test.spawn(EntityType.COW, new BlockPos(3, 1, 3));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        test.assertFalse(AegisWings.attack(player, target), "Locked wings could attack");
        unlock(player);
        AegisWings.setTier(player, UPGRADE);
        test.assertTrue(AegisWings.attack(player, target), "Innate weapon depended on exploration or style");
        test.assertFalse(AegisWings.attack(player, target), "Same-tick duplicate volley accepted");
        AegisWings.setTier(player, WingsTier.FIRST);
        player.tickCount += FeatherAttack.hitTick(15, 16);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertTrue(Math.abs(target.getHealth() - 520) < .001, "Pending volley lost its 16 x 30 tier snapshot");
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertTrue(Math.abs(target.getHealth() - 520) < .001, "Volley repeated after completion");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void boostIsOptInAndDoesNotBrakeFastFlight(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        unlock(player);
        player.setOnGround(false);
        player.startFallFlying();
        test.assertFalse(AegisWings.boostFlight(player), "Level one gained unintended acceleration");
        AegisWings.setTier(player, UPGRADE);
        player.setYRot(0); player.setXRot(0);
        player.setDeltaMovement(Vec3.ZERO);
        test.assertTrue(AegisWings.boostFlight(player) && player.getDeltaMovement().z > 0, "Configured boost did not accelerate toward view");
        for (int i = 0; i < 100; i++) AegisWings.boostFlight(player);
        test.assertTrue(player.getDeltaMovement().length() <= WingsTier.SECOND.flightBoost().maxSpeed() + .000001, "Boost exceeded its speed cap");
        var fast = new Vec3(0, 0, 3);
        player.setDeltaMovement(fast);
        test.assertFalse(AegisWings.boostFlight(player), "Fast glide accepted extra acceleration");
        test.assertTrue(player.getDeltaMovement().equals(fast), "Boost braked existing flight");
        player.stopFallFlying();
        player.setDeltaMovement(Vec3.ZERO);
        test.assertFalse(AegisWings.boostFlight(player), "Boost worked outside elytra flight");
        test.succeed();
    }
}
