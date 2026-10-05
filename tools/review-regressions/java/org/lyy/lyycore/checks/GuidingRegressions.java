package org.lyy.lyycore.checks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.blockEntities.AlloyCauldronBlockEntity;
import org.lyy.lyycore.content.entity.guiding.*;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.registry.*;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class GuidingRegressions {
    private static void step(GuidingBoss boss) throws Exception {
        var method = GuidingBoss.class.getDeclaredMethod("customServerAiStep"); method.setAccessible(true); method.invoke(boss);
    }
    @GameTest(template = "empty", batch = "guiding")
    public static void reagentRetainsSpiderEyeAndResearchProducesProof(GameTestHelper test) {
        var pot = new AlloyCauldronBlockEntity(test.absolutePos(BlockPos.ZERO), LyyBlocks.ALLOY_CAULDRON.get().defaultBlockState());
        pot.setLevel(test.getLevel());
        for (var item : List.of(Items.SLIME_BALL, Items.MAGMA_CREAM, Items.SPIDER_EYE, Items.ROTTEN_FLESH,
                LyyItems.HEART_OF_NOTHINGNESS.get(), Items.DRAGON_EGG, Items.NETHER_STAR, LyyItems.CRYSTAL_BLOCK.get())) pot.add(new ItemStack(item));
        test.assertTrue(pot.ingredients().isEmpty() && pot.finishedPotion().is(LyyItems.GUIDING_REAGENT),
                "Complete recipe did not immediately consume its ingredients and produce reagent");
        test.assertTrue(pot.bottle().is(LyyItems.GUIDING_REAGENT), "Eight-ingredient recipe did not produce reagent");
        test.assertTrue(pot.ingredients().isEmpty(), "Bottling did not consume ingredients");
        var research = ResearchManager.get(test.getLevel(), ResourceLocation.parse("lyycore:research/any_more")).value();
        test.assertTrue(research.dangerous() && research.experiencePoints() == 0
                && research.production().orElseThrow().duration() == 600
                && research.production().orElseThrow().result().is(LyyItems.PROOF), "Dangerous research cost or production differs from design");
        test.succeed();
    }
    @GameTest(template = "empty", batch = "guiding")
    public static void guardsReviveAndPhaseTransitionConsumesThem(GameTestHelper test) throws Exception {
        var level = test.getLevel();
        var player = ReviewRegressions.player(level);
        var spawnShield = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        spawnShield.setAccessible(true); spawnShield.setInt(player, 0);
        var origin = test.absolutePos(new BlockPos(3, 30, 3)).getCenter();
        player.moveTo(origin.add(0, 0, 8)); level.addNewPlayer(player);
        var boss = LyyEntities.GUIDING_LIGHT.get().create(level); boss.moveTo(origin);
        boss.beginSummoning(player, test.absolutePos(BlockPos.ZERO)); level.addFreshEntity(boss);
        GuidingBoss next = null;
        try {
            step(boss);
            test.assertTrue(boss.animation().equals("summon"), "Opening summon animation did not start");
            var battle = GuidingEncounter.get(level).battle(boss.getUUID());
            test.assertTrue(battle.guards().size() == 4 && battle.standing().size() == 4, "Opening wave was incomplete");
            var guards = battle.guards().stream().map(id -> (GuidingGuard)level.getEntity(id)).toList();
            test.assertTrue(guards.stream().filter(GuidingGuard::ranged).count() == 2, "Opening guards have wrong types");
            var melee = guards.stream().filter(g -> !g.ranged()).findFirst().orElseThrow();
            melee.setHealth(100); player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); player.setHealth(1000);
            for (int hit = 0; hit < 11; hit++) { player.invulnerableTime = 0; melee.doHurtTarget(player); }
            test.assertTrue(melee.conviction() == 10 && melee.getHealth() == 300
                    && melee.getAttributeValue(Attributes.ATTACK_DAMAGE) == 100, "Conviction or on-hit healing is wrong");
            test.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER), "Melee guard inflicted wither");
            test.assertFalse(boss.hurt(player.damageSources().playerAttack(player), 100), "Boss was vulnerable before guards fell");
            for (var guard : guards) guard.hurt(player.damageSources().playerAttack(player), 10000);
            test.assertTrue(guards.stream().allMatch(g -> g.down() && g.isAlive()), "Defeated guards died instead of falling down");
            test.assertTrue(guards.stream().allMatch(g -> g.animation().equals("down")), "Guards skipped their down animation");
            var restored = LyyEntities.LOST_ADHERENT.get().create(level);
            var saved = new CompoundTag(); guards.getFirst().saveWithoutId(saved); restored.load(saved);
            test.assertTrue(restored.down(), "Downed state was not saved");
            test.assertTrue(restored.animation().equals("down"), "Animation state was not saved with the guard");
            var first = guards.getFirst();
            for (int i = 0; i < 1199; i++) first.aiStep();
            test.assertTrue(first.down(), "Guard revived before sixty seconds");
            first.aiStep();
            test.assertFalse(first.down(), "Guard did not revive after sixty seconds");
            test.assertTrue(first.animation().equals("revive"), "Guard skipped its revive animation");
            test.assertFalse(boss.hurt(player.damageSources().playerAttack(player), 100), "Revived guard did not restore boss protection");
            first.invulnerableTime = 0; first.hurt(player.damageSources().playerAttack(player), 10000);
            test.assertTrue(boss.hurt(player.damageSources().playerAttack(player), 10000), "Boss remained invulnerable after all guards fell");
            next = (GuidingBoss)level.getEntity(battle.boss());
            test.assertTrue(next != boss && next.secondPhase() && next.action() == GuidingBoss.Action.ABSORB, "Second phase did not start absorbing");
            test.assertTrue(next.animation().equals("hatch"), "Second phase did not start hatching");
            test.assertTrue(next.getBbWidth() == 0 && !next.isPickable(), "Absorption still has a collision volume");
            for (int i = 0; i < 40; i++) step(next);
            test.assertTrue(next.getBbWidth() > 0 && guards.stream().noneMatch(Entity::isAlive), "Absorption failed to consume downed guards or restore collision");
            test.assertTrue(next.getMaxHealth() == 4000 && next.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 30, "Second-phase attributes are wrong");
            test.assertTrue(next.getExperienceReward(level, player) == 1000, "Final boss experience reward is wrong");
            next.hurt(player.damageSources().playerAttack(player), 10000);
            var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    next.getBoundingBox().inflate(3));
            test.assertTrue(drops.stream().filter(drop -> drop.getItem().is(LyyItems.UNEXTINGUISHED_DESIRE))
                    .mapToInt(drop -> drop.getItem().getCount()).sum() == 1, "Final boss must drop exactly one Unextinguished Desire");
            test.assertFalse(drops.stream().anyMatch(drop -> drop.getItem().is(LyyItems.HEART_OF_NOTHINGNESS)),
                    "Final boss still drops the former boss's heart");
        } finally {
            if (next != null) next.discard(); boss.discard(); level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        test.succeed();
    }
    @GameTest(template = "empty", batch = "guiding")
    public static void laserTurnRateAndCaptureSpeedsFollowDesign(GameTestHelper test) throws Exception {
        var current = new Vec3(0,0,1); var desired = new Vec3(1,0,0);
        var nextDirection = GuidingLaser.turnTowards(current, desired, Math.toRadians(2.5));
        test.assertTrue(Math.abs(Math.toDegrees(Math.acos(current.dot(nextDirection))) - 2.5) < .001, "Laser exceeded its turn rate");
        var level = test.getLevel(); var player = ReviewRegressions.player(level);
        var origin = test.absolutePos(new BlockPos(3, 50, 3)).getCenter(); player.moveTo(origin.add(0,0,8)); level.addNewPlayer(player);
        var boss = LyyEntities.ENDLESS_DEMAND.get().create(level); boss.moveTo(origin); boss.beginSummoning(player, BlockPos.ZERO); level.addFreshEntity(boss);
        try {
            step(boss);
            // A saved GRAB is deliberately canceled on load. Start a real live grab instead.
            var begin = GuidingBoss.class.getDeclaredMethod("beginGrab", net.minecraft.world.entity.player.Player.class, boolean.class);
            begin.setAccessible(true); begin.invoke(boss, player, false);
            boss.captured(player);
            test.assertTrue(Math.abs(boss.pullVelocity(player).length() - .25) < .0001, "Captured player is not pulled at five meters/second");
            var data = new CompoundTag(); boss.addAdditionalSaveData(data);
            data.putString("Action", "DEVOUR"); data.putInt("ActionTicks", 0); boss.readAdditionalSaveData(data);
            test.assertTrue(Math.abs(boss.pullVelocity(player).length() - .02) < .0001, "Devour approach is not 0.4 meters/second");
            for (int i = 0; i < 80; i++) step(boss);
            test.assertTrue(boss.action() == GuidingBoss.Action.WANDER, "Devour did not end after four seconds");
            for (int i = 0; i < 39; i++) step(boss);
            test.assertTrue(boss.action() == GuidingBoss.Action.WANDER, "Another action interrupted the two-second recovery");
        } finally { boss.discard(); level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
        test.succeed();
    }
}
