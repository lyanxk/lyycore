package org.lyy.lyycore.client;

import java.util.HashMap;
import java.util.Map;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lyy.lyycore.content.AegisWings;
import org.lyy.lyycore.content.wings.WingsAttack;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.registry.LyyEffects;

/** The authored 48-feather slash follows its target, then recalls to the moving player. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class WingsPursuitRenderer {
    private static final Map<Integer, Attack> ACTIVE = new HashMap<>();
    private static ClientLevel level;
    private static AnimatedMeshModel model;
    private static float[] sampledPose;
    private static int root;

    private static final class Attack {
        final WingsNetwork.Pursuit packet;
        Vec3 target;
        Attack(WingsNetwork.Pursuit packet) { this.packet = packet; target = packet.target(); }
    }

    private WingsPursuitRenderer() { }

    private static void refreshLevel() {
        var current = Minecraft.getInstance().level;
        if (level != current) { ACTIVE.clear(); level = current; }
    }

    public static void add(WingsNetwork.Pursuit packet) {
        refreshLevel();
        if (level != null) ACTIVE.put(packet.playerId(), new Attack(packet));
    }

    static boolean active(Player player) {
        refreshLevel();
        var attack = ACTIVE.get(player.getId());
        return attack != null && valid(attack);
    }

    private static boolean valid(Attack attack) {
        return level.getGameTime() - attack.packet.started() < WingsAttack.PURSUIT_DURATION_TICKS
                && level.getEntity(attack.packet.playerId()) instanceof Player player
                && player.isAlive() && !player.isSpectator() && AegisWings.unlocked(player)
                && !player.hasEffect(LyyEffects.CRYSTALLIZATION);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        refreshLevel();
        if (level == null) return;
        ACTIVE.values().removeIf(attack -> !valid(attack));
        for (var attack : ACTIVE.values()) {
            // Keep the last impact location after the hit, even if it kills/removes the target.
            if (level.getGameTime() - attack.packet.started() <= WingsAttack.PURSUIT_HIT_TICKS
                    && level.getEntity(attack.packet.targetId()) instanceof net.minecraft.world.entity.LivingEntity target)
                attack.target = target.getBoundingBox().getCenter();
        }
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t*t*(3-2*t);
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || level == null || ACTIVE.isEmpty()) return;
        var currentModel = FourWingAnimation.model();
        if (model != currentModel) {
            model = currentModel;
            sampledPose = model.newPose();
            root = model.boneIndex("root") * 9;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var pose = event.getPoseStack();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        for (var attack : ACTIVE.values()) {
            if (!valid(attack) || !(level.getEntity(attack.packet.playerId()) instanceof AbstractClientPlayer player)
                    || player.isInvisible()) continue;
            float ticks = level.getGameTime() - attack.packet.started() + partial;
            if (ticks < 0 || ticks >= WingsAttack.PURSUIT_DURATION_TICKS) continue;
            float seconds = ticks / 20;
            Vec3 origin = player.getPosition(partial);
            Vec3 target = attack.target;
            if (ticks < WingsAttack.PURSUIT_HIT_TICKS
                    && level.getEntity(attack.packet.targetId()) instanceof net.minecraft.world.entity.LivingEntity victim)
                target = victim.getPosition(partial).add(0, victim.getBbHeight()/2, 0);
            var bounds = new AABB(origin, target).inflate(8);
            if (bounds.distanceToSqr(camera) > 128*128 || !event.getFrustum().isVisible(bounds)) continue;

            Vec3 delta = target.subtract(origin);
            float aimYaw = delta.horizontalDistanceSqr() < .001 ? player.yBodyRot
                    : (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
            float reach = smooth(seconds/.42F) * (1-smooth((seconds-.94F)/.46F));
            float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            float yaw = Mth.rotLerp(reach, bodyYaw, aimYaw);
            model.sample("pursuit_v3", seconds, sampledPose);

            // The source hits (0, 1, -4). Translate the formation instead of stretching
            // feathers; remove the offset during recall so they return to the current back.
            Vec3 localTarget = delta.yRot((float)Math.toRadians(yaw));
            sampledPose[root] += (float)localTarget.x * 16 * reach;
            sampledPose[root+1] += (float)(localTarget.y-1) * 16 * reach;
            sampledPose[root+2] += (float)(4-localTarget.z) * 16 * reach;

            var idle = FourWingAnimation.get(player);
            idle.update(player, partial);
            float blend = smooth(seconds/.12F) * (1-smooth((seconds-1.28F)/.12F));
            for (int i = 0; i < sampledPose.length; i++) sampledPose[i] = Mth.lerp(blend, idle.pose()[i], sampledPose[i]);
            pose.pushPose();
            pose.translate(origin.x-camera.x, origin.y-camera.y, origin.z-camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            pose.scale(1, 1, -1);
            model.renderPose(pose, buffers, LightTexture.FULL_BRIGHT, sampledPose);
            pose.popPose();
        }
    }
}
