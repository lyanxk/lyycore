package org.lyy.lyycore.checks;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.skills.BasicSkills;
import org.lyy.lyycore.content.skills.SkillInput;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.content.skills.StyleSystem;
import org.lyy.lyycore.content.wings.WingsScoop;
import org.lyy.lyycore.network.SkillNetwork;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class AdvancedSkillRegressions {
    private static void research(ServerPlayer player, String... ids) {
        SkillSystem.unlockFromFactor(player);
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        var completed = new CompoundTag();
        for (String id : ids) completed.putBoolean("lyycore:research/" + id, true);
        persisted.put("lyycore:research", completed);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.setShiftKeyDown(true);
    }
    private static <T extends Mob> T mob(GameTestHelper test, EntityType<T> type, Vec3 position) {
        var mob = type.create(test.getLevel());
        mob.moveTo(position);
        mob.setNoAi(true); mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.setHealth(200);
        test.getLevel().addFreshEntity(mob);
        return mob;
    }
    @GameTest(template = "empty", batch = "advanced_skills")
    public static void scoopUsesSixMovingTrajectoriesAndSharesHits(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        research(player, "wings_enhancement", "go_blades");
        StyleSystem.select(player, StyleSystem.Style.OFFENSE);
        var origin = test.absolutePos(new BlockPos(3, 35, 3)).getCenter();
        player.moveTo(origin); player.setYRot(0);
        SkillInput.accept(player, true, 1, 0);
        test.assertTrue(WingsScoop.active(player), "Offense combo failed");
        var feathers = test.getLevel().getEntitiesOfClass(org.lyy.lyycore.content.wings.ScoopFeather.class,
                new net.minecraft.world.phys.AABB(origin, origin).inflate(4));
        test.assertTrue(feathers.size() == 6, "Scoop must create exactly six collision entities");
        test.assertFalse(AegisWings.scoop(player), "Scoop recast during its animation");
        var oldPoint = org.lyy.lyycore.content.wings.ScoopPaths.position(3, 20, origin, 0).add(0, -.5, 0);
        var previousLocation = mob(test, EntityType.COW, oldPoint);
        var neutral = mob(test, EntityType.COW, oldPoint.add(3, 0, 0));
        var pet = mob(test, EntityType.WOLF, oldPoint.add(3, 0, 0));
        pet.setOwnerUUID(player.getUUID());
        var immune = mob(test, EntityType.COW, oldPoint.add(3, 0, 0));
        immune.setInvulnerable(true);
        test.assertFalse(AegisWings.attack(player, neutral), "Volley overlapped the scoop animation");
        int started = player.tickCount;
        for (int tick = 1; tick < 40; tick++) {
            player.tickCount = started + tick;
            if (tick == 9) player.moveTo(origin.add(3, 0, 0));
            WingsScoop.tick(new PlayerTickEvent.Pre(player));
            feathers.forEach(org.lyy.lyycore.content.wings.ScoopFeather::tick);
            if (tick == 1) test.assertTrue(neutral.getHealth() == 200, "Area damage occurred before a feather arrived");
            if (tick == 20) {
                var expected = org.lyy.lyycore.content.wings.ScoopPaths.position(3, tick, player.position(), 0);
                test.assertTrue(feathers.stream().anyMatch(feather -> feather.getBoundingBox().getCenter().distanceToSqr(expected) < .0001),
                        "Collision trajectory did not follow the player");
            }
        }
        test.assertTrue(neutral.getHealth() == 160 && pet.getHealth() == 160, "Trajectories missed neutral/owned creatures or dealt duplicate hits");
        test.assertTrue(neutral.getDeltaMovement().y > 0, "Trajectory hit did not launch the target");
        test.assertTrue(previousLocation.getHealth() == 200, "Scoop still attacked its original world position");
        test.assertTrue(immune.getHealth() == 200 && immune.getDeltaMovement().y == 0, "Immune target was damaged or launched");
        player.tickCount = started + 40;
        WingsScoop.tick(new PlayerTickEvent.Pre(player));
        test.assertTrue(feathers.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved), "Scoop entities survived the animation");
        test.assertTrue(AegisWings.scoop(player), "Scoop remained locked after two seconds");
        WingsScoop.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
        test.assertTrue(test.getLevel().getEntitiesOfClass(org.lyy.lyycore.content.wings.ScoopFeather.class,
                new net.minecraft.world.phys.AABB(origin, origin).inflate(30)).isEmpty(), "Logout leaked collision entities");
        for (var entity : new Mob[]{previousLocation, neutral, pet, immune}) entity.discard();
        test.succeed();
    }
    @GameTest(template = "empty", batch = "advanced_skills")
    public static void blinkIncludesPhantomsExcludesNeutralsAndKeepsCooldown(GameTestHelper test) {
        var hover = new ArrayList<SkillNetwork.Hover>();
        var player = ReviewRegressions.player(test.getLevel(), packet -> {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SkillNetwork.Hover state) hover.add(state);
        });
        research(player, "behind_you");
        StyleSystem.select(player, StyleSystem.Style.MOBILITY);
        var origin = test.absolutePos(new BlockPos(3, 50, 3)).getCenter();
        player.moveTo(origin);
        var cow = mob(test, EntityType.COW, origin.add(0, 0, 1));
        var enderman = mob(test, EntityType.ENDERMAN, origin.add(1, 0, 0));
        var target = mob(test, EntityType.PHANTOM, origin.add(0, 0, 4));
        target.setYRot(0);
        SkillInput.accept(player, true, 1, 0);
        test.assertTrue(player.position().distanceToSqr(target.position().add(0, 0, -1)) < .001, "Blink did not choose the phantom behind neutral creatures");
        test.assertTrue(target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 254, "Blink did not apply Slowness 255");
        test.assertTrue(hover.size() == 1, "Hover was not synchronized");
        player.setDeltaMovement(0, -1, 0);
        BasicSkills.tick(new PlayerTickEvent.Pre(player));
        test.assertTrue(player.getDeltaMovement().y == 0, "Hover did not suspend falling");
        var destination = player.position();
        target.moveTo(target.position().add(2, 0, 0));
        test.runAfterDelay(1, () -> {
            player.tickCount++;
            SkillInput.accept(player, false, 1, 0);
            SkillInput.accept(player, true, 1, 0);
            test.assertTrue(player.position().equals(destination), "Blink bypassed its one-second cooldown");
        });
        test.runAfterDelay(21, () -> {
            player.tickCount += 20;
            SkillInput.accept(player, false, 1, 0);
            SkillInput.accept(player, true, 1, 0);
            test.assertTrue(player.position().distanceToSqr(target.position().add(0, 0, -1)) < .001, "Blink did not recover after its cooldown");
            cow.discard(); enderman.discard(); target.discard();
            test.succeed();
        });
    }
    @GameTest(template = "empty", batch = "advanced_skills")
    public static void blockedBlinkDoesNotFallbackToDash(GameTestHelper test) {
        var player = ReviewRegressions.player(test.getLevel());
        research(player, "behind_you", "exploration");
        StyleSystem.select(player, StyleSystem.Style.MOBILITY);
        var origin = test.absolutePos(new BlockPos(3, 65, 3)).getCenter();
        player.moveTo(origin); player.setYRot(0);
        var target = mob(test, EntityType.ZOMBIE, origin.add(0, 0, 4));
        target.setYRot(0);
        var obstruction = BlockPos.containing(target.position().add(0, 0, -1));
        test.getLevel().setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
        SkillInput.accept(player, true, 1, 0);
        test.assertTrue(player.position().equals(origin), "Blocked blink teleported into terrain or fell back to a dash");
        test.assertFalse(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Failed blink still slowed the target");
        test.getLevel().removeBlock(obstruction, false);
        target.discard();
        test.succeed();
    }
}
