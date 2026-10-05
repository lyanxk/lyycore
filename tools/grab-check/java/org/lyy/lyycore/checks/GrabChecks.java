package org.lyy.lyycore.checks;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.entity.guiding.*;
import org.lyy.lyycore.registry.LyyEntities;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class GrabChecks {
    private static void call(GuidingBoss boss, String name, Class<?>[] types, Object... args) throws Exception {
        var method = GuidingBoss.class.getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(boss, args);
    }
    private static void step(GuidingBoss boss) throws Exception { call(boss, "customServerAiStep", new Class<?>[0]); }
    private static GuidingGrab begin(GuidingBoss boss, ServerPlayer player) throws Exception {
        call(boss, "beginGrab", new Class<?>[]{Player.class, boolean.class}, player, true);
        var field = GuidingBoss.class.getDeclaredField("activeGrab"); field.setAccessible(true);
        var id = (UUID)field.get(boss);
        return id == null ? null : (GuidingGrab)((net.minecraft.server.level.ServerLevel)boss.level()).getEntity(id);
    }
    private record Fixture(GuidingBoss boss, ServerPlayer player) {
        void close() { boss.discard(); player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
    }
    private static Fixture fixture(GameTestHelper test) throws Exception {
        var level = test.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "rope-check"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override public void setListenerForServerboundHandshake(PacketListener listener) { }
        };
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { }
        };
        var origin = test.absolutePos(new BlockPos(3, 20, 3)).getCenter();
        player.moveTo(origin.add(0, 0, 8)); level.addNewPlayer(player);
        var boss = LyyEntities.ENDLESS_DEMAND.get().create(level);
        boss.moveTo(origin); boss.setNoAi(true); boss.beginSummoning(player, BlockPos.ZERO); level.addFreshEntity(boss);
        step(boss);
        return new Fixture(boss, player);
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void captureWrapPullBoundAndRelease(GameTestHelper test) throws Exception {
        var fixture = fixture(test); var boss = fixture.boss; var player = fixture.player;
        try {
            var grab = begin(boss, player);
            grab.setPos(player.getBoundingBox().getCenter()); grab.tick();
            test.assertTrue(!grab.isRemoved() && grab.phase() == GuidingGrab.Phase.WRAP, "Contact must retain the rope and start wrapping");
            test.assertTrue(grab.owner() == boss && grab.target() == player, "Missing network owner/target ids");
            for (int i = 0; i < GuidingBoss.GRAB_WRAP_TICKS - 1; i++) {
                step(boss);
                test.assertTrue(boss.pullVelocity(player).lengthSqr() == 0, "Target moved before the wrap completed");
            }
            step(boss); grab.tick();
            test.assertTrue(grab.phase() == GuidingGrab.Phase.PULL && boss.pullVelocity(player).lengthSqr() > 0, "Completed wrap did not start pulling");
            player.moveTo(boss.position().add(0, .5, 2)); step(boss); grab.tick();
            test.assertTrue(grab.phase() == GuidingGrab.Phase.BOUND, "Arriving target did not enter bound idle");
            call(boss, "finishAction", new Class<?>[0]); grab.tick();
            test.assertTrue(grab.phase() == GuidingGrab.Phase.RELEASE && !boss.pulling(player), "Release kept capture controls active");
            test.runAfterDelay(5, () -> {
                try { test.assertTrue(grab.isRemoved(), "Release did not retire after four ticks"); test.succeed(); }
                finally { fixture.close(); }
            });
        } catch (Throwable failure) { fixture.close(); throw failure; }
    }
    @GameTest(template = "empty")
    public static void interruptedCapturesAndReloadsRecover(GameTestHelper test) throws Exception {
        var fixture = fixture(test); var boss = fixture.boss; var player = fixture.player;
        try {
            Consumer<EntityJoinLevelEvent> reject = event -> { if (event.getEntity() instanceof GuidingGrab) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(reject);
            try { test.assertTrue(begin(boss, player) == null && !boss.grabbing(), "Rejected launch stuck in grab"); }
            finally { NeoForge.EVENT_BUS.unregister(reject); }
            var first = begin(boss, player); first.discard();
            test.assertFalse(boss.grabbing(), "Destroyed projectile left capture active");
            var current = begin(boss, player); boss.grabMissed(first.getUUID());
            test.assertTrue(boss.ownsGrab(current.getUUID()), "Stale release canceled a later cast");
            current.setPos(player.getBoundingBox().getCenter()); current.tick();
            current.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            for (int i = 0; i < 20; i++) step(boss);
            test.assertFalse(boss.pulling(player), "Unloaded rope left player captured");
            var last = begin(boss, player); last.setPos(player.getBoundingBox().getCenter()); last.tick();
            var saved = new CompoundTag(); boss.addAdditionalSaveData(saved); boss.readAdditionalSaveData(saved);
            test.assertFalse(boss.pulling(player), "Reload resumed a transient capture");
            last.tick(); test.assertTrue(last.phase() == GuidingGrab.Phase.RELEASE, "Obsolete rope did not release");
            test.succeed();
        } finally { fixture.close(); }
    }
}
