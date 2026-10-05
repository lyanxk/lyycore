package org.lyy.lyycore.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lyy.lyycore.content.wings.FeatherAttack;
import org.lyy.lyycore.network.WingsNetwork;

/** Cosmetic side-sweeps and returns; damage remains exclusively server-owned. */
@EventBusSubscriber(modid = "lyycore", value = Dist.CLIENT)
public final class FeatherAttackRenderer {
    private static final List<WingsNetwork.Attack> ATTACKS = new ArrayList<>();
    private static final java.util.Map<WingsNetwork.Attack, FlightPoints> POINTS = new java.util.HashMap<>();
    private static final class FlightPoints {
        final Vec3[] launch = new Vec3[16], direction = new Vec3[16], home = new Vec3[16];
    }
    private static ClientLevel level;
    private static AnimatedMeshModel mesh;
    private static final double RENDER_DISTANCE_SQUARED = 128 * 128;
    private FeatherAttackRenderer() { }
    private static void refreshLevel() {
        if (level != Minecraft.getInstance().level) {
            ATTACKS.clear();
            POINTS.clear(); FourWingAnimation.reset();
            level = Minecraft.getInstance().level;
        }
    }
    public static void add(WingsNetwork.Attack attack) {
        refreshLevel();
        if (level == null) return;
        if (ATTACKS.size() >= 128) POINTS.remove(ATTACKS.removeFirst());
        ATTACKS.add(attack);
        if (attack.featherCount() == 16 && level.getEntity(attack.playerId()) instanceof Player player) preparePoints(attack, player, 0);
    }
    public static float age(Player player, float partial) {
        var attack = current(player);
        return attack == null ? -1 : (player.level().getGameTime() - attack.started() + partial) / 20;
    }
    public static WingsNetwork.Attack current(Player player) {
        for (int i = ATTACKS.size() - 1; i >= 0; i--) {
            var attack = ATTACKS.get(i);
            if (attack.playerId() == player.getId()) return attack;
        }
        return null;
    }
    @SubscribeEvent public static void reload(ModelEvent.BakingCompleted event) { mesh = null; FourWingAnimation.reset(); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        refreshLevel();
        if (level != null) ATTACKS.removeIf(attack -> (level.getGameTime() - attack.started()) / 20F > FeatherAttack.duration(attack.featherCount())
                || !(level.getEntity(attack.playerId()) instanceof Player player) || !player.isAlive());
        POINTS.keySet().retainAll(ATTACKS);
    }
    private static Vec3 local(Vec3 origin, float yaw, double x, double y, double z) {
        return origin.add(new Vec3(x / 16, y / 16, -z / 16).yRot((float) Math.toRadians(-yaw)));
    }
    private static Vec3 curve(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        double s = 1 - t;
        return a.scale(s*s*s).add(b.scale(3*s*s*t)).add(c.scale(3*s*t*t)).add(d.scale(t*t*t));
    }
    private static Vec3 point(WingsNetwork.Attack attack, Player owner, int feather, float time, float partial) {
        int index = feather;
        feather %= 4;
        int sideSign = feather % 2 == 0 ? -1 : 1;
        Vec3 launch = local(attack.origin(), attack.yaw(), sideSign * (feather < 2 ? 28.828427 : 31.828427), feather < 2 ? 30 : 38, 1.171573);
        Vec3 home = local(owner.getPosition(partial), owner.yBodyRot, sideSign * (feather < 2 ? 11.8 : 16.1), feather < 2 ? 29.5 : 33.3, feather < 2 ? 4.76 : 5.2);
        Vec3 launchDirection = null;
        if (attack.featherCount() == 16) {
            var points = POINTS.get(attack);
            launch = points.launch[index]; home = points.home[index]; launchDirection = points.direction[index];
        }
        Vec3 forward = attack.target().subtract(attack.origin()).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < 0.01) forward = Vec3.directionFromRotation(0, attack.yaw());
        Vec3 side = new Vec3(forward.z, 0, -forward.x).scale(sideSign);
        double radius = Math.max(0.6, attack.width() * 0.7);
        Vec3 target = attack.target().add(0, feather < 2 ? -0.1 : 0.3, 0);
        Vec3 entry = target.add(side.scale(radius)), exit = target.subtract(side.scale(radius));
        Vec3 back = target.add(forward.scale(radius * 1.8)).add(side.scale(radius * 1.6));
        // Return follows the player's current position instead of a fixed preview distance.
        if (time < 0.2F) return curve(launch, launch.add(launchDirection == null ? side.scale(.8).add(forward.scale(.5)) : launchDirection.scale(.95)), entry.add(side.scale(radius * 0.5)), entry, time / 0.2);
        if (time < 0.43F) return curve(entry, entry.subtract(side.scale(radius * 0.65)), exit.add(side.scale(radius * 0.65)), exit, (time - 0.2) / 0.23);
        if (time < 0.58F) return curve(exit, exit.subtract(side.scale(radius * 0.5)), back.add(forward.scale(radius)), back, (time - 0.43) / 0.15);
        return curve(back, back.subtract(forward.scale(radius)), home.add(side.scale(1.2)).add(forward.scale(0.8)), home,
                Math.clamp((time - 0.58) / (FeatherAttack.FLIGHT - 0.58), 0, 1));
    }
    private static void preparePoints(WingsNetwork.Attack attack, Player owner, float partial) {
        var points = POINTS.computeIfAbsent(attack, ignored -> new FlightPoints());
        var model = FourWingAnimation.model();
        var animation = FourWingAnimation.get(owner);
        if (owner instanceof net.minecraft.client.player.AbstractClientPlayer client) animation.update(client, partial);
        for (int feather = 0; feather < 16; feather++) {
            if (points.launch[feather] == null) {
                float release = FeatherAttack.release(feather, 16);
                var start = model.attackLocator(feather, "launch", animation.pose(), release - .0001F);
                var direction = model.attackLocator(feather, "direction", animation.pose(), release - .0001F);
                points.launch[feather] = wingPoint(attack.origin(), attack.yaw(), owner, start, partial);
                points.direction[feather] = wingPoint(attack.origin(), attack.yaw(), owner, direction, partial).subtract(points.launch[feather]).normalize();
            }
            var home = model.attackLocator(feather, "home", animation.pose(), FeatherAttack.returnedAt(feather, 16));
            points.home[feather] = wingPoint(owner.getPosition(partial), owner.yBodyRot, owner, home, partial);
        }
    }
    private static Vec3 wingPoint(Vec3 origin, float yaw, Player owner, Vec3 point, float partial) {
        Vec3 local = new Vec3(point.x, point.y - 1.5, -point.z);
        if (owner.isFallFlying()) {
            float amount = Math.min(1, (owner.getFallFlyingTicks() + partial) * (owner.getFallFlyingTicks() + partial) / 100);
            local = local.xRot((float)Math.toRadians(-90 - owner.getXRot()) * amount);
        } else if (owner.isCrouching()) local = local.xRot(.5F);
        return origin.add(local.add(0, owner.isCrouching() ? 1.25 : 1.5, 0).yRot((float)Math.toRadians(-yaw)));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || level == null || ATTACKS.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        for (var attack : ATTACKS) {
            if (!(level.getEntity(attack.playerId()) instanceof Player owner) || owner.isInvisible()) continue;
            // Include the launch, target, current return position and Bezier control-point margins.
            // Culling only the owner would hide feathers flying into the camera from off screen.
            double margin = Math.max(0.6, attack.width() * 0.7) * 4 + 4;
            AABB bounds = new AABB(attack.origin(), attack.target()).minmax(owner.getBoundingBox()).inflate(margin);
            if (bounds.distanceToSqr(camera) > RENDER_DISTANCE_SQUARED || !event.getFrustum().isVisible(bounds)) continue;
            float age = (level.getGameTime() - attack.started() + partial) / 20;
            if (attack.featherCount() == 16) preparePoints(attack, owner, partial);
            for (int feather = 0; feather < attack.featherCount(); feather++) {
                float time = age - FeatherAttack.release(feather, attack.featherCount());
                if (time < 0 || time >= FeatherAttack.FLIGHT) continue;
                Vec3 position = point(attack, owner, feather, time, partial);
                if (position.distanceToSqr(camera) > RENDER_DISTANCE_SQUARED
                        || !event.getFrustum().isVisible(AABB.ofSize(position, 4, 4, 4))) continue;
                Vec3 direction = point(attack, owner, feather, Math.min(time + 0.002F, FeatherAttack.FLIGHT), partial).subtract(position).normalize();
                if (direction.lengthSqr() < 0.01) continue;
                pose.pushPose();
                pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
                pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), direction.toVector3f()));
                if (attack.featherCount() == 16) {
                    FourWingAnimation.model().renderAttackFeather(pose, buffers, LightTexture.FULL_BRIGHT, feather);
                } else {
                    if (mesh == null) mesh = new AnimatedMeshModel("white_feather_projectile");
                    mesh.render(pose, buffers, LightTexture.FULL_BRIGHT, "fly", time);
                }
                pose.popPose();
            }
        }
    }
}
