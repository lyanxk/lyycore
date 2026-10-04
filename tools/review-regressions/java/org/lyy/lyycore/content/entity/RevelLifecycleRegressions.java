package org.lyy.lyycore.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.registry.LyyEntities;
import java.util.*;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class RevelLifecycleRegressions {
    private static LifeRevel boss(GameTestHelper test) {
        var boss = LyyEntities.LIFE_REVEL.get().create(test.getLevel());
        boss.setNoAi(true);
        boss.setPos(test.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        test.getLevel().addFreshEntity(boss);
        return boss;
    }
    private static RevelDancer dancer(GameTestHelper test, LifeRevel boss) {
        var dancer = LyyEntities.REVEL_DANCER.get().create(test.getLevel());
        dancer.setNoAi(true); dancer.bind(boss); dancer.setPos(boss.position().add(2, 0, 0));
        test.getLevel().addFreshEntity(dancer); boss.minionAvailable(dancer);
        return dancer;
    }
    private static void reconcile(LifeRevel boss) throws Exception {
        var method = LifeRevel.class.getDeclaredMethod("reconcileSummons");
        method.setAccessible(true); method.invoke(boss);
    }
    @GameTest(template = "empty")
    public static void unloadedSummonRemainsOwned(GameTestHelper test) throws Exception {
        var boss = boss(test);
        var dancer = dancer(test, boss);
        var saved = new CompoundTag(); dancer.saveWithoutId(saved);
        try {
            dancer.remove(RemovalReason.UNLOADED_TO_CHUNK);
            reconcile(boss);
            test.assertTrue(boss.dancers().contains(dancer.getUUID()), "Unloaded summon was counted as removed");
            var loaded = LyyEntities.REVEL_DANCER.get().create(test.getLevel());
            loaded.load(saved);
            test.getLevel().addFreshEntity(loaded);
            loaded.customServerAiStep();
            test.assertTrue(boss.dancers().size() == 1, "Reload duplicated summon ownership");
            loaded.discard();
            test.assertTrue(boss.dancers().isEmpty(), "Actual destruction was not reported");
        } finally { boss.discard(); }
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void minionDeathsWhileBossUnloadedAreReplayedOnce(GameTestHelper test) throws Exception {
        var original = boss(test);
        var dancers = List.of(dancer(test, original), dancer(test, original), dancer(test, original));
        var saved = new CompoundTag(); original.saveWithoutId(saved);
        original.remove(RemovalReason.UNLOADED_TO_CHUNK);
        test.assertTrue(dancers.getFirst().boss() == null, "Boss fixture did not unload");
        for (var dancer : dancers) dancer.kill();
        var restored = LyyEntities.LIFE_REVEL.get().create(test.getLevel());
        restored.load(saved); test.getLevel().addFreshEntity(restored);
        try {
            reconcile(restored);
            test.assertTrue(restored.dancers().isEmpty(), "Deferred removals were lost");
            var result = new CompoundTag(); restored.addAdditionalSaveData(result);
            test.assertTrue(result.getInt("PendingLasers") == 1, "Deferred dancer deaths lost their laser credit");
            reconcile(restored); restored.addAdditionalSaveData(result);
            test.assertTrue(result.getInt("PendingLasers") == 1, "Death notifications were applied twice");
        } finally { restored.discard(); dancers.forEach(RevelDancer::discard); }
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void knownUnloadedBossDoesNotExpireMinions(GameTestHelper test) {
        var original = boss(test);
        var dancer = dancer(test, original);
        var saved = new CompoundTag(); original.saveWithoutId(saved);
        original.remove(RemovalReason.UNLOADED_TO_CHUNK);
        for (int i = 0; i < 220; i++) dancer.customServerAiStep();
        test.assertFalse(dancer.isRemoved(), "A temporarily unloaded boss caused its minion to expire");
        var restored = LyyEntities.LIFE_REVEL.get().create(test.getLevel());
        restored.load(saved); test.getLevel().addFreshEntity(restored);
        restored.discard();
        test.assertTrue(dancer.isRemoved(), "Boss destruction failed to clear loaded summons");
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void removalJournalSurvivesSaveAndCleansFinishedOwners(GameTestHelper test) {
        var data = new RevelSummons();
        var boss = UUID.randomUUID(); var first = UUID.randomUUID(); var second = UUID.randomUUID();
        data.register(boss, List.of(first, second), List.of());
        data.removed(boss, first, true, true);
        var restored = RevelSummons.load(data.save(new CompoundTag(), test.getLevel().registryAccess()), test.getLevel().registryAccess());
        test.assertTrue(restored.active(boss) && Boolean.TRUE.equals(restored.drain(boss).get(first)), "Pending death failed to persist");
        test.assertTrue(restored.drain(boss).isEmpty(), "Journal replay was not consumed");
        restored.end(boss, true);
        var finished = RevelSummons.load(restored.save(new CompoundTag(), test.getLevel().registryAccess()), test.getLevel().registryAccess());
        test.assertTrue(finished.ended(boss) && finished.killed(boss), "Unloaded summon lost owner termination");
        finished.removed(boss, second, false, false);
        test.assertFalse(finished.ended(boss), "Completed owner record leaked after its last summon was removed");
        test.succeed();
    }
}
