package org.lyy.lyycore.checks;

import com.illusivesoulworks.caelus.api.CaelusApi;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.content.wings.WingsFlight;
import java.util.function.Consumer;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class WingFlightRegressions {
    private static ServerPlayer player(GameTestHelper test, boolean unlocked) {
        var player = ReviewRegressions.player(test.getLevel());
        player.moveTo(test.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        player.setOnGround(false);
        var research = new CompoundTag();
        research.putBoolean(AegisWings.RESEARCH.toString(), unlocked);
        var persisted = new CompoundTag();
        persisted.put("lyycore:research", research);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        WingsFlight.update(player);
        return player;
    }

    private static void startFlight(ServerPlayer player) throws Exception {
        // Use the server handler called by Caelus's client jump input, not vanilla's equipment-only method.
        Class.forName("com.illusivesoulworks.caelus.common.network.CPacketFlight")
                .getMethod("handle", ServerPlayer.class).invoke(null, player);
    }

    private static void flightTick(ServerPlayer player, int ticks) throws Exception {
        // Exercise the transformed vanilla method, including its equipment checks.
        var counter = LivingEntity.class.getDeclaredField("fallFlyTicks");
        counter.setAccessible(true);
        counter.setInt(player, ticks);
        var update = LivingEntity.class.getDeclaredMethod("updateFallFlying");
        update.setAccessible(true);
        update.invoke(player);
    }

    @GameTest(template = "empty")
    public static void wingsFlyWithoutUsableElytra(GameTestHelper test) throws Exception {
        var player = player(test, true);
        var broken = new ItemStack(Items.ELYTRA);
        broken.setDamageValue(broken.getMaxDamage() - 1);
        for (var chest : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.DIAMOND_CHESTPLATE), broken}) {
            player.stopFallFlying();
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            startFlight(player);
            test.assertTrue(player.isFallFlying(), "Wings could not start with " + chest);
            flightTick(player, 19);
            test.assertTrue(player.isFallFlying(), "Equipment disabled innate wings");
        }
        player.setOnGround(true);
        flightTick(player, 20);
        test.assertFalse(player.isFallFlying(), "Wings did not stop on landing");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void wingsPreserveEquippedElytraTicks(GameTestHelper test) throws Exception {
        var player = player(test, true);
        var elytra = new ItemStack(Items.ELYTRA);
        elytra.setDamageValue(7);
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        startFlight(player);
        test.assertTrue(player.isFallFlying(), "Wing flight did not start");
        for (int ticks = 0; ticks < 40; ticks++) flightTick(player, ticks);
        test.assertTrue(player.isFallFlying(), "Wing flight stopped");
        test.assertTrue(elytra.getDamageValue() == 9, "Caelus bypassed normal equipped elytra durability");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedWingsPreserveNormalElytraFlight(GameTestHelper test) throws Exception {
        var player = player(test, false);
        test.assertFalse(player.tryToStartFallFlying(), "Locked wings allowed unequipped flight");
        var elytra = new ItemStack(Items.ELYTRA);
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        test.assertTrue(player.tryToStartFallFlying(), "Normal elytra could not start");
        flightTick(player, 19);
        test.assertTrue(player.isFallFlying(), "Normal elytra flight stopped");
        test.assertTrue(elytra.getDamageValue() == 1, "Original elytra tick was skipped");
        elytra.setDamageValue(elytra.getMaxDamage() - 1);
        flightTick(player, 20);
        test.assertFalse(player.isFallFlying(), "Broken elytra kept flying without wings");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void researchChangesOnlyOwnFlightModifier(GameTestHelper test) throws Exception {
        var player = player(test, false);
        var research = ResearchManager.all(test.getLevel()).stream()
                .filter(entry -> entry.id().equals(AegisWings.RESEARCH)).findFirst().orElseThrow().value();
        ResearchProgress.setCompleted(player, AegisWings.RESEARCH, research, true);
        WingsFlight.update(player);
        var attribute = player.getAttribute(CaelusApi.getInstance().getFallFlyingAttribute());
        var ownId = ResourceLocation.parse("lyycore:aegis_wings_flight");
        test.assertTrue(attribute.hasModifier(ownId), "Research did not grant flight");
        var other = new AttributeModifier(ResourceLocation.parse("regression:other_flight"), 1, AttributeModifier.Operation.ADD_VALUE);
        attribute.addTransientModifier(other);
        ResearchProgress.setCompleted(player, AegisWings.RESEARCH, research, false);
        test.assertFalse(attribute.hasModifier(ownId), "Forgotten research kept flight permission");
        test.assertTrue(attribute.hasModifier(other.id()), "Research removed another provider's modifier");
        startFlight(player);
        flightTick(player, 1);
        test.assertTrue(player.isFallFlying(), "Other provider lost flight after forgetting wings");
        attribute.removeModifier(other.id());
        flightTick(player, 2);
        test.assertFalse(player.isFallFlying(), "Flight continued after all providers were removed");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void respawnAndLoginRestoreTransientFlight(GameTestHelper test) throws Exception {
        var original = player(test, true);
        for (boolean respawn : new boolean[]{false, true}) {
            var restored = player(test, false);
            restored.getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    original.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy());
            if (respawn) NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(restored, false));
            else NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(restored));
            startFlight(restored);
            flightTick(restored, 1);
            test.assertTrue(restored.isFallFlying(), "Persisted research did not restore flight on " + (respawn ? "respawn" : "login"));
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void innateGlidingEmitsOneGameEvent(GameTestHelper test) throws Exception {
        for (boolean equipped : new boolean[]{false, true}) {
            var player = player(test, true);
            if (equipped) player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            int[] count = {0};
            Consumer<VanillaGameEvent> listener = event -> {
                if (event.getCause() == player && event.getVanillaEvent().equals(GameEvent.ELYTRA_GLIDE)) count[0]++;
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                startFlight(player);
                flightTick(player, 9); // Vanilla emits on (fallFlyTicks + 1) % 10 == 0.
                var counter = LivingEntity.class.getDeclaredField("fallFlyTicks");
                counter.setAccessible(true);
                counter.setInt(player, 10); // LivingEntity increments before PlayerTickEvent.Post.
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                test.assertTrue(count[0] == 1, "Expected one glide event, got " + count[0] + " (equipped=" + equipped + ")");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        test.succeed();
    }
}
