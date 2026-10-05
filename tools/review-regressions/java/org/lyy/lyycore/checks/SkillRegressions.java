package org.lyy.lyycore.checks;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.DamageReductionEvent;
import org.lyy.lyycore.content.skills.*;
import org.lyy.lyycore.network.SkillNetwork;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.wings.FeatherAttack;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class SkillRegressions {
    private static final class TestArrow extends Arrow {
        TestArrow(net.minecraft.server.level.ServerLevel level) { super(EntityType.ARROW, level); }
        void hit(ServerPlayer player) { onHitEntity(new EntityHitResult(player)); }
    }
    private static void unlock(ServerPlayer player) {
        var persisted = new CompoundTag();
        var research = new CompoundTag();
        research.putBoolean(BasicSkills.RESEARCH.toString(), true);
        persisted.put("lyycore:research", research);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        SkillSystem.unlockFromFactor(player);
    }

    @GameTest(template = "empty")
    public static void actionLimitIsPerPlayerPerActionPerServerTick(GameTestHelper test) {
        var first = ReviewRegressions.player(test.getLevel());
        var second = ReviewRegressions.player(test.getLevel());
        unlock(first); unlock(second);
        StyleSystem.select(first, StyleSystem.Style.MOBILITY);
        StyleSystem.select(second, StyleSystem.Style.MOBILITY);
        var key = ResourceLocation.parse("lyycore:regression_action");
        var other = ResourceLocation.parse("lyycore:regression_other_action");
        var calls = new AtomicInteger();
        SkillSystem.register(StyleSystem.Style.MOBILITY, key, new SkillSystem.Skill(BasicSkills.RESEARCH, player -> {
            calls.incrementAndGet();
            test.assertFalse(SkillSystem.cast(player, key), "Reentrant skill cast was accepted");
            return true;
        }));
        SkillSystem.register(StyleSystem.Style.MOBILITY, other, new SkillSystem.Skill(BasicSkills.RESEARCH, player -> true));
        test.assertTrue(SkillSystem.cast(first, key), "First action rejected");
        GuardSkill.reset(first);
        StyleSystem.select(first, StyleSystem.Style.TECHNIQUE);
        StyleSystem.select(first, StyleSystem.Style.MOBILITY);
        test.assertFalse(SkillSystem.cast(first, key), "Style/input reset bypassed tick limit");
        test.assertTrue(SkillSystem.cast(first, other) && SkillSystem.cast(second, key), "Limit leaked to another action/player");
        test.assertTrue(calls.get() == 2, "Action fired more than once");
        test.runAfterDelay(1, () -> {
            test.assertTrue(SkillSystem.cast(first, key) && calls.get() == 3, "Next server tick was incorrectly blocked");
            test.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void repeatedInputEdgesCannotDashTwice(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        unlock(player);
        StyleSystem.select(player, StyleSystem.Style.MOBILITY);
        player.moveTo(test.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        player.setYRot(0); player.setXRot(0);
        var start = player.position();
        SkillInput.accept(player, true, 1, 0);
        var after = player.position();
        test.assertTrue(after.distanceToSqr(start) > 1, "Initial dash did not move");
        for (int i = 0; i < 5; i++) {
            SkillInput.accept(player, false, 0, 0);
            SkillInput.accept(player, true, 1, 0);
        }
        test.assertTrue(player.position().equals(after), "Repeated key edges dashed again in the same tick");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void shieldAnimationStartsOnGuardPressInsteadOfDamage(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        unlock(player);
        player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("lyycore:research")
                .putBoolean(AegisWings.RESEARCH.toString(), true);
        StyleSystem.select(player, StyleSystem.Style.TECHNIQUE);
        long before = AegisWings.shieldStarted(player);
        var passive = new DamageReductionEvent(player, player.damageSources().generic(), 8);
        NeoForge.EVENT_BUS.post(passive);
        test.assertTrue(passive.getMultiplier() == .5 && AegisWings.shieldStarted(player) == before,
                "Passive armor must reduce damage without starting the shield animation");
        SkillInput.accept(player, true, 0, 0);
        long started = AegisWings.shieldStarted(player);
        test.assertTrue(started == test.getLevel().getGameTime() && started != before,
                "Guard press did not start the animation without an incoming hit");
        test.runAfterDelay(1, () -> {
            SkillInput.accept(player, true, 0, 0);
            test.assertTrue(AegisWings.shieldStarted(player) == started, "Holding guard restarted the animation");
            var perfect = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().generic(), 8));
            NeoForge.EVENT_BUS.post(perfect);
            test.assertTrue(perfect.isCanceled() && AegisWings.shieldStarted(player) == started,
                    "Perfect guard failed or restarted the animation on hit");
            player.tickCount += 4;
            var ordinary = new DamageReductionEvent(player, player.damageSources().generic(), 8);
            NeoForge.EVENT_BUS.post(ordinary);
            test.assertTrue(ordinary.getMultiplier() == .25 && AegisWings.shieldStarted(player) == started,
                    "Ordinary guard failed or restarted the animation on hit");
            test.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void guardTapAndCounterPublishOnlyFinalState(GameTestHelper test) {
        var states = new ArrayList<SkillNetwork.State>();
        var player = ReviewRegressions.player(test.getLevel(), packet -> {
            if (packet instanceof ClientboundCustomPayloadPacket payload && payload.payload() instanceof SkillNetwork.State state) states.add(state);
        });
        unlock(player);
        StyleSystem.select(player, StyleSystem.Style.TECHNIQUE);
        GuardSkill.setValue(player, 50);
        player.setShiftKeyDown(true);
        SkillInput.accept(player, true, 1, 0);
        SkillInput.accept(player, false, 0, 0);
        test.assertTrue(GuardSkill.guarding(player), "Counter tap lost its judgment window");
        var target = EntityType.ZOMBIE.create(test.getLevel());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        states.clear();
        var damage = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(target), 8));
        NeoForge.EVENT_BUS.post(damage);
        test.assertTrue(damage.isCanceled() && GuardSkill.value(player) == 0 && target.getHealth() < 200, "Perfect counter did not cancel incoming damage");
        test.assertTrue(states.size() == 1 && states.getFirst().guard() == 0, "Intermediate guard charge was synchronized");
        SkillNetwork.sync(player);
        test.assertTrue(states.size() == 1, "Unchanged state was sent twice");
        player.tickCount += 3;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertFalse(GuardSkill.guarding(player), "Released guard did not expire");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void skeletonArrowCanBePerfectCountered(GameTestHelper test) {
        var skeleton = test.spawn(EntityType.SKELETON, new BlockPos(3, 1, 3));
        skeleton.setNoAi(true);
        skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        skeleton.getAttribute(Attributes.ARMOR).setBaseValue(0);
        for (int elapsed = 0; elapsed <= 4; elapsed++) {
            var player = ReviewRegressions.player(test.getLevel());
            // End vanilla login invulnerability before exercising the real arrow hit path.
            for (int tick = 0; tick < 60; tick++) player.tick();
            unlock(player);
            StyleSystem.select(player, StyleSystem.Style.TECHNIQUE);
            player.moveTo(skeleton.position());
            GuardSkill.setValue(player, 50);
            player.setShiftKeyDown(true);
            skeleton.setHealth(200);
            skeleton.invulnerableTime = 0;
            SkillInput.accept(player, false, 1, 0); // W was already held.
            SkillInput.accept(player, true, 1, 0);
            player.tickCount += elapsed;
            if (elapsed == 4) NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
            var arrow = new TestArrow(test.getLevel());
            arrow.setOwner(skeleton);
            arrow.setDeltaMovement(0, 0, 1);
            arrow.setBaseDamage(8);
            arrow.hit(player);
            if (elapsed < 4) {
                test.assertTrue(player.getHealth() == 20 && player.hurtTime == 0, "Skeleton arrow bypassed perfect guard");
                test.assertTrue(GuardSkill.value(player) == 0 && Math.abs(skeleton.getHealth() - 136) < .001,
                        "Skeleton arrow did not trigger the 64-damage perfect counter at tick " + elapsed);
            } else {
                test.assertTrue(GuardSkill.value(player) == 0 && Math.abs(skeleton.getHealth() - 180) < .001,
                        "Ordinary counter did not fire at the end of the extended window");
                test.assertTrue(player.getHealth() < 20, "Late arrow was canceled after both perfect windows expired");
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void countersRequireSneakingAtStartAndResolution(GameTestHelper test) {
        for (boolean sneakAtStart : new boolean[] {false, true}) {
            for (boolean sneakAtResolution : new boolean[] {false, true}) {
                if (sneakAtStart && sneakAtResolution) continue; // Covered by the counter success tests.
                for (boolean incomingHit : new boolean[] {false, true}) {
                    var player = ReviewRegressions.player(test.getLevel());
                    unlock(player);
                    StyleSystem.select(player, StyleSystem.Style.TECHNIQUE);
                    var target = test.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
                    target.setNoAi(true);
                    player.moveTo(target.position());
                    float targetHealth = target.getHealth();
                    GuardSkill.setValue(player, 50);
                    player.setShiftKeyDown(sneakAtStart);
                    SkillInput.accept(player, true, 1, 0);
                    player.setShiftKeyDown(sneakAtResolution);
                    if (incomingHit) {
                        var damage = new LivingIncomingDamageEvent(player,
                                new DamageContainer(player.damageSources().mobAttack(target), 8));
                        NeoForge.EVENT_BUS.post(damage);
                        test.assertTrue(damage.isCanceled(), "Sneaking requirement disabled perfect guard");
                        test.assertTrue(target.getHealth() == targetHealth && GuardSkill.value(player) == 60,
                                "Perfect counter bypassed sneaking requirement");
                    }
                    player.tickCount += 4;
                    NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
                    test.assertTrue(target.getHealth() == targetHealth && GuardSkill.value(player) == (incomingHit ? 60 : 50),
                            "Ordinary counter bypassed sneaking requirement or consumed guard points");
                    target.discard();
                }
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void perfectGuardCancelsBeforeDamageProcessing(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        unlock(player);
        StyleSystem.select(player, StyleSystem.Style.TECHNIQUE);
        SkillInput.accept(player, true, 0, 0);
        float health = player.getHealth();
        // Generic kill bypasses the fixture player's initial spawn protection.
        test.assertFalse(player.hurt(player.damageSources().genericKill(), 8), "Perfect guard still accepted the hit");
        test.assertTrue(player.getHealth() == health && player.hurtTime == 0 && player.invulnerableTime == 0,
                "Canceled hit changed health or triggered hurt feedback");
        test.assertTrue(GuardSkill.value(player) == 10, "Canceled hit did not award guard charge");

        var voidHit = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().fellOutOfWorld(), 8));
        NeoForge.EVENT_BUS.post(voidHit);
        test.assertFalse(voidHit.isCanceled(), "Guard canceled void damage");

        player.tickCount += 4;
        var source = player.damageSources().generic();
        var incoming = new LivingIncomingDamageEvent(player, new DamageContainer(source, 8));
        NeoForge.EVENT_BUS.post(incoming);
        test.assertFalse(incoming.isCanceled(), "Ordinary guard canceled damage after the perfect window");
        var reduction = new DamageReductionEvent(player, source, 8);
        NeoForge.EVENT_BUS.post(reduction);
        test.assertTrue(reduction.getMultiplier() == .5 && GuardSkill.value(player) == 6,
                "Ordinary guard lost its reduction or guard cost");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void buildingFlightOnlyRevokesItsOwnPermission(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        unlock(player);
        StyleSystem.select(player, StyleSystem.Style.BUILDING);
        BuildingSkills.updateFlight(player);
        test.assertTrue(player.getAbilities().mayfly, "Building flight not granted");
        StyleSystem.select(player, StyleSystem.Style.MOBILITY);
        BuildingSkills.updateFlight(player);
        test.assertFalse(player.getAbilities().mayfly, "Owned flight not revoked");
        player.getAbilities().mayfly = true;
        StyleSystem.select(player, StyleSystem.Style.BUILDING);
        BuildingSkills.updateFlight(player);
        StyleSystem.select(player, StyleSystem.Style.MOBILITY);
        BuildingSkills.updateFlight(player);
        test.assertTrue(player.getAbilities().mayfly, "An external flight permission was revoked");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void offenseSchedulesOneVolleyAndFourHits(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        unlock(player);
        StyleSystem.select(player, StyleSystem.Style.OFFENSE);
        var target = test.spawn(EntityType.COW, new BlockPos(3, 1, 3));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        target.hurt(player.damageSources().playerAttack(player), 1);
        float withoutWings = target.getHealth();
        player.tickCount += FeatherAttack.hitTick(3);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertTrue(target.getHealth() == withoutWings, "Offense created feather attacks without innate wings");
        player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("lyycore:research")
                .putBoolean(AegisWings.RESEARCH.toString(), true);
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), 1);
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), 1);
        float beforeFeathers = target.getHealth();
        player.tickCount += FeatherAttack.hitTick(3);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertTrue(Math.abs(target.getHealth() - (beforeFeathers - 40)) < .001, "Repeated attack triggered extra volleys or lost a scheduled feather");
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertTrue(Math.abs(target.getHealth() - (beforeFeathers - 40)) < .001, "Delayed hits recursively scheduled another volley");
        test.succeed();
    }
}
