package org.lyy.lyycore.client;

import java.util.HashMap;
import java.util.Map;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lyy.lyycore.content.wings.WingsScoop;
import org.lyy.lyycore.network.WingsNetwork;

/** All 48 feathers follow the player's current position and facing throughout the clip. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class WingsScoopRenderer {
    private static final Map<Integer, WingsNetwork.Scoop> ACTIVE = new HashMap<>();
    private static ClientLevel level;
    private WingsScoopRenderer() { }
    private static void refreshLevel() {
        var current = Minecraft.getInstance().level;
        if (level != current) { ACTIVE.clear(); level = current; }
    }
    public static void add(WingsNetwork.Scoop attack) {
        refreshLevel();
        if (level != null) ACTIVE.put(attack.playerId(), attack);
    }
    static boolean active(Player player) {
        refreshLevel();
        var attack = ACTIVE.get(player.getId());
        return attack != null && player.level().getGameTime() - attack.started() < WingsScoop.DURATION;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        refreshLevel();
        if (level != null) ACTIVE.values().removeIf(attack -> level.getGameTime() - attack.started() >= WingsScoop.DURATION
                || !(level.getEntity(attack.playerId()) instanceof Player player) || !player.isAlive());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || level == null || ACTIVE.isEmpty()) return;
        var pose = event.getPoseStack();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (var attack : ACTIVE.values()) {
            if (!(level.getEntity(attack.playerId()) instanceof Player player) || player.isInvisible()) continue;
            Vec3 playerPosition = player.getPosition(partial);
            var bounds = AABB.ofSize(playerPosition, 44, 44, 44);
            if (!event.getFrustum().isVisible(bounds) || bounds.distanceToSqr(camera) > 128 * 128) continue;
            float age = level.getGameTime() - attack.started() + partial;
            if (age < 0 || age >= WingsScoop.DURATION) continue;
            Vec3 origin = playerPosition.subtract(camera);
            pose.pushPose();
            pose.translate(origin.x, origin.y, origin.z);
            pose.mulPose(Axis.YP.rotationDegrees(-player.getViewYRot(partial)));
            pose.scale(1, 1, -1);
            FourWingAnimation.model().render(pose, buffers, LightTexture.FULL_BRIGHT,
                    "scoop_attack", age / WingsScoop.DURATION * WingsScoop.CLIP_SECONDS);
            pose.popPose();
        }
    }
}
