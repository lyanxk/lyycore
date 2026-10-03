package org.lyy.lyycore.gametest;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.LyyCore;
import org.lyy.lyycore.content.SonnetEvents;
import org.lyy.lyycore.content.entity.*;
import org.lyy.lyycore.content.item.SonnetBowItem;
import org.lyy.lyycore.registry.*;

@GameTestHolder(LyyCore.MODID)
@PrefixGameTestTemplate(false)
public final class SonnetGameTests {
    @GameTest(template = "empty", batch = "sonnet_render_cache")
    public static void volleyPathCacheTracksItsInputs(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1, 1, 1)));
        var dome = LyyEntities.SONNET_DOME.get().create(helper.getLevel());
        var volley = LyyEntities.SONNET_VOLLEY.get().create(helper.getLevel());
        dome.prepare(player);
        volley.prepare(dome, player);
        var path = volley.path();
        helper.assertTrue(path.size() == 33 && path == volley.path(), "Repeated frames must reuse the same 32-bounce path");
        for (int i = 1; i < path.size(); i++) {
            var point = path.get(i).subtract(dome.position());
            double boundary = (point.x * point.x + point.z * point.z) / (SonnetDome.RADIUS * SonnetDome.RADIUS)
                    + point.y * point.y / (SonnetDome.HEIGHT * SonnetDome.HEIGHT);
            helper.assertTrue(Math.abs(boundary - 1) < 0.001, "Cached bounce endpoints must stay on the dome boundary");
        }
        dome.setPos(dome.position().add(4, 0, 0));
        volley.prepare(dome, player);
        var moved = volley.path();
        helper.assertTrue(moved != path && moved.get(1).distanceTo(path.get(1).add(4, 0, 0)) < 0.001,
                "New synchronized dome coordinates must invalidate the cached trajectory");
        volley.setUUID(java.util.UUID.randomUUID());
        helper.assertTrue(!volley.path().equals(moved), "A new entity seed must rebuild the path");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "sonnet_recall")
    public static void ultimateKeyRecallsOwnedDomeAndCancelsPendingLaunch(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new Vec3(1, 1, 1)));
        var stack = new ItemStack(LyyItems.WHISPER_OF_THE_PAST.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        org.lyy.lyycore.network.SonnetNetwork.activate(player);
        var dome = SonnetBowItem.resolveDome(stack, helper.getLevel());
        helper.assertTrue(dome != null && dome.getBoundingBox().getXsize() == 48,
                "A 24-block radius must have a 48-block entity bound");
        helper.assertTrue(dome.contains(dome.position().add(23.9, 0, 0)) && !dome.contains(dome.position().add(24.1, 0, 0)),
                "The actual area test must include the expanded radius and stop at 24 blocks");
        var stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!dome.recall(stranger) && dome.isValid(), "Another player must not recall the owner's dome");
        player.getInventory().setItem(1, stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.runAfterDelay(11, () -> {
            helper.assertTrue(dome.isActive() && SonnetBowItem.isCrystal(stack), "The bound bow must transform even after changing slots");
            helper.assertTrue(dome.fire(player), "Start a volley before recalling the dome");
            org.lyy.lyycore.network.SonnetNetwork.activate(player);
            helper.assertTrue(dome.isRemoved() && !SonnetBowItem.isCrystal(stack),
                    "Pressing the ultimate key with an empty hand must recall the dome and revert the bow immediately");
            helper.assertTrue(!dome.fire(player), "A recalled dome must no longer accept shots");
        });
        helper.runAfterDelay(13, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(SonnetVolley.class, dome.getBoundingBox()).isEmpty(),
                    "Recalling must stop the outstanding volley");
            player.getInventory().setItem(1, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            org.lyy.lyycore.network.SonnetNetwork.activate(player);
            var pending = SonnetBowItem.resolveDome(stack, helper.getLevel());
            helper.assertTrue(pending != null && !pending.isOpen(), "A fresh ultimate can start after manual recall");
            org.lyy.lyycore.network.SonnetNetwork.activate(player);
            helper.assertTrue(pending.isRemoved() && SonnetBowItem.resolveDome(stack, helper.getLevel()) == null,
                    "Pressing again during the launch must cancel the pending dome too");
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!SonnetBowItem.isCrystal(stack), "A cancelled launch must not reopen the dome later");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(SonnetArrow.class, dome.getBoundingBox().inflate(40)).stream()
                    .noneMatch(arrow -> arrow.getOwner() == player), "Cancelled ultimate arrows must disappear");
            player.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 25, batch = "sonnet_animation")
    public static void crystalShotAutomaticallyRedrawsWithoutHoldingUse(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new Vec3(1, 1, 1)));
        var bow = LyyItems.WHISPER_OF_THE_PAST.get();
        var stack = new ItemStack(bow);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var dome = helper.spawn(LyyEntities.SONNET_DOME.get(), new Vec3(1, 1, 1));
        dome.prepare(player); SonnetBowItem.bind(stack, dome); dome.open();
        helper.assertTrue(SonnetBowItem.crystalDrawStage(stack, helper.getLevel()) == 3,
                "An unused crystal bow starts ready at full draw");
        bow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SonnetBowItem.crystalDrawStage(stack, helper.getLevel()) == 0 && !player.isUsingItem(),
                "Shooting releases the string without requiring a held use action");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(SonnetBowItem.crystalDrawStage(stack, helper.getLevel()) == 1,
                    "The bow must automatically nock and start drawing after release");
            bow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(SonnetBowItem.crystalDrawStage(stack, helper.getLevel()) == 1,
                    "A rejected shot during cooldown must not reset the animation");
        });
        helper.runAfterDelay(5, () -> helper.assertTrue(SonnetBowItem.crystalDrawStage(stack, helper.getLevel()) == 2,
                "Automatic redraw must pass through the intermediate pose"));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(SonnetBowItem.crystalDraw(stack, helper.getLevel(), 0) == 1 && !player.isUsingItem(),
                    "The bow must be fully drawn by the next allowed shot without holding right-click");
            dome.discard(); SonnetBowItem.refreshBinding(stack, helper.getLevel());
            helper.assertTrue(!SonnetBowItem.isCrystal(stack), "Dome expiry must also clear the animation state");
            player.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "sonnet")
    public static void freeShotsPreventFallingWithoutDisablingGravity(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1, 2, 1)));
        var bow = LyyItems.WHISPER_OF_THE_PAST.get();
        var stack = new ItemStack(bow);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(bow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "An empty quiver must allow drawing");
        double normalGravity = player.getAttributeValue(Attributes.GRAVITY);
        player.setDeltaMovement(0, -1, 0);
        SonnetEvents.playerPre(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        helper.assertTrue(player.getAttributeValue(Attributes.GRAVITY) == normalGravity && player.getDeltaMovement().y == 0,
                "Drawing must stop descending without changing gravity");
        player.setOnGround(false);
        player.setDeltaMovement(0, 0.42, 0);
        double previousY = player.getY();
        for (int tick = 0; tick < 24; tick++) {
            SonnetEvents.playerPre(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            player.travel(Vec3.ZERO);
            SonnetEvents.playerPost(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(player.getY() >= previousY - 0.000001, "Drawing must never lose altitude");
            if (tick == 0) helper.assertTrue(player.getDeltaMovement().y > 0 && player.getDeltaMovement().y < 0.42,
                    "Gravity must still decelerate upward motion while drawing");
            previousY = player.getY();
        }
        helper.assertTrue(player.getDeltaMovement().y == 0, "The player must settle at the jump apex instead of floating upward forever");
        player.setPos(helper.absoluteVec(new Vec3(1, 2, 1)));
        player.stopUsingItem();
        SonnetEvents.playerPost(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        helper.assertTrue(player.getAttributeValue(Attributes.GRAVITY) == normalGravity, "Normal gravity must remain unchanged after release");
        player.setDeltaMovement(0, -0.2, 0);
        SonnetEvents.playerPre(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        helper.assertTrue(player.getDeltaMovement().y == -0.2, "Releasing the bow must allow falling again");
        bow.releaseUsing(stack, helper.getLevel(), player, 71990);
        var ammo = new ItemStack(Items.ARROW, 7);
        player.getInventory().setItem(1, ammo);
        bow.releaseUsing(stack, helper.getLevel(), player, 71980);
        helper.assertTrue(ammo.getCount() == 7 && stack.getDamageValue() == 0, "Shooting must consume neither arrows nor durability");
        var arrows = helper.getLevel().getEntitiesOfClass(SonnetArrow.class, helper.getBounds());
        helper.assertTrue(arrows.size() == 2, "Both empty-quiver and charged shots must spawn");
        helper.assertTrue(arrows.stream().anyMatch(a -> !a.isCharged() && !a.isNoGravity()), "Partial shot must retain gravity");
        helper.assertTrue(arrows.stream().anyMatch(a -> a.isCharged() && a.isNoGravity()), "Full shot must disable gravity");
        arrows.forEach(Entity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "sonnet")
    public static void chargedArrowPiercesAndCrystallizesTwoTargets(GameTestHelper helper) {
        var first = helper.spawn(EntityType.COW, new Vec3(1, 2, 1));
        var second = helper.spawn(EntityType.COW, new Vec3(1, 2, 2.2));
        for (var cow : java.util.List.of(first, second)) {
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); cow.setHealth(200);
            cow.setNoAi(true); cow.setNoGravity(true);
        }
        var arrow = helper.spawn(LyyEntities.CRYSTAL_ARROW.get(), new Vec3(1, 2.5, 0));
        arrow.setCharged(true); arrow.setDeltaMovement(0, 0, 3);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(first.getHealth() == 180 && second.getHealth() == 180, "One charged arrow must deal exactly 20 to each pierced target");
            helper.assertTrue(first.hasEffect(LyyEffects.CRYSTALLIZATION) && second.hasEffect(LyyEffects.CRYSTALLIZATION), "Both targets must crystallize");
            helper.assertTrue(arrow.isAlive() && Math.abs(arrow.getDeltaMovement().z - 3) < 0.001, "Piercing must preserve forward velocity");
            arrow.discard(); first.discard(); second.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 170, batch = "sonnet")
    public static void allArrowsExpireAfterEightSecondsEvenAfterReload(GameTestHelper helper) {
        var partial = helper.spawn(LyyEntities.CRYSTAL_ARROW.get(), new Vec3(1, 2, 1));
        var charged = helper.spawn(LyyEntities.CRYSTAL_ARROW.get(), new Vec3(1, 2, 2)); charged.setCharged(true);
        var spectral = helper.spawn(LyyEntities.CRYSTAL_SPECTRAL_ARROW.get(), new Vec3(2, 2, 1));
        var arrows = java.util.List.<AbstractArrow>of(partial, charged, spectral);
        helper.runAfterDelay(80, () -> {
            for (var arrow : arrows) arrow.load(arrow.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        });
        helper.runAfterDelay(159, () -> {
            for (var arrow : arrows) helper.assertTrue(arrow.isAlive() && arrow.pickup == AbstractArrow.Pickup.DISALLOWED, "Arrows must exist until eight seconds and stay uncollectible");
        });
        helper.runAfterDelay(161, () -> {
            for (var arrow : arrows) helper.assertTrue(arrow.isRemoved(), "Reloading must not reset the eight-second lifespan");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80, batch = "sonnet")
    public static void crystallizationStopsMovementThenRestoresAi(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new Vec3(1, 2, 1));
        cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(LyyEffects.CRYSTALLIZATION, 20));
        var anchor = cow.position();
        cow.setDeltaMovement(0.5, 0.6, 0.5);
        helper.runAfterDelay(5, () -> helper.assertTrue(cow.distanceToSqr(anchor) < 0.001 && cow.isNoAi(), "Crystallized mobs must not move or act"));
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(!cow.hasEffect(LyyEffects.CRYSTALLIZATION) && !cow.isNoAi(), "Effect expiry must restore the original AI state");
            cow.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 325, batch = "sonnet")
    public static void ultimateOpensAfterHalfSecondAndExpiresAfterFifteen(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new Vec3(1, 2, 1)));
        var stack = new ItemStack(LyyItems.WHISPER_OF_THE_PAST.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        org.lyy.lyycore.network.SonnetNetwork.activate(player);
        var dome = SonnetBowItem.resolveDome(stack, helper.getLevel());
        helper.assertTrue(dome != null && !dome.isOpen() && !SonnetBowItem.isCrystal(stack), "Ultimate starts with a pending bound dome");
        // Switching slots during the launch must not transform an unrelated bow.
        player.getInventory().setItem(1, stack); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.runAfterDelay(11, () -> {
            SonnetBowItem.refreshBinding(stack, helper.getLevel());
            helper.assertTrue(dome.isActive() && SonnetBowItem.isCrystal(stack), "Dome must open and transform the bound bow after ten ticks");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(SonnetArrow.class, dome.getBoundingBox().inflate(40)).stream().noneMatch(a -> a.getOwner() == player), "Launch arrow disappears after half a second");
        });
        helper.runAfterDelay(309, () -> helper.assertTrue(dome.isActive(), "Dome should last the full fifteen seconds after opening"));
        helper.runAfterDelay(311, () -> {
            SonnetBowItem.refreshBinding(stack, helper.getLevel());
            helper.assertTrue(dome.isRemoved() && !SonnetBowItem.isCrystal(stack), "Expiry must revert the bow");
            player.discard(); helper.succeed();
        });
    }

}
