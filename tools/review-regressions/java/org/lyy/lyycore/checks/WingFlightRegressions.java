package org.lyy.lyycore.checks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.AegisWings;

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
        return player;
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
            test.assertTrue(player.tryToStartFallFlying(), "Wings could not start with " + chest);
            flightTick(player, 19);
            test.assertTrue(player.isFallFlying(), "Equipment disabled innate wings");
        }
        player.setOnGround(true);
        flightTick(player, 20);
        test.assertFalse(player.isFallFlying(), "Wings did not stop on landing");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void wingsTakePriorityOverEquippedElytra(GameTestHelper test) throws Exception {
        var player = player(test, true);
        var elytra = new ItemStack(Items.ELYTRA);
        elytra.setDamageValue(7);
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        test.assertTrue(player.tryToStartFallFlying(), "Wing flight did not start");
        for (int ticks = 0; ticks < 40; ticks++) flightTick(player, ticks);
        test.assertTrue(player.isFallFlying(), "Wing flight stopped");
        test.assertTrue(elytra.getDamageValue() == 7, "Innate wings consumed equipped elytra durability");
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
}
