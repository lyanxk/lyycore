package org.lyy.lyycore.checks;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.blockEntities.MindControlBeaconBlockEntity;
import org.lyy.lyycore.content.cauldron.CauldronMixes;
import org.lyy.lyycore.content.control.MindControl;
import org.lyy.lyycore.content.control.MindControlData;
import org.lyy.lyycore.content.entity.guiding.*;
import org.lyy.lyycore.content.wings.ScoopFeather;
import org.lyy.lyycore.content.wings.WingsScoop;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyEntities;

/** Interrupted operations and lifecycle boundaries, in addition to the normal gameplay checks. */
@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class LifecycleRegressions {
    private static Method bossMethod(String name, Class<?>... types) throws Exception {
        var method = GuidingBoss.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
    private static UUID grabId(GuidingBoss boss) throws Exception {
        var field = GuidingBoss.class.getDeclaredField("activeGrab");
        field.setAccessible(true);
        return (UUID) field.get(boss);
    }

    @GameTest(template = "empty", batch = "lifecycle_grab")
    public static void failedRemovedAndUnloadedGrabsRecover(GameTestHelper test) throws Exception {
        var level = test.getLevel();
        var player = ReviewRegressions.player(level);
        var origin = test.absolutePos(new BlockPos(3, 30, 3)).getCenter();
        player.moveTo(origin.add(0, 0, 8)); level.addNewPlayer(player);
        var boss = LyyEntities.ENDLESS_DEMAND.get().create(level);
        boss.moveTo(origin); boss.beginSummoning(player, BlockPos.ZERO); level.addFreshEntity(boss);
        var step = bossMethod("customServerAiStep");
        var begin = bossMethod("beginGrab", Player.class, boolean.class);
        Consumer<EntityJoinLevelEvent> reject = event -> {
            if (event.getEntity() instanceof GuidingGrab) event.setCanceled(true);
        };
        try {
            step.invoke(boss);
            NeoForge.EVENT_BUS.addListener(reject);
            try {
                begin.invoke(boss, player, false);
                test.assertFalse(boss.grabbing(), "Rejected projectile left the boss grabbing");
            } finally { NeoForge.EVENT_BUS.unregister(reject); }

            begin.invoke(boss, player, false);
            var obsolete = grabId(boss);
            test.assertTrue(obsolete != null, "Live grab did not receive an identity");
            level.getEntity(obsolete).discard();
            test.assertFalse(boss.grabbing(), "Permanent projectile removal did not release the action");

            begin.invoke(boss, player, true);
            var current = grabId(boss);
            boss.grabMissed(obsolete);
            test.assertTrue(boss.ownsGrab(current), "A stale callback canceled a later grab");
            level.getEntity(current).remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            test.assertTrue(boss.grabbing(), "Ordinary projectile unload was treated as immediate destruction");
            for (int tick = 0; tick < 20; tick++) step.invoke(boss);
            test.assertFalse(boss.grabbing(), "Unavailable projectile left the boss permanently waiting");

            begin.invoke(boss, player, false);
            var late = level.getEntity(grabId(boss));
            var saved = new CompoundTag(); boss.addAdditionalSaveData(saved);
            boss.readAdditionalSaveData(saved);
            test.assertFalse(boss.grabbing(), "Reload resumed a transient grab without a valid projectile");
            late.tick();
            test.assertTrue(late.isRemoved() && !boss.grabbing(), "An obsolete projectile survived boss reload");
        } finally {
            boss.discard(); level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "lifecycle_dimension")
    public static void encounterMembersCannotLeaveTheirDimension(GameTestHelper test) {
        var level = test.getLevel();
        var destination = level.getServer().getLevel(Level.NETHER);
        test.assertTrue(destination != null, "Nether is missing from the test server");
        var boss = LyyEntities.GUIDING_LIGHT.get().create(level);
        var guard = LyyEntities.LOST_ADHERENT.get().create(level);
        var origin = test.absolutePos(new BlockPos(3, 20, 3)).getCenter();
        boss.moveTo(origin); guard.moveTo(origin.add(2, 0, 0));
        level.addFreshEntity(boss); level.addFreshEntity(guard);
        var data = GuidingEncounter.get(level);
        var battle = data.create(boss.getUUID(), boss.getUUID(), null);
        guard.bind(boss.getUUID()); data.addGuard(boss.getUUID(), guard.getUUID());
        try {
            var transition = new DimensionTransition(destination, origin, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING);
            test.assertTrue(guard.changeDimension(transition) == null && guard.level() == level && !guard.isRemoved(),
                    "Guard escaped its dimension-local encounter");
            test.assertTrue(boss.changeDimension(transition) == null && boss.level() == level && !boss.isRemoved(),
                    "Boss escaped its dimension-local encounter");
            guard.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            test.assertTrue(battle.guards().contains(guard.getUUID()) && battle.standing().contains(guard.getUUID()),
                    "Chunk unload removed the guard's standing record");
        } finally {
            if (!guard.isRemoved()) guard.discard();
            data.removeGuard(boss.getUUID(), guard.getUUID()); data.end(boss.getUUID()); boss.discard();
        }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "lifecycle_execution")
    public static void canceledExecutionKeepsControlAndRequiresExplicitRetry(GameTestHelper test) throws Exception {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(3, 1, 3));
        level.setBlockAndUpdate(pos, LyyBlocks.MIND_CONTROL_BEACON.get().defaultBlockState());
        var beacon = (MindControlBeaconBlockEntity) level.getBlockEntity(pos);
        var owner = ReviewRegressions.player(level); beacon.activate(owner);
        var mob = test.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        MindControl.bind(mob, beacon.binding());
        var data = MindControlData.get(level);
        var binding = beacon.binding();
        var attempts = new int[1];
        Consumer<LivingIncomingDamageEvent> cancel = event -> {
            if (event.getEntity() == mob) { attempts[0]++; event.setCanceled(true); }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            test.assertTrue(beacon.command(owner, 0), "Execution command was rejected");
            test.assertTrue(attempts[0] == 1 && mob.isAlive() && MindControl.binding(mob) == binding,
                    "Canceled damage lost control or ran more than once");
            test.assertTrue(data.execution(mob.getUUID()).failed(), "Failed execution was not recorded");
            for (int tick = 0; tick < 5; tick++) MindControl.processExecution(mob);
            test.assertTrue(attempts[0] == 1, "A failed execution was automatically retried");
            var load = MindControlData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
            load.setAccessible(true);
            var restored = (MindControlData) load.invoke(null, data.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            test.assertTrue(restored.execution(mob.getUUID()).failed() && restored.beginExecution(mob.getUUID()) == null,
                    "Save/load rearmed a failed command");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        try {
            data.execute(binding); // Explicit retry through the same command service, after removing cancellation.
            MindControl.processExecution(mob);
            test.assertTrue(!mob.isAlive() && !binding.mobs.contains(mob.getUUID()) && data.execution(mob.getUUID()) == null,
                    "Successful retry was not acknowledged");
        } finally { mob.discard(); beacon.release(); }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "lifecycle_deferred")
    public static void deferredExecutionKeepsIssuingOwnerAfterRebind(GameTestHelper test) throws Exception {
        var level = test.getLevel(); var data = MindControlData.get(level);
        var firstOwner = UUID.randomUUID(); var secondOwner = UUID.randomUUID();
        var first = data.activate(firstOwner, GlobalPos.of(level.dimension(), test.absolutePos(BlockPos.ZERO)));
        var second = data.activate(secondOwner, GlobalPos.of(level.dimension(), test.absolutePos(new BlockPos(6, 1, 6))));
        var mob = EntityType.ZOMBIE.create(level); mob.moveTo(test.absolutePos(new BlockPos(3, 2, 3)).getCenter());
        MindControl.bind(mob, first); data.execute(first);
        var request = data.execution(mob.getUUID());
        var load = MindControlData.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        var restored = (MindControlData) load.invoke(null, data.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        test.assertTrue(restored.execution(mob.getUUID()).equals(request), "Deferred command lost attribution on save/load");
        data.release(firstOwner, first.beacon);
        MindControl.bind(mob, second);
        test.assertTrue(MindControl.binding(mob) == second && data.execution(mob.getUUID()).owner().equals(firstOwner),
                "Rebinding rewrote the deferred execution issuer");
        var killer = new UUID[1];
        Consumer<LivingDeathEvent> death = event -> {
            if (event.getEntity() == mob && event.getSource().getEntity() != null)
                killer[0] = event.getSource().getEntity().getUUID();
        };
        NeoForge.EVENT_BUS.addListener(death);
        try {
            level.addFreshEntity(mob); MindControl.processExecution(mob);
            test.assertTrue(!mob.isAlive() && firstOwner.equals(killer[0]), "Deferred kill was credited to the new owner");
        } finally {
            NeoForge.EVENT_BUS.unregister(death); mob.discard(); data.release(secondOwner, second.beacon);
        }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "lifecycle_commands")
    public static void repeatedCommandsAreCoalescedPerTick(GameTestHelper test) {
        var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(3, 1, 3));
        level.setBlockAndUpdate(pos, LyyBlocks.MIND_CONTROL_BEACON.get().defaultBlockState());
        var beacon = (MindControlBeaconBlockEntity) level.getBlockEntity(pos);
        var owner = ReviewRegressions.player(level); beacon.activate(owner);
        var mob = test.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        test.assertTrue(beacon.command(owner, 2), "First scan failed");
        var newcomer = test.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        test.assertFalse(beacon.command(owner, 2), "Second scan was executed in the same tick");
        test.assertTrue(MindControl.binding(newcomer) == null, "Repeated request still scanned new entities");
        test.assertTrue(beacon.command(owner, 4), "First mode change failed");
        mob.setTarget(newcomer);
        test.assertTrue(beacon.command(owner, 4) && mob.getTarget() == newcomer, "Same mode reset combat targeting");
        test.assertFalse(beacon.command(owner, 1), "Alternating mode requests bypassed the same-tick limit");
        test.runAfterDelay(1, () -> {
            try {
                test.assertTrue(beacon.command(owner, 2) && MindControl.binding(newcomer) != null,
                        "A later tick could not scan");
                test.assertTrue(beacon.command(owner, 1) && beacon.binding().mode() == MindControlData.Mode.GATHER,
                        "A later tick could not change mode");
                test.succeed();
            } finally { mob.discard(); newcomer.discard(); beacon.release(); }
        });
    }

    @GameTest(template = "empty", batch = "lifecycle_teleport")
    public static void shortTeleportsResetScoopAtTheActualServerTeleport(GameTestHelper test) {
        var level = test.getLevel(); var player = ReviewRegressions.player(level);
        var origin = test.absolutePos(new BlockPos(3, 40, 3)).getCenter();
        player.moveTo(origin); player.setYRot(0); level.addNewPlayer(player);
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        var research = persisted.getCompound("lyycore:research");
        research.putBoolean(AegisWings.ENHANCEMENT.toString(), true);
        persisted.put("lyycore:research", research); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        try {
            test.assertTrue(WingsScoop.start(player), "Could not start the level-two scoop");
            var feathers = level.getEntitiesOfClass(ScoopFeather.class, player.getBoundingBox().inflate(20));
            test.assertTrue(feathers.size() == 6, "Scoop collision representatives are incomplete");
            player.tickCount += 2;
            feathers.forEach(ScoopFeather::tick);
            var before = feathers.stream().map(feather -> feather.getBoundingBox().getCenter()).toList();
            // Do not call WingsScoop.teleported here: verify the real network-listener mixin path.
            player.teleportTo(origin.x + 4, origin.y, origin.z);
            for (int i = 0; i < feathers.size(); i++)
                test.assertTrue(feathers.get(i).getBoundingBox().getCenter().distanceTo(before.get(i).add(4, 0, 0)) < .00001,
                        "A four-block teleport retained a pre-teleport collision sample");
            player.teleportTo(origin.x + 2, origin.y, origin.z);
            for (int i = 0; i < feathers.size(); i++)
                test.assertTrue(feathers.get(i).getBoundingBox().getCenter().distanceTo(before.get(i).add(2, 0, 0)) < .00001,
                        "A second short teleport in the same tick failed to relocate the sweep");
            var sample = feathers.getFirst().getBoundingBox().getCenter();
            player.setPos(origin.x + 3, origin.y, origin.z);
            test.assertTrue(feathers.getFirst().getBoundingBox().getCenter().equals(sample),
                    "Ordinary movement incorrectly reset the sweep immediately");
        } finally {
            WingsScoop.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "lifecycle_recipes")
    public static void cachedCauldronRecipesRemainIsolatedFromCallers(GameTestHelper test) {
        var recipes = CauldronMixes.specials();
        test.assertTrue(recipes == CauldronMixes.specials(), "Fixed recipes are reconstructed per call");
        var recipe = recipes.getFirst(); var ingredients = recipe.ingredients();
        int before = ingredients.getFirst().getCount(); ingredients.getFirst().setCount(0);
        test.assertTrue(recipe.ingredients().getFirst().getCount() == before, "A caller mutated the cached recipe");
        test.assertTrue(CauldronMixes.specialIngredient(Items.SPIDER_EYE) && !CauldronMixes.specialIngredient(Items.COBBLESTONE),
                "Cached special-ingredient index differs from the recipes");
        test.succeed();
    }
}
