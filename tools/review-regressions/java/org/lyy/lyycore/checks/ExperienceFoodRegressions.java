package org.lyy.lyycore.checks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.registry.LyyItems;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ExperienceFoodRegressions {
    @GameTest(template = "empty")
    public static void bossDropsGrantExperienceWhenEaten(GameTestHelper test) {
        checkConsumption(test, LyyItems.PURE_CRYSTAL.get(), 300, false);
        checkConsumption(test, LyyItems.HEART_OF_NOTHINGNESS.get(), 500, false);
        checkConsumption(test, LyyItems.UNEXTINGUISHED_DESIRE.get(), 1000, false);
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeEatingPreservesBossDrops(GameTestHelper test) {
        checkConsumption(test, LyyItems.PURE_CRYSTAL.get(), 300, true);
        checkConsumption(test, LyyItems.HEART_OF_NOTHINGNESS.get(), 500, true);
        checkConsumption(test, LyyItems.UNEXTINGUISHED_DESIRE.get(), 1000, true);
        test.succeed();
    }

    private static void checkConsumption(GameTestHelper test, Item item, int experience, boolean creative) {
        var player = ReviewRegressions.player(test.getLevel());
        if (creative) player.setGameMode(GameType.CREATIVE);
        player.getFoodData().setFoodLevel(20);
        var stack = new ItemStack(item, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = item.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(result.getResult().consumesAction() && player.isUsingItem(), "Must be edible at full hunger");
        player.releaseUsingItem();
        test.assertTrue(player.totalExperience == 0 && stack.getCount() == 2, "Interrupted eating must not grant XP or consume items");
        item.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        // Tick the normal use lifecycle so finishing cannot accidentally grant XP twice.
        int duration = stack.getUseDuration(player);
        for (int tick = 0; tick <= duration; tick++) player.doTick();
        test.assertTrue(player.totalExperience == experience, "Wrong XP reward for " + item);
        test.assertTrue(player.getMainHandItem().getCount() == (creative ? 2 : 1), "Incorrect item consumption");
        test.assertTrue(player.getFoodData().getFoodLevel() == 20, "XP food must preserve hunger");
    }
}
