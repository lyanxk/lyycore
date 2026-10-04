package org.lyy.lyycore.checks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.SonnetEvents;
import org.lyy.lyycore.registry.LyyEffects;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class FreezeRegressions {
    @GameTest(template = "empty")
    public static void playerTeleportUpdatesAuthoritativeAnchor(GameTestHelper test) throws Exception {
        var player = ReviewRegressions.player(test.getLevel());
        player.moveTo(test.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        player.addEffect(new MobEffectInstance(LyyEffects.CRYSTALLIZATION, 60));
        SonnetEvents.playerPre(new PlayerTickEvent.Pre(player));
        var destination = player.position().add(2, 1, 2);
        var server = test.getLevel().getServer();
        server.getCommands().getDispatcher().execute("tp @s " + coordinates(destination),
                server.createCommandSourceStack().withEntity(player).withLevel(test.getLevel()));
        SonnetEvents.playerPost(new PlayerTickEvent.Post(player));
        test.assertTrue(player.position().equals(destination), "Server freeze undid player teleport");
        player.setPos(destination.add(1, -1, 0));
        player.setDeltaMovement(1, -1, 0);
        SonnetEvents.playerPre(new PlayerTickEvent.Pre(player));
        test.assertTrue(player.position().equals(destination) && player.getDeltaMovement().equals(Vec3.ZERO),
                "Player correction failed at new anchor");
        player.removeEffect(LyyEffects.CRYSTALLIZATION);
        SonnetEvents.playerPre(new PlayerTickEvent.Pre(player));
        player.setPos(destination.add(1, 0, 0));
        SonnetEvents.playerPost(new PlayerTickEvent.Post(player));
        test.assertTrue(player.position().equals(destination.add(1, 0, 0)), "Player remained locked after thawing");
        test.succeed();
    }

    private static Zombie frozenZombie(GameTestHelper test, boolean noAi) {
        var zombie = EntityType.ZOMBIE.create(test.getLevel());
        zombie.moveTo(test.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        zombie.setNoAi(noAi);
        test.getLevel().addFreshEntity(zombie);
        zombie.addEffect(new MobEffectInstance(LyyEffects.CRYSTALLIZATION, 60));
        freeze(zombie);
        return zombie;
    }

    private static void freeze(Zombie zombie) { SonnetEvents.entityPre(new EntityTickEvent.Pre(zombie)); }

    private static String coordinates(Vec3 position) {
        // Saved GameTest worlds can be far enough from zero for Double.toString
        // to emit exponent notation, which the command coordinate parser rejects.
        return String.format(java.util.Locale.ROOT, "%.3f %.3f %.3f", position.x, position.y, position.z);
    }

    @GameTest(template = "empty")
    public static void dimensionTransferRebasesFrozenAnchor(GameTestHelper test) {
        for (boolean noAi : new boolean[]{false, true}) {
            var original = frozenZombie(test, noAi);
            Zombie moved = null;
            try {
                var destination = new Vec3(150, 80, 150);
                moved = (Zombie) original.changeDimension(new DimensionTransition(test.getLevel().getServer().getLevel(Level.NETHER),
                        destination, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
                test.assertTrue(moved != null && moved.position().equals(destination), "Dimension transfer failed");
                freeze(moved);
                test.assertTrue(moved.position().equals(destination), "Frozen entity returned to coordinates from the old dimension");
                moved.setPos(destination.add(1, -1, 0));
                moved.setDeltaMovement(1, -1, 0);
                freeze(moved);
                test.assertTrue(moved.position().equals(destination) && moved.getDeltaMovement().equals(Vec3.ZERO),
                        "Frozen movement escaped the new anchor");
                moved.removeEffect(LyyEffects.CRYSTALLIZATION);
                freeze(moved);
                test.assertTrue(moved.isNoAi() == noAi, "Cross-dimension freeze overwrote the original AI state");
                test.assertFalse(moved.getPersistentData().contains("SonnetFrozenState"), "Thawed entity retained its anchor");
            } finally {
                original.discard();
                if (moved != null) moved.discard();
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void teleportCommandMovesFrozenEntity(GameTestHelper test) throws Exception {
        var zombie = frozenZombie(test, false);
        try {
            var destination = zombie.position().add(2, 1, 2);
            var server = test.getLevel().getServer();
            server.getCommands().getDispatcher().execute("tp " + zombie.getUUID() + " " + coordinates(destination),
                    server.createCommandSourceStack());
            test.assertTrue(zombie.position().equals(destination), "Teleport command did not execute");
            freeze(zombie);
            test.assertTrue(zombie.position().equals(destination) && zombie.isNoAi(), "Freeze undid an accepted teleport");
            zombie.setPos(destination.add(1, 0, 0));
            freeze(zombie);
            test.assertTrue(zombie.position().equals(destination), "Walking escaped the anchor after teleport");
        } finally { zombie.discard(); }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void teleportCancellationAndActualLandingAreRespected(GameTestHelper test) {
        var zombie = frozenZombie(test, false);
        try {
            var original = zombie.position();
            var canceled = new EntityTeleportEvent.TeleportCommand(zombie, 100, 70, 100);
            SonnetEvents.teleport(canceled);
            // Another event listener cancels after ours has observed the request.
            canceled.setCanceled(true);
            zombie.setPos(original.add(1, 0, 0));
            freeze(zombie);
            test.assertTrue(zombie.position().equals(original), "Canceled teleport released the frozen anchor");

            var accepted = new EntityTeleportEvent.ChorusFruit(zombie, 100, 70, 100);
            SonnetEvents.teleport(accepted);
            var actual = original.add(2, 2, 2);
            zombie.setPos(actual); // The final landing can differ from the requested position.
            var laterCanceled = new EntityTeleportEvent.TeleportCommand(zombie, 200, 80, 200);
            SonnetEvents.teleport(laterCanceled);
            laterCanceled.setCanceled(true);
            freeze(zombie);
            test.assertTrue(zombie.position().equals(actual), "Ignored actual landing or lost an earlier successful teleport");
            zombie.setPos(actual.add(1, 0, 0));
            freeze(zombie);
            test.assertTrue(zombie.position().equals(actual), "Teleport authorization leaked into later ticks");
        } finally { zombie.discard(); }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyFrozenStateRebasesWithoutLosingAi(GameTestHelper test) {
        var zombie = frozenZombie(test, false);
        try {
            zombie.getPersistentData().getCompound("SonnetFrozenState").remove("Dimension");
            var current = zombie.position().add(3, 0, 0);
            zombie.setPos(current);
            freeze(zombie);
            test.assertTrue(zombie.position().equals(current), "Legacy anchor applied stale coordinates");
            zombie.removeEffect(LyyEffects.CRYSTALLIZATION);
            freeze(zombie);
            test.assertFalse(zombie.isNoAi(), "Legacy upgrade lost the original AI state");
        } finally { zombie.discard(); }
        test.succeed();
    }
}
